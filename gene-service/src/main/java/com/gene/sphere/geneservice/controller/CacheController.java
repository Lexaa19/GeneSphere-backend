package com.gene.sphere.geneservice.controller;

import com.gene.sphere.geneservice.cache.RedisCacheService;
import com.gene.sphere.geneservice.cache.CacheStatus;
import com.gene.sphere.geneservice.cache.ClearResult;
import com.gene.sphere.geneservice.config.RedisHealthIndicator;
import com.gene.sphere.geneservice.model.GeneRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller for managing gene cache operations.
 * Provides endpoints for cache status, clearing cache, searching, health checks, and listing cache keys.
 * Used only by admin, not exposed to clients.
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {
    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private RedisCacheService cacheService;

    @Autowired
    private RedisHealthIndicator redisHealthIndicator;

    @Value("${cache.max-keys:1000}")
    private int maxKeys;

    /**
     * Get the current status of the cache.
     * @return CacheStatus object with availability info.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CacheStatus> getCacheStatus() {
        try {
            var cacheStatus = cacheService.getStatus();
            logger.info("Cache status retrieved successfully: available={}", cacheStatus.isAvailable());
            return ResponseEntity.ok(cacheStatus); // Always return OK - let the response body show availability
        } catch (Exception e) {
            logger.error("Error retrieving cache status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clear all gene-related entries from the cache.
     * @return ClearResult with details of the operation.
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClearResult> clearGenes(@RequestParam(required = false) String pattern) {
        try {
            if (pattern != null && !pattern.startsWith("gene:")) {
                return ResponseEntity.badRequest().body(
                        ClearResult.failure(pattern, "Pattern must start with 'gene:' prefix for security")
                );
            }
            String actualPattern = pattern != null ? pattern : "gene:*";
            var clearResult = cacheService.clearByPattern(actualPattern);
            return ResponseEntity.ok(clearResult);
        } catch (Exception e) {
            logger.error("Error clearing cache with pattern: {}", pattern, e);
            return ResponseEntity.internalServerError().body(
                    ClearResult.failure(pattern != null ? pattern : "unknown",
                            "Internal server error: " + e.getMessage())
            );
        }
    }
    /**
     * Clear cache for a specific gene.
     * @param geneName Name of the gene to clear from cache.
     * @return ClearResult with details of the operation.
     */
    @DeleteMapping("/clear/{geneName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClearResult> clearGene(@PathVariable String geneName) {
        try {
            if (geneName == null || geneName.trim().isEmpty()) {
                logger.warn("Invalid gene name provided for cache clearing");
                return ResponseEntity.badRequest().build();
            }
            var clearResult = cacheService.clearByPattern("gene:" + geneName.toUpperCase());
            return ResponseEntity.ok(clearResult);
        } catch (Exception e) {
            logger.error("Error clearing cache", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search for genes in the cache by pattern.
     * @param pattern Pattern to match gene keys.
     * @return List of matching GeneRecord objects.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchGenes(@RequestParam String pattern) {
        try {
            // Avoid * as it is a wildcard that matches all the possible keys in the cache.
            // Can cause performance issues by returning a huge result
            // Potentially expose all the cached data, so the security could be at risk
            if ("*".equals(pattern) || pattern == null || pattern.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "Invalid pattern"));
            }
            List<GeneRecord> matchingGenes = cacheService.searchGenesByPattern(pattern);
            logger.info("Found {} genes matching pattern: {}", matchingGenes.size(), pattern);
            return ResponseEntity.ok(matchingGenes);
        } catch (Exception e) {
            logger.error("Error searching genes with pattern: {}", pattern, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check the health status of the Redis cache.
     * @return Map with health status and details.
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Health health = redisHealthIndicator.health();
            Map<String, Object> result = new HashMap<>();
            result.put("status", health.getStatus().getCode());
            result.put("details", health.getDetails());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "DOWN");
            errorResult.put("error", e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * List cache keys matching a given pattern.
     * @param pattern Redis key pattern (default: gene:*)
     * @param maxKeys Maximum number of keys to return (default: 1000)
     * @return List of matching cache key strings.
     */
    @GetMapping("/keys")
    public ResponseEntity<List<String>> getCacheKeys(@RequestParam(defaultValue = "gene:*") String pattern, @RequestParam(defaultValue = "1000") int maxKeys) {
        try {
            if (pattern == null || pattern.trim().isEmpty()) {
                logger.warn("Empty or null pattern provided");
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            if (maxKeys < 1 || maxKeys > 1000) {
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            Set<String> keysSet = cacheService.getKeysByPattern(pattern, maxKeys);
            List<String> keys = new ArrayList<>(keysSet);
            logger.info("Retrieved {} cache keys", keys.size());
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            logger.error("Failed to get cache keys", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Internal server error");
            return ResponseEntity.status(500).body((List<String>) error);
        }
    }
}