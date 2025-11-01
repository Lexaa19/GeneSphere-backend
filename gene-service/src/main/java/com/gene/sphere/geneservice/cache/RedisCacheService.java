package com.gene.sphere.geneservice.cache;

import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.CacheService;
import com.gene.sphere.geneservice.service.GeneService;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *  Redis cache service with distributed locking for gene data.
 *
 * <p>This service provides efficient caching of {@link GeneRecord} objects using Redis
 * with stampede prevention through Redisson distributed locks. Key features:
 * <ul>
 *   <li><strong>Cache-Aside Pattern:</strong> Cache hits return directly from Redis,
 *       misses fetch from database and populate cache</li>
 *   <li><strong>Stampede Prevention:</strong> Distributed locks ensure only one thread/instance
 *       queries database for the same gene, even under high concurrency</li>
 *   <li><strong>Graceful Degradation:</strong> Falls back to database if cache or locks fail</li>
 *   <li><strong>Production-Safe Operations:</strong> Uses SCAN instead of KEYS,
 *       batched deletions, configurable TTLs</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This service is thread-safe, relying on Spring's
 * thread-safe {@link RedisTemplate} and Redisson's distributed locks.
 *
 * <p><strong>Architecture:</strong>
 * <pre>
 * Request → Check Redis → HIT: Return immediately
 *                      → MISS: Acquire Lock → Fetch from DB → Cache → Return
 *                              ↓ Lock Failed
 *                              Wait → Retry Cache → Return
 * </pre>
 *
 * @author Gene Service Team
 * @version 2.0
 * @since 1.0
 */
@Service
public class RedisCacheService implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

    /**
     * Redis template for performing cache operations.
     * Thread-safe and handles connection pooling automatically.
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Gene service for database fallback when cache misses occur.
     */
    private final GeneService geneService;

    /**
     * Redisson client for distributed locking to prevent cache stampede.
     */
    private final RedissonClient redissonClient;

    /**
     * Configuration for cache behavior including TTL and key prefixes.
     */
    private final CacheConfig redisCacheConfig;

    /**
     * Micrometer registry for tracking cache metrics and performance.
     */
    @Autowired
    private MeterRegistry meterRegistry;

    // ==================== CONFIGURATION PROPERTIES ====================

    /**
     * Time-to-live for cached entries (default: 5 days).
     */
    @Value("${cache.redis.ttl:5d}")
    private Duration cacheTtl;

    /**
     * Maximum time to wait when trying to acquire a lock (default: 3 seconds).
     */
    @Value("${cache.redis.lock-wait-time:3s}")
    private Duration lockWaitTime;

    /**
     * Maximum time to hold a lock before auto-release (default: 10 seconds).
     * Prevents deadlocks if a thread crashes while holding the lock.
     */
    @Value("${cache.redis.lock-lease-time:10s}")
    private Duration lockLeaseTime;


    /**
     * Constructs a new RedisCacheService with the specified dependencies.
     *
     * <p>The {@code redisCacheConfig} parameter is optional (can be null).
     * If not provided, a default configuration will be created with:
     * <ul>
     *   <li>TTL: 5 days</li>
     *   <li>Key prefix: "gene:"</li>
     *   <li>Fallback enabled: true</li>
     * </ul>
     *
     * @param redisTemplate    the Redis template for cache operations, must not be null
     * @param geneService      the gene service for database fallback, must not be null
     * @param redissonClient   the Redisson client for distributed locking, must not be null
     * @param redisCacheConfig optional cache configuration, can be null for defaults
     */
    public RedisCacheService(
            RedisTemplate<String, Object> redisTemplate,
            GeneService geneService,
            RedissonClient redissonClient,
            @Autowired(required = false) CacheConfig redisCacheConfig) {
        this.redisTemplate = redisTemplate;
        this.geneService = geneService;
        this.redissonClient = redissonClient;
        this.redisCacheConfig = redisCacheConfig != null ? redisCacheConfig :
                new CacheConfig(Duration.ofDays(5), "gene:", true);

        logger.info("RedisCacheService initialized with TTL: {}, Lock wait: {}, Lock lease: {}",
                cacheTtl, lockWaitTime, lockLeaseTime);
    }

    // ==================== PUBLIC API (CacheService Interface) ====================

    /**
     * Retrieves a gene by its name with distributed lock-based stampede prevention.
     *
     * <p>This method implements a cache-aside pattern with distributed locking:
     * <ol>
     *   <li>Validates the input gene name</li>
     *   <li>Checks Redis cache for existing entry (fast path)</li>
     *   <li>If cache hit: returns cached {@link GeneRecord}</li>
     *   <li>If cache miss: uses distributed lock to prevent stampede
     *       <ul>
     *         <li>Lock acquired: fetch from database, cache result, return</li>
     *         <li>Lock contention: wait briefly, retry cache (another thread likely populated it)</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p><strong>Cache Key Format:</strong> {@code gene:GENENAME} (uppercase)
     * <p><strong>TTL:</strong> Configurable (default: 5 days)
     * <p><strong>Stampede Prevention:</strong> Only one thread/instance queries DB per gene
     *
     * @param name the gene name to search for (case-insensitive), must not be null or blank
     * @return an {@link Optional} containing the {@link GeneRecord} if found,
     *         or {@link Optional#empty()} if not found or invalid input
     */
    @Override
    public Optional<GeneRecord> getGeneByName(String name) {
        if (name == null || name.isBlank()) {
            meterRegistry.counter("cache.requests.invalid").increment();
            return Optional.empty();
        }

        String normalizedName = name.toUpperCase();
        String cacheKey = buildCacheKey(normalizedName);

        // Fast path: Check cache first
        Optional<GeneRecord> cached = getFromCache(cacheKey);
        if (cached.isPresent()) {
            meterRegistry.counter("cache.hits").increment();
            logger.debug("Cache hit for gene: {}", normalizedName);
            return cached;
        }

        // Slow path: Cache miss - use distributed lock to prevent stampede
        meterRegistry.counter("cache.misses").increment();
        logger.debug("Cache miss for gene: {}, acquiring lock", normalizedName);
        return fetchWithLock(normalizedName, cacheKey);
    }

    /**
     * Removes a specific gene from the Redis cache.
     *
     * <p>This operation is useful when:
     * <ul>
     *   <li>Gene data has been updated in the database</li>
     *   <li>You want to force a fresh fetch on the next request</li>
     *   <li>Implementing cache invalidation strategies</li>
     * </ul>
     *
     * <p>If the gene is not in cache, this operation is a no-op (no error thrown).
     *
     * @param geneName the name of the gene to evict from cache, must not be null or empty
     * @example
     * <pre>
     * // After updating TP53 in database
     * cacheService.evictGene("TP53");
     * // Next getGeneByName("TP53") will fetch fresh data
     * </pre>
     */
    @Override
    public void evictGene(String geneName) {
        if (geneName == null || geneName.isBlank()) {
            logger.warn("Attempted to evict gene with null/blank name");
            return;
        }

        String cacheKey = buildCacheKey(geneName);
        redisTemplate.delete(cacheKey);
        meterRegistry.counter("cache.evictions").increment();
        logger.info("Evicted gene from cache: {}", geneName);
    }

    /**
     * Clears all gene entries from the Redis cache using production-safe SCAN.
     *
     * <p>This operation removes all cached genes (keys matching pattern {@code gene:*})
     * but leaves other cache entries untouched. Uses SCAN instead of KEYS for
     * production safety. Useful for:
     * <ul>
     *   <li>Bulk cache invalidation after database updates</li>
     *   <li>Cache maintenance operations</li>
     *   <li>Testing scenarios</li>
     * </ul>
     *
     * @throws RuntimeException if Redis connection fails
     */
    @Override
    public void clearCache() {
        logger.info("Clearing all gene cache entries");
        meterRegistry.counter("cache.clears.all").increment();

        try {
            Set<String> keysToDelete = getKeysByPattern("gene:*", 50_000);

            if (keysToDelete.isEmpty()) {
                logger.info("No gene cache entries to clear");
                return;
            }

            long deleted = deleteKeysInBatches(keysToDelete);
            logger.info("Cleared {} gene cache entries", deleted);
            meterRegistry.counter("cache.clears.keys", "count", String.valueOf(deleted)).increment();

        } catch (Exception e) {
            logger.error("Failed to clear cache", e);
            meterRegistry.counter("cache.errors", "operation", "clear").increment();
            throw new RuntimeException("Cache clearing failed", e);
        }
    }

    // ==================== CACHE OPERATIONS (Private) ====================

    /**
     * Retrieves a gene from Redis cache without database fallback.
     *
     * <p>This is a pure cache read operation used by other methods.
     * Returns empty if key doesn't exist or value is not a GeneRecord.
     *
     * @param cacheKey the Redis key to fetch
     * @return {@link Optional} containing the gene if found and valid, empty otherwise
     */
    private Optional<GeneRecord> getFromCache(String cacheKey) {
        try {
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value instanceof GeneRecord gene) {
                return Optional.of(gene);
            }
        } catch (Exception e) {
            logger.warn("Error reading from cache key: {}", cacheKey, e);
            meterRegistry.counter("cache.errors", "operation", "read").increment();
        }
        return Optional.empty();
    }

    /**
     * Fetches gene from database using distributed lock to prevent cache stampede.
     *
     * <p>Lock acquisition flow:
     * <ol>
     *   <li>Try to acquire lock with configured timeout</li>
     *   <li>If acquired: fetch from DB, cache result, release lock</li>
     *   <li>If contention: wait briefly, retry cache check (another thread populating)</li>
     *   <li>If interrupted/error: fallback to direct DB query (graceful degradation)</li>
     * </ol>
     *
     * @param geneName the normalized gene name (uppercase)
     * @param cacheKey the Redis cache key
     * @return {@link Optional} containing the gene record
     */
    private Optional<GeneRecord> fetchWithLock(String geneName, String cacheKey) {
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire lock with timeout
            boolean acquired = lock.tryLock(
                    lockWaitTime.toMillis(),
                    lockLeaseTime.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (acquired) {
                try {
                    logger.debug("Lock acquired for gene: {}", geneName);
                    meterRegistry.counter("cache.locks.acquired").increment();
                    return fetchAndCache(geneName, cacheKey);
                } finally {
                    // Always unlock, even if exception occurs
                    lock.unlock();
                    logger.debug("Lock released for gene: {}", geneName);
                }
            } else {
                // Lock contention - another thread is fetching
                logger.debug("Lock contention for gene: {}, waiting for other thread", geneName);
                meterRegistry.counter("cache.locks.contention").increment();
                return retryCache(cacheKey, geneName);
            }

        } catch (InterruptedException e) {
            // Thread was interrupted (user cancelled, timeout, or server shutdown)
            // Restore interrupted status so caller can detect cancellation
            // Fallback to direct DB query - slower but user still gets data
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for lock: {}", geneName, e);
            meterRegistry.counter("cache.locks.interrupted").increment();
            return geneService.getGeneByName(geneName);

        } catch (Exception e) {
            // Unexpected error in lock acquisition (Redis down, network issue, etc.)
            // Log error and increment metric for monitoring/alerting
            // Graceful degradation: bypass cache and query DB directly
            logger.error("Error in fetchWithLock for gene: {}", geneName, e);
            meterRegistry.counter("cache.errors", "operation", "lock").increment();
            return geneService.getGeneByName(geneName);
        }
    }

    /**
     * Fetches gene from database and caches the result.
     *
     * <p>Double-check pattern implementation:
     * <ol>
     *   <li>Double-check cache (race condition: another thread may have populated while waiting)</li>
     *   <li>If still empty: query database</li>
     *   <li>If found: save to Redis with TTL</li>
     *   <li>Return result</li>
     * </ol>
     *
     * <p>This method is only called by the thread that successfully acquired the lock.
     *
     * @param geneName the normalized gene name
     * @param cacheKey the Redis cache key
     * @return {@link Optional} containing the gene record
     */
    private Optional<GeneRecord> fetchAndCache(String geneName, String cacheKey) {
        // Double-check cache (race condition protection)
        // Another thread might have filled it while we waited for lock
        Optional<GeneRecord> cached = getFromCache(cacheKey);
        if (cached.isPresent()) {
            logger.debug("Gene found in cache during double-check: {}", geneName);
            meterRegistry.counter("cache.double_check.hits").increment();
            return cached;
        }

        // Cache still empty - fetch from database
        logger.debug("Fetching gene from database: {}", geneName);
        meterRegistry.counter("cache.database.queries").increment();

        Optional<GeneRecord> result = geneService.getGeneByName(geneName);

        // Cache the result if found
        result.ifPresentOrElse(
                gene -> {
                    redisTemplate.opsForValue().set(cacheKey, gene, cacheTtl);
                    meterRegistry.counter("cache.writes").increment();
                    logger.debug("Cached gene: {} with TTL: {}", geneName, cacheTtl);
                },
                () -> {
                    logger.debug("Gene not found in database: {}", geneName);
                    meterRegistry.counter("cache.database.not_found").increment();
                }
        );

        return result;
    }

    /**
     * Retries cache check after waiting for another thread to populate it.
     *
     * <p>Strategy when lock contention occurs:
     * <ol>
     *   <li>Wait briefly (100ms) for the lock-holding thread to complete DB query and cache</li>
     *   <li>Check cache again (high probability it's now populated)</li>
     *   <li>If still empty: fallback to direct DB query (graceful degradation)</li>
     * </ol>
     *
     * <p>This prevents multiple database queries while providing eventual consistency.
     *
     * @param cacheKey the Redis cache key
     * @param geneName the gene name (for logging)
     * @return {@link Optional} containing the gene record
     */
    private Optional<GeneRecord> retryCache(String cacheKey, String geneName) {
        // Wait briefly for the other thread to populate cache
        try {
            Thread.sleep(100); // 100ms should be enough for most DB queries
        } catch (InterruptedException e) {
            // Restore interrupted status and continue
            Thread.currentThread().interrupt();
            logger.debug("Interrupted during retry wait for gene: {}", geneName);
        }

        // Check cache again - likely populated by the lock-holding thread
        Optional<GeneRecord> cached = getFromCache(cacheKey);
        if (cached.isPresent()) {
            logger.debug("Cache populated by other thread for gene: {}", geneName);
            meterRegistry.counter("cache.retry.hits").increment();
            return cached;
        }

        // Still not in cache - fallback to DB query
        // This shouldn't happen often, log as warning
        logger.warn("Cache still empty after retry for gene: {}, querying DB directly", geneName);
        meterRegistry.counter("cache.retry.misses").increment();
        return geneService.getGeneByName(geneName);
    }

    // ==================== PATTERN-BASED OPERATIONS ====================

    /**
     * Clears cache entries matching the specified Redis pattern.
     *
     * <p>This method provides flexible cache management by allowing targeted
     * clearing of cache entries based on Redis glob patterns. Supports:
     * <ul>
     *   <li>{@code gene:BRCA*} - All genes starting with "BRCA"</li>
     *   <li>{@code gene:*} - All gene entries</li>
     *   <li>{@code gene:??53} - Genes ending with "53" (TP53, etc.)</li>
     * </ul>
     *
     * <p><strong>Security:</strong> Pattern must start with "gene:" prefix to prevent
     * accidental deletion of non-gene cache entries.
     *
     * <p><strong>Java 17 Feature:</strong> Uses switch expressions with pattern
     * matching for cleaner validation and error handling.
     *
     * @param pattern the Redis key pattern using glob syntax, must not be null or blank
     * @return {@link ClearResult} containing operation outcome, deletion count, and status message
     */
    public ClearResult clearByPattern(String pattern) {
        meterRegistry.counter("cache.clears.by_pattern.requests").increment();

        return switch (validatePattern(pattern)) {
            case VALID -> executeClearOperation(pattern);
            case NULL_OR_BLANK -> ClearResult.failure(
                    pattern != null ? pattern : "",
                    "Pattern cannot be null or blank"
            );
            case DANGEROUS_PATTERN -> ClearResult.failure(
                    pattern,
                    "Dangerous pattern detected - too broad or potentially harmful"
            );
            case INVALID_PREFIX -> ClearResult.failure(
                    pattern,
                    "Pattern must start with 'gene:' prefix for security"
            );
            case TOO_LONG -> ClearResult.failure(
                    pattern,
                    "Pattern exceeds maximum allowed length (100 characters)"
            );
            case INVALID_FORMAT -> ClearResult.failure(
                    pattern,
                    "Pattern contains invalid characters or format"
            );
        };
    }

    /**
     * Searches for genes in cache matching the specified pattern.
     *
     * <p>This method scans cached gene keys and returns matching gene records.
     * Useful for:
     * <ul>
     *   <li>Finding all cached genes of a family (e.g., "BRCA")</li>
     *   <li>Cache inspection and debugging</li>
     *   <li>Batch operations on related genes</li>
     * </ul>
     *
     * <p><strong>Performance Note:</strong> Uses Redis SCAN for production safety.
     * For large caches, this may take time. Consider using more specific patterns.
     *
     * @param pattern the search pattern (e.g., "BRCA", "TP5"), case-insensitive
     * @return list of gene records matching the pattern (empty if none found)
     */
    public List<GeneRecord> searchGenesByPattern(String pattern) {
        logger.info("Searching cached genes with pattern: {}", pattern);
        meterRegistry.counter("cache.search.requests").increment();

        if (pattern == null || pattern.trim().isEmpty()) {
            logger.warn("Empty or null search pattern provided");
            meterRegistry.counter("cache.search.invalid").increment();
            return new ArrayList<>();
        }

        String normalizedPattern = pattern.trim().toUpperCase();
        List<GeneRecord> matchingGenes = new ArrayList<>();

        try {
            // Create Redis SCAN pattern
            String scanPattern = redisCacheConfig.keyPrefix() + "*" + normalizedPattern + "*";

            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(scanPattern)
                    .count(100)
                    .build();

            // Scan and collect matching genes
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
                    while (cursor.hasNext()) {
                        String key = new String(cursor.next());
                        Object value = redisTemplate.opsForValue().get(key);

                        if (value instanceof GeneRecord geneRecord) {
                            // Double-check the gene name matches our pattern
                            if (geneRecord.name().toUpperCase().contains(normalizedPattern)) {
                                matchingGenes.add(geneRecord);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error during cache scan for pattern: {}", pattern, e);
                    meterRegistry.counter("cache.errors", "operation", "scan").increment();
                }
                return null;
            });

            logger.info("Found {} cached genes matching pattern: {}", matchingGenes.size(), pattern);
            meterRegistry.counter("cache.search.results", "count", String.valueOf(matchingGenes.size())).increment();
            return matchingGenes;

        } catch (Exception e) {
            logger.error("Error searching genes by pattern: {}", pattern, e);
            meterRegistry.counter("cache.errors", "operation", "search").increment();
            return new ArrayList<>();
        }
    }

    // ==================== MONITORING & HEALTH ====================

    /**
     * Retrieves comprehensive cache status and statistics.
     *
     * <p>This method provides operational visibility into the cache state by
     * collecting key metrics about cached entries. Uses Redis SCAN for production safety.
     *
     * <p><strong>Performance Note:</strong> Uses Redis SCAN command which is production-safe,
     * non-blocking, and handles large datasets efficiently through cursor-based iteration.
     *
     * <p><strong>Error Handling:</strong> If Redis is unavailable, returns empty
     * status rather than throwing exceptions to maintain service availability.
     *
     * @return {@link CacheStatus} containing current cache metrics and availability
     * @see CacheStatus#of(Long, Long, Long, String)
     * @see CacheStatus#empty()
     */
    public CacheStatus getStatus() {
        meterRegistry.counter("cache.status.checks").increment();

        try {
            long geneCount = countKeysByPattern("gene:*");
            logger.debug("Cache status check: {} genes cached", geneCount);
            return CacheStatus.of(geneCount, geneCount, 0L, "AVAILABLE");
        } catch (Exception e) {
            logger.error("Failed to get cache status", e);
            meterRegistry.counter("cache.errors", "operation", "status").increment();
            return CacheStatus.empty();
        }
    }

    /**
     * Checks if a specific gene exists in cache without fetching it.
     *
     * <p>This is a lightweight check using Redis EXISTS command.
     * Useful for cache inspection without triggering database queries.
     *
     * @param geneName the gene name to check
     * @return true if gene exists in cache, false otherwise
     */
    public boolean isGeneInCache(String geneName) {
        meterRegistry.counter("cache.exists.checks").increment();

        try {
            String cacheKey = buildCacheKey(geneName);
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            logger.error("Failed to check if gene is in cache: {}", geneName, e);
            meterRegistry.counter("cache.errors", "operation", "exists").increment();
            return false;
        }
    }

    /**
     * Gets detailed cache information for a specific gene.
     *
     * <p>Returns a human-readable string with cache status and TTL.
     * Useful for debugging and monitoring individual gene cache state.
     *
     * @param geneName the gene name to inspect
     * @return formatted string with cache status and TTL
     */
    public String getGeneCacheInfo(String geneName) {
        try {
            String cacheKey = buildCacheKey(geneName);
            boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));

            if (exists) {
                Long ttl = redisTemplate.getExpire(cacheKey);
                return String.format("Gene %s: ✅ CACHED (TTL: %d seconds)", geneName, ttl);
            } else {
                return String.format("Gene %s: ❌ NOT CACHED", geneName);
            }
        } catch (Exception e) {
            logger.error("Error getting cache info for gene: {}", geneName, e);
            return String.format("Gene %s: 💥 ERROR checking cache", geneName);
        }
    }

    /**
     * Pings Redis to check connectivity and responsiveness.
     *
     * <p>Used by health checks and monitoring to verify Redis availability.
     *
     * @return "PONG" if Redis is healthy
     * @throws RuntimeException if Redis is unreachable or not responding
     */
    public String ping() {
        try {
            meterRegistry.counter("cache.health.checks").increment();
            String response = redisTemplate.getConnectionFactory().getConnection().ping();
            logger.debug("Redis ping successful: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Redis ping failed", e);
            meterRegistry.counter("cache.errors", "operation", "ping").increment();
            throw e;
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Builds a standardized cache key for the given gene name.
     *
     * <p>Cache key format: {@code gene:UPPERCASE_GENE_NAME}
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "tp53"} → {@code "gene:TP53"}</li>
     *   <li>{@code "BRCA1"} → {@code "gene:BRCA1"}</li>
     *   <li>{@code "egfr"} → {@code "gene:EGFR"}</li>
     * </ul>
     *
     * <p><strong>Design Decision:</strong> Gene names are normalized to uppercase
     * to ensure case-insensitive caching (tp53, TP53, Tp53 all use same cache key).
     *
     * @param geneName the gene name to create a cache key for, must not be null
     * @return the formatted cache key string
     */
    private String buildCacheKey(String geneName) {
        String normalizedName = geneName.toUpperCase();
        return String.format("gene:%s", normalizedName);
    }

    /**
     * Counts Redis keys matching a pattern using SCAN for production safety.
     *
     * <p>This method uses Redis SCAN command which:
     * <ul>
     *   <li>Is non-blocking (doesn't freeze Redis)</li>
     *   <li>Uses cursor-based iteration</li>
     *   <li>Handles large datasets efficiently</li>
     *   <li>Provides configurable batch sizes</li>
     * </ul>
     *
     * @param pattern the Redis key pattern to match (e.g., "gene:*")
     * @return count of keys matching the pattern, 0 if error occurs
     */
    private long countKeysByPattern(String pattern) {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            long count = 0;
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
            return count;
        } catch (Exception e) {
            logger.warn("Failed to scan keys with pattern: {}", pattern, e);
            return 0L;
        }
    }

    /**
     * Gets Redis keys matching a pattern using SCAN with configurable limits.
     *
     * <p>Prevents memory issues by limiting the maximum number of keys collected.
     *
     * @param pattern the Redis key pattern to match
     * @param maxKeys maximum number of keys to collect (prevents OOM)
     * @return set of matching keys (limited by maxKeys parameter)
     */
    public Set<String> getKeysByPattern(String pattern, int maxKeys) {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        Set<String> matchingKeys = new HashSet<>();

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext() && matchingKeys.size() < maxKeys) {
                matchingKeys.add(cursor.next());
            }

            if (matchingKeys.size() >= maxKeys) {
                logger.warn("Reached max keys limit ({}) for pattern: {}", maxKeys, pattern);
            }
        } catch (Exception e) {
            logger.warn("Failed to scan keys with pattern: {}", pattern, e);
            meterRegistry.counter("cache.errors", "operation", "scan_keys").increment();
        }

        return matchingKeys;
    }

    /**
     * Deletes Redis keys in batches to avoid overwhelming Redis.
     *
     * <p>Production-safe deletion with:
     * <ul>
     *   <li>Batched operations (1000 keys per batch)</li>
     *   <li>Small delays between batches (10ms)</li>
     *   <li>Graceful handling of interruptions</li>
     * </ul>
     *
     * @param keys the set of keys to delete
     * @return total number of keys actually deleted
     */
    private long deleteKeysInBatches(Set<String> keys) {
        final int BATCH_SIZE = 1000;
        final int DELAY_MS = 10;
        long totalDeleted = 0;

        List<String> keyList = new ArrayList<>(keys);
        int totalBatches = (int) Math.ceil((double) keyList.size() / BATCH_SIZE);

        logger.debug("Deleting {} keys in {} batches", keyList.size(), totalBatches);

        for (int i = 0; i < keyList.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, keyList.size());
            List<String> batch = keyList.subList(i, endIndex);

            Long deleted = redisTemplate.delete(batch);
            totalDeleted += deleted != null ? deleted : 0;

            // Small delay between batches to be gentle on Redis
            if (endIndex < keyList.size()) {
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Batch deletion interrupted after {} keys", totalDeleted);
                    break;
                }
            }
        }

        meterRegistry.counter("cache.deletions.batched", "count", String.valueOf(totalDeleted)).increment();
        return totalDeleted;
    }

    /**
     * Executes the cache clear operation for a given Redis pattern.
     *
     * <p>Scans for keys matching the pattern and deletes them in batches for safety.
     * Returns a detailed result with success/failure status and error messages.
     *
     * @param pattern the Redis key pattern to clear (e.g., "gene:BRCA*")
     * @return {@link ClearResult} indicating the outcome of the operation
     */
    private ClearResult executeClearOperation(String pattern) {
        try {
            Set<String> keysToDelete = getKeysByPattern(pattern, 50_000);

            if (keysToDelete.isEmpty()) {
                logger.info("No cache entries found matching pattern: {}", pattern);
                return ClearResult.success(0L, pattern);
            }

            long totalDeleted = deleteKeysInBatches(keysToDelete);
            logger.info("Cleared {} cache entries for pattern: {}", totalDeleted, pattern);
            meterRegistry.counter("cache.clears.by_pattern.success").increment();

            return ClearResult.success(totalDeleted, pattern);

        } catch (RedisConnectionFailureException e) {
            logger.error("Redis connection failed while clearing cache for pattern: {}", pattern, e);
            meterRegistry.counter("cache.errors", "operation", "clear_pattern", "type", "connection").increment();
            return ClearResult.failure(pattern, "Redis connection failed - cache may be unavailable");

        } catch (RedisSystemException e) {
            logger.error("Redis system error while clearing cache for pattern: {}", pattern, e);
            meterRegistry.counter("cache.errors", "operation", "clear_pattern", "type", "system").increment();
            return ClearResult.failure(pattern, "Redis system error occurred during operation");

        } catch (Exception e) {
            logger.error("Unexpected error while clearing cache for pattern: {}", pattern, e);
            meterRegistry.counter("cache.errors", "operation", "clear_pattern", "type", "unexpected").increment();
            return ClearResult.failure(pattern,
                    "Unexpected error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * Validates Redis pattern format and content for safety.
     *
     * <p>Ensures patterns are safe to use with Redis SCAN and won't accidentally
     * affect non-gene cache entries.
     *
     * @param pattern the pattern to validate
     * @return {@link PatternValidation} enum indicating validation result
     */
    private PatternValidation validatePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return PatternValidation.NULL_OR_BLANK;
        }

        String trimmedPattern = pattern.trim();

        if (isDangerousPattern(trimmedPattern)) {
            return PatternValidation.DANGEROUS_PATTERN;
        }

        if (!trimmedPattern.startsWith("gene:")) {
            return PatternValidation.INVALID_PREFIX;
        }

        if (trimmedPattern.length() > 100) {
            return PatternValidation.TOO_LONG;
        }

        return PatternValidation.VALID;
    }

    /**
     * Checks if a pattern is dangerous and could affect non-gene keys.
     *
     * <p>Prevents patterns that could:
     * <ul>
     *   <li>Match all Redis keys (e.g., "*")</li>
     *   <li>Escape gene namespace (e.g., starts with wildcard)</li>
     *   <li>Contain dangerous characters (newlines, spaces)</li>
     * </ul>
     *
     * @param pattern the pattern to check
     * @return true if pattern is dangerous, false otherwise
     */
    private boolean isDangerousPattern(String pattern) {
        // Patterns that could match everything
        if ("*".equals(pattern) || "?".equals(pattern)) {
            return true;
        }

        // Patterns that don't start with gene prefix
        if (pattern.startsWith("*") || pattern.startsWith("?")) {
            return true;
        }

        // Patterns with dangerous characters
        return pattern.contains(" ") || pattern.contains("\n") || pattern.contains("\r");
    }

    // ==================== INNER CLASSES & ENUMS ====================

    /**
     * Validation states for Redis patterns.
     *
     * <p>Enumeration defining possible validation outcomes for Redis key patterns
     * used in cache clearing operations. Used internally by pattern validation logic.
     *
     * @since 1.0.0
     */
    private enum PatternValidation {
        /** Pattern is valid and safe to use */
        VALID,
        /** Pattern is null or contains only whitespace */
        NULL_OR_BLANK,
        /** Pattern has invalid format */
        INVALID_FORMAT,
        /** Pattern is dangerous and could affect non-gene keys */
        DANGEROUS_PATTERN,
        /** Pattern doesn't start with required gene: prefix */
        INVALID_PREFIX,
        /** Pattern exceeds maximum allowed length */
        TOO_LONG
    }

    /**
     * Configuration record for Redis cache behavior.
     *
     * <p>This record encapsulates cache configuration parameters using
     * Java 17's record feature for immutable data classes.
     *
     * @param defaultTimeToLive the default TTL for cache entries
     * @param keyPrefix         the prefix used for all cache keys (e.g., "gene:")
     * @param enableFallback    whether to fall back to database on cache failures
     */
    public record CacheConfig(
            Duration defaultTimeToLive,
            String keyPrefix,
            boolean enableFallback
    ) {
    }
}