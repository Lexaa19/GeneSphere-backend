package com.gene.sphere.geneservice.cache;


/**
 * Result record for cache clearing operations in the Redis cache service.
 *
 * <p>This immutable record encapsulates the outcome of cache clearing operations,
 * providing detailed information about the success or failure of the operation,
 * the number of entries affected, and any relevant messages for debugging or
 * operational monitoring.</p>
 *
 * <p>This record is used by cache management methods such as:
 * <ul>
 *   <li>{@link RedisCacheService#clearCache()}</li>
 *   <li>{@link RedisCacheService#clearByPattern(String)}</li>
 *   <li>Future bulk cache operations</li>
 * </ul>
 *
 * <p><strong>Design Philosophy:</strong> This record follows the principle of
 * providing comprehensive operation feedback, allowing calling code to make
 * informed decisions about retry logic, logging, and error handling.</p>
 *
 * @param success      true if the clearing operation completed successfully, false otherwise
 * @param deletedCount the number of cache entries that were actually deleted from Redis
 * @param message      human-readable message describing the operation result or error details
 * @param timestamp    Unix timestamp (milliseconds) when the clear operation was executed
 * @param pattern      the Redis key pattern that was used for the clear operation (optional)
 *
 * @see com.gene.sphere.geneservice.cache.RedisCacheService
 * @see com.gene.sphere.geneservice.cache.CacheStatus
 */
public record ClearResult(
        boolean success,
        long deletedCount,
        String message,
        long timestamp,
        String pattern
) {
    
    /**
     * Compact constructor that validates fundamental invariants.
     * This ensures all ClearResult instances are valid, regardless of how they're created.
     */
    public ClearResult {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Pattern cannot be null or blank");
        }
        if (deletedCount < 0) {
            throw new IllegalArgumentException("Deleted count cannot be negative: " + deletedCount);
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("Timestamp must be positive: " + timestamp);
        }
    }

    /**
     * Creates a successful clear result with the specified deletion count and pattern.
     *
     * <p>This factory method should be used when a cache clearing operation
     * completes successfully. It automatically sets the timestamp to the current
     * system time and generates an appropriate success message.</p>
     *
     * <p><strong>Usage Example:</strong></p>
     * <pre>{@code
     * // After successfully deleting 150 cache entries
     * ClearResult result = ClearResult.success(150L, "gene:*");
     * assert result.success() == true;
     * assert result.deletedCount() == 150L;
     * }</pre>
     *
     * @param deletedCount the number of cache entries that were successfully deleted, must be >= 0
     * @param pattern      the Redis key pattern used for the operation, must not be null
     * @return a new ClearResult indicating successful operation
     * @throws IllegalArgumentException if deletedCount is negative or pattern is null (via compact constructor)
     */
    public static ClearResult success(long deletedCount, String pattern) {
        String message = deletedCount == 0
                ? String.format("No cache entries found matching pattern '%s'", pattern)
                : String.format("Successfully cleared %d cache entries matching pattern '%s'", deletedCount, pattern);

        return new ClearResult(true, deletedCount, message, System.currentTimeMillis(), pattern);
    }

    /**
     * Creates a failed clear result with error information.
     *
     * @param pattern the Redis key pattern that was attempted, must not be null
     * @param errorMessage the error message describing what went wrong, must not be null
     * @return a new ClearResult indicating failed operation
     * @throws IllegalArgumentException if pattern or errorMessage is null (via compact constructor)
     */
    public static ClearResult failure(String pattern, String errorMessage) {
        String message = String.format("Failed to clear cache entries for pattern '%s': %s",
                pattern, errorMessage);
        return new ClearResult(false, 0L, message, System.currentTimeMillis(), pattern);
    }
    /**
     * Creates a clear result for operations that completed with partial success.
     *
     * <p>This factory method handles scenarios where some cache entries were
     * deleted successfully, but the operation encountered issues that prevented
     * complete success. This provides more nuanced feedback than simple
     * success/failure states.</p>
     *
     * @param partialCount the number of entries successfully deleted before failure
     * @param pattern      the Redis key pattern used for the operation
     * @param warningMessage descriptive message about the partial success
     * @return a new ClearResult indicating partial success
     */
    public static ClearResult partialSuccess(long partialCount, String pattern, String warningMessage) {
        String message = String.format("Partially cleared %d cache entries for pattern '%s': %s",
                partialCount, pattern, warningMessage);
        return new ClearResult(true, partialCount, message, System.currentTimeMillis(), pattern);
    }

    /**
     * Checks if any cache entries were actually deleted during the operation.
     *
     * <p>This convenience method helps distinguish between operations that
     * succeeded but found no matching entries versus operations that deleted
     * actual cache data.</p>
     *
     * @return true if at least one cache entry was deleted, false otherwise
     */
    public boolean hasDeletedEntries() {
        return deletedCount > 0;
    }

    /**
     * Checks if the operation completed without errors, regardless of deletion count.
     *
     * <p>A successful operation with zero deletions (no matching keys found)
     * is still considered successful from an operational standpoint.</p>
     *
     * @return true if the operation completed successfully, false if errors occurred
     */
    public boolean isSuccessful() {
        return success;
    }

    /**
     * Gets the deletion rate as a formatted percentage string.
     *
     * <p>This method is useful for monitoring and reporting purposes when
     * you know the expected number of entries that should have been deleted.</p>
     *
     * @param expectedCount the expected number of entries to delete
     * @return formatted percentage string (e.g., "75.5%")
     */
    public String getDeletionRate(long expectedCount) {
        if (expectedCount <= 0) {
            return "N/A";
        }
        double rate = (double) deletedCount / expectedCount * 100;
        return String.format("%.1f%%", rate);
    }

    /**
     * Returns a human-readable string representation of the clear operation result.
     *
     * <p>The format includes success status, deletion count, pattern, and timestamp
     * in a format suitable for logging and debugging purposes.</p>
     *
     * @return formatted string with operation details
     */
    @Override
    public String toString() {
        return String.format(
                "ClearResult{success=%s, deleted=%d, pattern='%s', timestamp=%d, message='%s'}",
                success, deletedCount, pattern, timestamp, message
        );
    }
}