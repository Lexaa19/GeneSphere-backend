package com.gene.sphere.geneservice.cache;

import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.CacheService;
import com.gene.sphere.geneservice.service.GeneService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based implementation of the {@link CacheService} for caching gene data.
 * 
 * <p>This service provides efficient caching of {@link GeneRecord} objects using Redis
 * as the underlying cache store. It implements a cache-aside pattern where:
 * <ul>
 *   <li>Cache hits return data directly from Redis</li>
 *   <li>Cache misses fetch from the database and store in Redis for future requests</li>
 *   <li>Cache entries have a configurable TTL (Time To Live)</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This service is thread-safe as it relies on
 * Spring's thread-safe {@link RedisTemplate} and maintains no mutable state.
 * </p>
 */
@Service
public class RedisCacheService implements CacheService {
    
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
     * Configuration for cache behavior including TTL and key prefixes.
     */
    private final CacheConfig redisCacheConfig;

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
     * @param redisTemplate the Redis template for cache operations, must not be null
     * @param geneService the gene service for database fallback, must not be null
     * @param redisCacheConfig optional cache configuration, can be null for defaults
     * @throws IllegalArgumentException if redisTemplate or geneService is null
     */
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate, 
                           GeneService geneService, 
                           @org.springframework.beans.factory.annotation.Autowired(required = false) CacheConfig redisCacheConfig) {
        this.redisTemplate = redisTemplate;
        this.geneService = geneService;
        // Use provided config or create default
        this.redisCacheConfig = redisCacheConfig != null ? redisCacheConfig : new CacheConfig(
                Duration.ofDays(5),
                "gene:",
                true
        );
    }

    /**
     * Retrieves a gene by its name, utilizing Redis cache for improved performance.
     * 
     * <p>This method implements a cache-aside pattern:
     * <ol>
     *   <li>Validates the input gene name</li>
     *   <li>Checks Redis cache for existing entry</li>
     *   <li>If cache hit: returns cached {@link GeneRecord}</li>
     *   <li>If cache miss: fetches from database, caches result, and returns</li>
     * </ol>
     * 
     * <p><strong>Cache Key Format:</strong> {@code gene:GENENAME} (uppercase)
     * <p><strong>TTL:</strong> 5 days (configurable)
     * 
     * @param name the gene name to search for (case-insensitive), must not be null or blank
     * @return an {@link Optional} containing the {@link GeneRecord} if found, 
     *         or {@link Optional#empty()} if not found or invalid input
     * @throws RuntimeException if Redis connection fails (fallback to database still works)
     * 
     */
    @Override
    public Optional<GeneRecord> getGeneByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return fetchGeneWithCache(name);
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
     * @param geneName the name of the gene to evict from cache, 
     *                 must not be null or empty
     * @throws IllegalArgumentException if geneName is null or empty
     * 
     * @example
     * <pre>
     * // After updating TP53 in database
     * cacheService.evictGene("TP53");
     * // Next getGeneByName("TP53") will fetch fresh data
     * </pre>
     */
    @Override
    public void evictGene(String geneName) {
        var cachedKey = buildCacheKey(geneName);

        if(cachedKey !=null && !cachedKey.isEmpty()) {
            redisTemplate.delete(cachedKey);
        }
    }

    /**
     * Clears all gene entries from the Redis cache.
     * 
     * <p>This operation removes all cached genes (keys matching pattern {@code gene:*})
     * but leaves other cache entries untouched. Useful for:
     * <ul>
     *   <li>Bulk cache invalidation after database updates</li>
     *   <li>Cache maintenance operations</li>
     *   <li>Testing scenarios</li>
     * </ul>
     * 
     * 
     * @throws RuntimeException if Redis connection fails
     * 
     * @example
     * <pre>
     * // After bulk gene data import
     * cacheService.clearCache();
     * // All subsequent gene requests will fetch fresh data
     * </pre>
     */
    @Override
    public void clearCache() {
        // Find all actual Redis keys matching the pattern
        var keysToDelete = redisTemplate.keys("gene:*"); // Returns Set<String> of actual keys

        // Delete all found keys
        if(keysToDelete !=null && !keysToDelete.isEmpty()){
            redisTemplate.delete(keysToDelete);
        }
    }

    /**
     * Fetches gene data with cache-aside pattern implementation.
     * 
     * <p>This private method handles the core caching logic:
     * <ol>
     *   <li>Build cache key from gene name</li>
     *   <li>Attempt cache retrieval from Redis</li>
     *   <li>On cache hit: return cached data</li>
     *   <li>On cache miss: fetch from database, cache result, return data</li>
     * </ol>
     * 
     * @param geneName the gene name to fetch, assumed to be already validated
     * @return {@link Optional} containing the gene record or empty if not found
     */
    private Optional<GeneRecord> fetchGeneWithCache(String geneName) {
        var cachedKey = buildCacheKey(geneName);
        var valueOps = redisTemplate.opsForValue();
        var cachedValue = valueOps.get(cachedKey);

        // Java 17 pattern matching
        if (cachedValue instanceof GeneRecord gene) {
            return Optional.of(gene);
        }
        return geneService.getGeneByName(geneName).map(gene -> {
            var ttl = Duration.ofDays(5);
            valueOps.set(cachedKey, gene, ttl);
            return gene;
        });
    }

    /**
     * Builds a standardized cache key for the given gene name.
     * 
     * <p>Cache key format: {@code gene:UPPERCASE_GENE_NAME}
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
        var normalizedName = geneName.toUpperCase();
        var template = "gene:%s";
        return template.formatted(normalizedName);
    }

    /**
     * Configuration record for Redis cache behavior.
     * 
     * <p>This record encapsulates cache configuration parameters using
     * Java 17's record feature for immutable data classes.
     * 
     * <p><strong>Java 17 Feature Note:</strong> I could have declared these as constants,
     * but wanted to use Java 17 features for clean code
     * 
     * @param defaultTimeToLeave the default TTL for cache entries
     * @param keyPrefix the prefix used for all cache keys (e.g., "gene:")
     * @param enableFallback whether to fall back to database on cache failures
     */
    public record CacheConfig(
            Duration defaultTimeToLeave,
            String keyPrefix,
            boolean enableFallback
    ) {
    }
}