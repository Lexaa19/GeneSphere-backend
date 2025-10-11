package com.gene.sphere.geneservice.controller;


import com.gene.sphere.geneservice.model.Gene;
import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.GeneServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GeneController.class)
class GeneControllerTest {

    @Autowired
    private MockMvc mock;

    @MockBean
    private GeneServiceInterface geneService;

    @MockBean
    private CacheManager cacheManager;

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
    @WithMockUser  // This bypasses Spring Security authentication
    void getGene_shouldReturnGene_whenExists() throws Exception {
        // ARRANGE: Mock service to return a gene
        when(geneService.getGeneByName("TP53")).thenReturn(Optional.of(tp53GeneRecord));
        
        // ACT & ASSERT: Make request and verify response
        mock.perform(get("/genes/TP53"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("TP53"))
                .andExpect(jsonPath("$.description").value("Tumor suppressor gene"))
                .andExpect(jsonPath("$.normalFunction").value("DNA repair and cell cycle control"))
                .andExpect(jsonPath("$.mutationEffect").value("Loss of tumor suppression function"))
                .andExpect(jsonPath("$.prevalence").value("50% of all cancers"))
                .andExpect(jsonPath("$.therapies").value("None specific; clinical trials ongoing"))
                .andExpect(jsonPath("$.researchLinks").value("http://example.com/tp53"));
        
        // VERIFY: Service was called
        verify(geneService).getGeneByName("TP53");
    }
}