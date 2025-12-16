package com.gene.sphere.geneservice.controller;

import com.gene.sphere.geneservice.cache.RedisCacheService;
import com.gene.sphere.geneservice.model.GeneRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/genes")
public class GeneController {

    private static final Logger logger = LoggerFactory.getLogger(GeneController.class);

    @Autowired
    private RedisCacheService cacheService;

    /**
     * Get gene by name - REAL DATABASE + CACHE
     * This will:
     * 1. Check Redis cache first
     * 2. If not in cache, query database
     * 3. Store result in cache
     * 4. Return the gene
     */
    @GetMapping("/{name}")
    public ResponseEntity<GeneRecord> getGene(@PathVariable String name) {
        logger.info("Searching for gene: {}", name);
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Optional<GeneRecord> gene = cacheService.getGeneByName(name);
            if (gene.isPresent()) {
                return ResponseEntity.ok(gene.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error searching for gene: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if gene is in cache WITHOUT querying database
     */
    @GetMapping("/{name}/cached")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> isGeneCached(@PathVariable String name) {
        try {
            boolean inCache = cacheService.isGeneInCache(name);
            String message = String.format("Gene %s is %s in cache",
                    name, inCache ? "FOUND" : "❌ NOT FOUND");
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error checking cache for: " + name);
        }
    }

    /**
     * Force refresh - bypass cache and fetch from database
     */
    @GetMapping("/{name}/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GeneRecord> refreshGene(@PathVariable String name) {
        logger.info("Force refreshing gene from database: {}", name);

        try {
            // First evict from cache
            cacheService.evictGene(name);

            // Then fetch fresh from database (will auto-cache)
            var gene = cacheService.getGeneByName(name);

            if (gene.isPresent()) {
                logger.info("Refreshed gene: {}", name);
                return ResponseEntity.ok(gene.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error refreshing gene: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearAllGenesCached() {
        cacheService.clearByPattern("gene:*");
        return ResponseEntity.ok("All genes cleared from cache");
    }

}