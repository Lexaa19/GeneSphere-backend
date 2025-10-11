package com.gene.sphere.geneservice.service;

import com.gene.sphere.geneservice.model.GeneRecord;

import java.util.Optional;

/**
 * Service interface for caching gene data operations.
 * 
 * <p>This interface defines the contract for caching {@link GeneRecord} objects
 * to improve application performance by reducing database queries. Implementations
 * should provide efficient storage and retrieval mechanisms for gene data.
 * 
 * <p><strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Cache-Aside:</strong> Implementations should check cache first, 
 *       fall back to data source on cache miss</li>
 *   <li><strong>Write-Through/Write-Behind:</strong> Cache updates should be 
 *       coordinated with the underlying data store</li>
 * </ul>
 * 
 * <p><strong>Expected Behavior:</strong>
 * <ul>
 *   <li>Case-insensitive gene name handling (TP53, tp53, Tp53 should be equivalent)</li>
 *   <li>Graceful handling of null/empty inputs</li>
 *   <li>Thread-safe operations for concurrent access</li>
 *   <li>Proper cache invalidation when data changes</li>
 * </ul>
 * 
 */
public interface CacheService {
    
    /**
     * Retrieves a gene record by its name from the cache.
     * 
     * <p>This method should implement a cache-aside pattern:
     * <ol>
     *   <li>Check cache for the requested gene</li>
     *   <li>If found (cache hit), return the cached {@link GeneRecord}</li>
     *   <li>If not found (cache miss), fetch from the underlying data source</li>
     *   <li>Store the fetched data in cache for future requests</li>
     *   <li>Return the gene record</li>
     * </ol>
     * 
     * <p><strong>Input Handling:</strong>
     * <ul>
     *   <li>Gene names should be treated case-insensitively</li>
     *   <li>Leading/trailing whitespace should be trimmed</li>
     *   <li>Null or blank inputs should return {@link Optional#empty()}</li>
     * </ul>
     * 
     * <p><strong>Performance Expectations:</strong>
     * <ul>
     *   <li>Cache hits should be significantly faster than database queries</li>
     *   <li>Cache misses may be slightly slower due to cache population overhead</li>
     *   <li>Implementations should use appropriate TTL (Time To Live) strategies</li>
     * </ul>
     * 
     * @param name the gene name to search for (case-insensitive), 
     *             standard gene symbols like "TP53", "BRCA1", "EGFR" expected
     * @return an {@link Optional} containing the {@link GeneRecord} if found,
     *         or {@link Optional#empty()} if the gene doesn't exist or input is invalid
     * @throws CacheException if cache operations fail and no fallback is available
     * @throws IllegalStateException if the cache service is not properly initialized
     * 
     * @example
     * <pre>{@code
     * // Typical usage
     * Optional<GeneRecord> gene = cacheService.getGeneByName("TP53");
     * if (gene.isPresent()) {
     *     System.out.println("Gene found: " + gene.get().name());
     * } else {
     *     System.out.println("Gene not found");
     * }
     * 
     * // Case insensitive
     * Optional<GeneRecord> same = cacheService.getGeneByName("tp53"); // Same result
     * }</pre>
     */
    Optional<GeneRecord> getGeneByName(String name);
    
    /**
     * Removes a specific gene from the cache.
     * 
     * <p>This operation invalidates the cached entry for the specified gene,
     * forcing the next {@link #getGeneByName(String)} call to fetch fresh data
     * from the underlying data source. This is essential for maintaining data
     * consistency when gene information is updated.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>After updating gene data in the database</li>
     *   <li>When implementing cache invalidation policies</li>
     *   <li>For testing scenarios requiring fresh data</li>
     *   <li>Manual cache management operations</li>
     * </ul>
     * 
     * <p><strong>Behavior:</strong>
     * <ul>
     *   <li>If the gene exists in cache: removes it successfully</li>
     *   <li>If the gene doesn't exist in cache: operation is idempotent (no error)</li>
     *   <li>Gene name matching should follow same rules as {@link #getGeneByName(String)}</li>
     * </ul>
     * 
     * @param geneName the name of the gene to remove from cache,
     *                 must not be null or empty
     * @throws IllegalArgumentException if geneName is null, empty, or blank
     * @throws CacheException if cache eviction operation fails
     * 
     * @example
     * <pre>{@code
     * // After updating TP53 data in database
     * cacheService.evictGene("TP53");
     * 
     * // Next call will fetch fresh data
     * Optional<GeneRecord> freshGene = cacheService.getGeneByName("TP53");
     * }</pre>
     */
    void evictGene(String geneName);
    
    /**
     * Clears all gene records from the cache.
     * 
     * <p>This operation removes all cached gene entries, effectively resetting
     * the cache to an empty state. All subsequent gene lookups will result in
     * cache misses until the cache is repopulated through normal usage.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Bulk data refresh after database migrations</li>
     *   <li>Cache maintenance and cleanup operations</li>
     *   <li>Testing scenarios requiring clean cache state</li>
     *   <li>Memory management when cache grows too large</li>
     *   <li>Application restart preparation</li>
     * </ul>
     * 
     * <p><strong>Performance Impact:</strong>
     * <ul>
     *   <li>Immediate: Cache hit rate drops to 0% temporarily</li>
     *   <li>Short-term: Increased database load as cache repopulates</li>
     *   <li>Long-term: Normal performance once frequently accessed genes are cached</li>
     * </ul>
     * 
     * <p><strong>Implementation Considerations:</strong>
     * <ul>
     *   <li>Should only clear gene-related cache entries, not other application caches</li>
     *   <li>Should be atomic operation where possible</li>
     *   <li>Should handle large cache sizes efficiently</li>
     *   <li>Should be safe to call during normal application operation</li>
     * </ul>
     * 
     * @throws CacheException if cache clearing operation fails
     * @throws SecurityException if operation requires special permissions
     * 
     * @example
     * <pre>{@code
     * // Before database migration
     * cacheService.clearCache();
     * 
     * // Perform database updates...
     * // migrateGeneDatabase();
     * 
     * // Cache will repopulate with fresh data on next requests
     * Optional<GeneRecord> gene = cacheService.getGeneByName("TP53");
     * }</pre>
     */
    void clearCache();
}