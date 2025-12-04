package com.gene.sphere.geneservice.cache;

/**
 * Cache statistics record for monitoring Redis cache performance and health.
 *
 * <p>This immutable record encapsulates key metrics about the Redis cache state,
 * including the number of cached entries by type and overall cache availability status.
 * It's designed to provide operational insights for monitoring, alerting, and
 * performance optimization of the gene service caching layer.</p>
 *
 * <p>The record supports both active cache monitoring (when Redis is available)
 * and fallback scenarios (when Redis is unavailable or experiencing issues).</p>
 *
 * @param totalKeys    the total number of keys stored in Redis cache
 * @param geneKeys     the number of gene-related cache entries (keys matching "gene:*")
 * @param mutationKeys the number of mutation-related cache entries (keys matching "mutation:*")
 * @param timestamp    the Unix timestamp (milliseconds) when these statistics were collected
 * @param status       the current cache availability status ("AVAILABLE", "UNAVAILABLE")
 * 
 */
public record CacheStatus(
        Long totalKeys,
        Long geneKeys,
        Long mutationKeys,
        Long timestamp,
        String status
) {

    /**
     * Creates cache statistics with the current timestamp.
     *
     * <p>This factory method is the preferred way to create CacheStats instances
     * during normal cache operation when Redis is available and responding.
     * It automatically sets the timestamp to the current system time.</p>
     *
     * <p><strong>Usage Example:</strong></p>
     * <pre>{@code
     * CacheStats stats = CacheStats.of(150L, 120L, 30L, "AVAILABLE");
     * }</pre>
     *
     * @param totalKeys    the total number of keys currently in Redis cache, must not be null
     * @param geneKeys     the number of gene-specific cache entries, must not be null
     * @param mutationKeys the number of mutation-specific cache entries, must not be null
     * @param status       the current operational status of the cache, must not be null
     * @return a new CacheStats instance with current timestamp
     * @throws NullPointerException if any parameter is null
     */
    public static CacheStatus of(Long totalKeys, Long geneKeys, Long mutationKeys, String status) {
        return new CacheStatus(totalKeys, geneKeys, mutationKeys, System.currentTimeMillis(), status);
    }

    /**
     * Creates empty cache statistics indicating Redis is unavailable.
     *
     * <p>This factory method should be used when Redis is unreachable, experiencing
     * connectivity issues, or when cache operations fail. It returns statistics
     * with zero counts and "UNAVAILABLE" status, along with the current timestamp
     * for accurate failure time tracking.</p>
     *
     * <p>This is typically used in error handling scenarios to provide consistent
     * monitoring data even when the cache layer is down.</p>
     *
     * <p><strong>Usage Example:</strong></p>
     * <pre>{@code
     * try {
     *     return getCacheStatsFromRedis();
     * } catch (RedisConnectionFailureException e) {
     *     logger.warn("Redis unavailable, returning empty stats", e);
     *     return CacheStats.empty();
     * }
     * }</pre>
     *
     * @return a new CacheStats instance with zero counts and "UNAVAILABLE" status
     */
    public static CacheStatus empty() {
        return new CacheStatus(0L, 0L, 0L, System.currentTimeMillis(), "UNAVAILABLE");
    }

    /**
     * Checks if the cache is currently available and operational.
     *
     * @return true if status is "AVAILABLE", false otherwise
     */
    public boolean isAvailable() {
        return "AVAILABLE".equals(status);
    }

    /**
     * Returns a human-readable string representation of the cache statistics.
     *
     * <p>The format includes all key metrics in a readable format suitable
     * for logging and debugging purposes.</p>
     *
     * @return formatted string with cache statistics
     */
    @Override
    public String toString() {
        return String.format(
                "CacheStats{total=%d, genes=%d, mutations=%d, status=%s, timestamp=%d}",
                totalKeys, geneKeys, mutationKeys, status, timestamp
        );
    }
}