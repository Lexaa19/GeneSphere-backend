package com.gene.sphere.geneservice.controller;

import com.gene.sphere.geneservice.cache.ClearResult;
import com.gene.sphere.geneservice.cache.RedisCacheService;
import com.gene.sphere.geneservice.config.RedisHealthIndicator;
import com.gene.sphere.geneservice.model.Gene;
import com.gene.sphere.geneservice.model.GeneRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(CacheController.class)
class CacheControllerTest {

    @MockBean
    private RedisCacheService redisCacheService;

    @MockBean
    private RedisHealthIndicator redisHealthIndicator;
    // MockMvc allows you to test your Spring MVC controllers without starting a real server.
    // It simulates HTTP requests and checks responses, status codes, headers, and content.
    @Autowired
    private MockMvc mockMvc;

    // test data fields
    private Gene tp53Gene;
    private GeneRecord tp53GeneRecord;
    private Gene krasGene;
    private GeneRecord krasGeneRecord;

    @BeforeEach
    void setUp() {
        // Create the TP53 gene entity
        tp53Gene = new Gene();
        tp53Gene.setId(1);
        tp53Gene.setName("TP53");
        tp53Gene.setDescription("Tumor suppressor gene");
        tp53Gene.setNormalFunction("DNA repair and cell cycle control");
        tp53Gene.setMutationEffect("Loss of tumor suppression function");
        tp53Gene.setPrevalence("50% of all cancers");
        tp53Gene.setTherapies("None specific; clinical trials ongoing");
        tp53Gene.setResearchLinks("http://example.com/tp53");

        // Create TP53 GeneRecord DTO (what service should return)
        tp53GeneRecord = new GeneRecord(
                "TP53",
                "Tumor suppressor gene",
                "DNA repair and cell cycle control",
                "Loss of tumor suppression function",
                "50% of all cancers",
                "None specific; clinical trials ongoing",
                "http://example.com/tp53"
        );

        // Create the KRAS gene entity
        krasGene = new Gene();
        krasGene.setId(2);
        krasGene.setName("KRAS");
        krasGene.setDescription("Oncogene involved in cell signaling");
        krasGene.setNormalFunction("Regulates cell division and growth");
        krasGene.setMutationEffect("Promotes uncontrolled cell growth");
        krasGene.setPrevalence("30% of lung cancers, 40% of colorectal cancers");
        krasGene.setTherapies("KRAS G12C inhibitors (sotorasib, adagrasib)");
        krasGene.setResearchLinks("http://example.com/kras");

        // Create KRAS GeneRecord DTO (what service should return)
        krasGeneRecord = new GeneRecord(
                "KRAS",
                "Oncogene involved in cell signaling",
                "Regulates cell division and growth",
                "Promotes uncontrolled cell growth",
                "30% of lung cancers, 40% of colorectal cancers",
                "KRAS G12C inhibitors (sotorasib, adagrasib)",
                "http://example.com/kras"
        );
    }

    @Test
    void getGene_ShouldReturnGene_whenFoundInCache() throws Exception {
        // Arrange
        when(redisCacheService.searchGenesByPattern("TP53")).thenReturn(List.of(tp53GeneRecord));

        // Act & Assert
        mockMvc.perform(get("/api/cache/search?pattern=TP53"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("TP53"));

        // Verify correct interaction
        verify(redisCacheService).searchGenesByPattern("TP53");
    }

    @Test
    void searchGenes_ShouldReturnEmptyList_WhenNoMatch() throws Exception {
        // Arrange
        when(redisCacheService.searchGenesByPattern("TPP")).thenReturn(List.of());

        // Act and assert
        mockMvc.perform(get("/api/cache/search?pattern=TPP"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Verify interaction
        verify(redisCacheService).searchGenesByPattern("TPP");
    }

    @Test
    void searchGenes_ShouldReturnBadRequest_WhenPatternIsInvalid() throws Exception {
        // Arrange
        // Avoid * as it is a wildcard that matches all the possible keys in the cache.
        // Can cause performance issues by returning a huge result
        // Potentially expose all the cached data, so the security could be at risk
        when(redisCacheService.searchGenesByPattern("*")).thenReturn(List.of());

        // Act and assert
        mockMvc.perform(get("/api/cache/search?pattern=*"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"status\":\"error\",\"message\":\"Invalid pattern\"}"));

    }

    @Test
    void searchGenes_ShouldReturnBadRequest_WhenPatternIsNull() throws Exception {
        // Arrange
        when(redisCacheService.searchGenesByPattern("null")).thenReturn(List.of());

        // Act and assert
        mockMvc.perform(get("/api/cache/search?pattern="))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"status\":\"error\",\"message\":\"Invalid pattern\"}"));
    }

    @Test
    void clearGeneFromCache_ShouldSucceed_WhenGenesExistInCache() throws Exception {
        // Arrange
        when(redisCacheService.clearByPattern("gene:KRAS")).thenReturn(ClearResult.success(1L, "gene:KRAS"));

        // Act and assert
        mockMvc.perform(delete("/api/cache/clear?pattern=gene:KRAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedCount").value(1))
                .andExpect(jsonPath("$.pattern").value("gene:KRAS"));
    }

    @Test
    void clearGeneFromCache_ShouldReturnBadRequest_WhenPatternIsInvalid() throws Exception {
        mockMvc.perform(delete("/api/cache/clear?pattern=KRASS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.deletedCount").value(0))
                .andExpect(jsonPath("$.message").value("Failed to clear cache entries for pattern 'KRASS': Pattern must start with 'gene:' prefix for security"));
    }
}