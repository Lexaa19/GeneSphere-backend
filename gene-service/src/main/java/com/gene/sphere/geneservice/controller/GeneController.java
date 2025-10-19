package com.gene.sphere.geneservice.controller;

import com.gene.sphere.geneservice.model.Gene;
import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.GeneServiceInterface;
import com.gene.sphere.geneservice.service.GeneDataIngestionService;
import com.gene.sphere.geneservice.cache.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller that exposes endpoints for retrieving {@link Gene} resources.
 * <p>
 * Base path: /genes
 * <br>
 * Uses cached service for improved performance.
 */
@RestController
@RequestMapping("/genes")
public class GeneController {

    /**
     * Service layer component - using cached implementation for better performance.
     */
    @Autowired
    private GeneServiceInterface geneService;

    @Autowired
    private GeneDataIngestionService geneDataIngestionService;

    @Autowired
    private RedisCacheService cacheService;

    /**
     * Retrieves a {@link Gene} by its unique name.
     * <p>
     * Example request: GET /genes/{name}
     * This endpoint now uses Redis caching for improved performance!
     *
     * @param name the unique gene name to look up (e.g., "TP53")
     * @return a {@link ResponseEntity} containing:
     * <ul>
     *   <li>200 OK with the {@link Gene} in the response body if found</li>
     *   <li>404 Not Found if no gene with the given name exists</li>
     * </ul>
     */
    @GetMapping("/{name}")
    public ResponseEntity<GeneRecord> getGene(@PathVariable String name) {
        return cacheService.getGeneByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all genes.
     */
    @GetMapping
    public ResponseEntity<List<GeneRecord>> getAllGenes() {
        List<GeneRecord> genes = geneService.getAllGenes();
        return ResponseEntity.ok(genes);
    }

    /**
     * Creates a new gene.
     */
    @PostMapping
    public ResponseEntity<GeneRecord> createGene(@RequestBody GeneRecord geneRecord) {
        GeneRecord created = geneService.createGene(geneRecord);
        return ResponseEntity.ok(created);
    }

    /**
     * Updates an existing gene.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GeneRecord> updateGene(@PathVariable Integer id, @RequestBody GeneRecord geneRecord) {
        GeneRecord updated = geneService.updateGene(id, geneRecord);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a gene by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGene(@PathVariable Integer id) {
        geneService.deleteGene(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Administrative endpoint to populate database with lung cancer genes.
     * Call this once to populate your empty genes table.
     */
    @PostMapping("/admin/populate-lung-cancer-genes")
    public ResponseEntity<String> populateLungCancerGenes() {
        try {
            geneDataIngestionService.populateLungCancerGenes();
            return ResponseEntity.ok("✅ Successfully populated database with lung cancer genes");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to populate genes: " + e.getMessage());
        }
    }

    // ===== REDIS CACHE TESTING ENDPOINTS =====

    /**
     * Clears all cached genes from Redis.
     * Useful for testing cache behavior.
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        try {
            cacheService.clearCache();
            return ResponseEntity.ok("✅ Redis cache cleared successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to clear cache: " + e.getMessage());
        }
    }

    /**
     * Evicts a specific gene from the Redis cache.
     */
    @DeleteMapping("/cache/{name}")
    public ResponseEntity<String> evictGeneFromCache(@PathVariable String name) {
        try {
            cacheService.evictGene(name);
            return ResponseEntity.ok("✅ Gene '" + name + "' evicted from cache");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to evict gene from cache: " + e.getMessage());
        }
    }

}