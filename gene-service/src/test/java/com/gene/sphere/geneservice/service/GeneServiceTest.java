package com.gene.sphere.geneservice.service;

import com.gene.sphere.geneservice.factory.GeneFactory;
import com.gene.sphere.geneservice.model.Gene;
import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.repository.GeneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class GeneServiceTest {
    @Mock
    private GeneRepository geneRepository;
    @Mock
    private GeneFactory geneFactory;
    @InjectMocks
    private GeneService geneService;

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
    void getGeneByName_shouldReturnGene_whenNameExists() {
        // ARRANGE - Set up test conditions

        // 1. Configure mock repository to return testGene when findByNameIgnoreCase is called
        when(geneRepository.findByNameIgnoreCase("TP53")).thenReturn(Optional.of(tp53Gene));

        // 2. Configure mock factory to return testGeneRecord when toDto is called with testGene
        when(geneFactory.toDto(tp53Gene)).thenReturn(tp53GeneRecord);

        // ACT - Call the method you're testing

        // 3. Call the service method
        Optional<GeneRecord> result = geneService.getGeneByName("TP53");

        // ASSERT - Verify the results

        // 4. Check that the result contains a value
        assertTrue(result.isPresent(), "Result should contain a gene");

        // 5. Check that the returned GeneRecord has the expected values
        GeneRecord returnedRecord = result.get();
        assertEquals("TP53", returnedRecord.name(), "Gene name should match");
        assertEquals("Tumor suppressor gene", returnedRecord.description(), "Description should match");
        assertEquals("DNA repair and cell cycle control", returnedRecord.normalFunction(), "Function should match");

        // 6. Verify the repository method was called with the right argument
        verify(geneRepository).findByNameIgnoreCase("TP53");

        // 7. Verify the factory method was called with the right argument
        verify(geneFactory).toDto(tp53Gene);
    }

    @Test
    void getGeneByName_shouldReturnEmpty_whenNameNotFound() {
        // ARRANGE - Set up test conditions

        // 1. Configure mock repository to return empty when searching for non-existent gene
        when(geneRepository.findByNameIgnoreCase("ERR")).thenReturn(Optional.empty());

        // ACT - Call the method you're testing

        // 2. Call the service method with a non-existent gene name
        Optional<GeneRecord> result = geneService.getGeneByName("ERR");

        // ASSERT - Verify the results

        // 3. Check that the result is empty (no gene found)
        assertFalse(result.isPresent(), "Result should be empty");

        // 4. Verify the repository method was called with the search term
        verify(geneRepository).findByNameIgnoreCase("ERR");

        // 5. Verify the factory was NOT called (since there's no gene to convert)
        verifyNoInteractions(geneFactory);
    }

    @Test
    void deleteGene_shouldCallRepositoryDeleteById() {
        // ARRANGE - Set up test conditions
        // (No mocking needed for this simple case - deleteGene is a void method)

        // ACT - Call the method you're testing

        // 1. Call the service method to delete the gene
        geneService.deleteGene(tp53Gene.getId());

        // ASSERT - Verify the results

        // 2. Verify the repository's deleteById method was called with the correct ID
        verify(geneRepository).deleteById(tp53Gene.getId());

        // 3. Verify the factory was NOT called (delete operation doesn't need conversion)
        verifyNoInteractions(geneFactory);
    }

    @Test
    void getAllGenes_shouldReturnGenes_WhenGenesAreFound() {
        // ARRANGE - Set up test conditions
        List<Gene> genes = List.of(tp53Gene, krasGene);
        List<GeneRecord> expectedGeneRecords = List.of(tp53GeneRecord, krasGeneRecord);

        // 1. Configure mock repository to return a list of genes when findAll is called
        when(geneRepository.findAll()).thenReturn(genes);

        // 2. Configure mock factory to return tp53GeneRecord when toDto is called with testGene
        when(geneFactory.toDto(tp53Gene)).thenReturn(tp53GeneRecord);

        // 2. Configure mock factory to return krasGeneRecord when toDto is called with testGene
        when(geneFactory.toDto(krasGene)).thenReturn(krasGeneRecord);

        // ACT - Call the method you're testing

        // 3. Call the service method
        List<GeneRecord> result = geneService.getAllGenes();

        // ASSERT - Verify the results

        // 4. Check that the result is not null and has the expected size
        assertNotNull(result);

        // 5. Chek that the size is correct
        assertEquals(2, result.size(), "Result should contain 2 genes");

        // 6. Verify both genes are in the result
        assertTrue(result.contains(tp53GeneRecord));
        assertTrue(result.contains(krasGeneRecord));

        // 7. Verify the findAll() was called once
        verify(geneRepository).findAll();

        // 8. Verify the dto was called for each method (needed because the service depends on the factory to convert entities to DTOs)
        verify(geneFactory).toDto(tp53Gene);
        verify(geneFactory).toDto(krasGene);
    }

    @Test
    void createGene_shouldCreateAndReturnGene_whenValidInput() {
        // ARRANGE (Set up your test data)

        // 1. This is what the user sends to your API
        GeneRecord inputData = new GeneRecord(
                "BRCA1",                           // gene name
                "Important cancer gene",           // description  
                "Fixes DNA damage",                // what it normally does
                "Can cause breast cancer",         // what happens when broken
                "Found in 10% of cases",           // how common
                "Special drugs available",         // treatments
                "www.example.com"                  // research links
        );

        // 2. This is what your factory should create (no ID yet)
        Gene newGene = new Gene();
        newGene.setName("BRCA1");
        newGene.setDescription("Important cancer gene");
        newGene.setNormalFunction("Fixes DNA damage");
        newGene.setMutationEffect("Can cause breast cancer");
        newGene.setPrevalence("Found in 10% of cases");
        newGene.setTherapies("Special drugs available");
        newGene.setResearchLinks("www.example.com");

        // 3. This is what database returns after saving (now has ID!)
        Gene savedGene = new Gene();
        savedGene.setId(999);  // Database gave it an ID
        savedGene.setName("BRCA1");
        savedGene.setDescription("Important cancer gene");
        savedGene.setNormalFunction("Fixes DNA damage");
        savedGene.setMutationEffect("Can cause breast cancer");
        savedGene.setPrevalence("Found in 10% of cases");
        savedGene.setTherapies("Special drugs available");
        savedGene.setResearchLinks("www.example.com");

        // 4. This is what we expect to get back
        GeneRecord expectedResult = new GeneRecord(
                "BRCA1",
                "Important cancer gene",
                "Fixes DNA damage",
                "Can cause breast cancer",
                "Found in 10% of cases",
                "Special drugs available",
                "www.example.com"
        );

        // MOCK (Tell your fake objects what to do)
        when(geneFactory.fromDto(inputData)).thenReturn(newGene);
        when(geneRepository.save(newGene)).thenReturn(savedGene);
        when(geneFactory.toDto(savedGene)).thenReturn(expectedResult);

        //  ACT (Actually test the method)
        GeneRecord actualResult = geneService.createGene(inputData);

        //  ASSERT (Check if everything worked)
        assertEquals(expectedResult.name(), actualResult.name());
        assertEquals(expectedResult.description(), actualResult.description());

        // VERIFY (Make sure the right methods were called)
        verify(geneFactory).fromDto(inputData);
        verify(geneRepository).save(newGene);
        verify(geneFactory).toDto(savedGene);
    }


    @Test
    void updateGene_shouldUpdateAndReturnGene_whenGeneExists() {
        Integer geneId = 1;

        // reuse the TP53 gene
        // update the link and the prevalence for this gene
        GeneRecord updatedGene = new GeneRecord(
                "TP53",
                "Tumor suppressor gene",
                "DNA repair and cell cycle control",
                "Loss of tumor suppression function",
                "60% of all cancers",
                "None specific; clinical trials ongoing",
                "http://new-research-link.com"
        );

        // expected result
        GeneRecord expectedResult = new GeneRecord(
                "TP53",
                "Tumor suppressor gene",
                "DNA repair and cell cycle control",
                "Loss of tumor suppression function",
                "60% of all cancers",                      // updated
                "None specific; clinical trials ongoing",
                "http://new-research-link.com"             // updated
        );

        // mock - use the existing TP54 gene
        when(geneRepository.findById(geneId)).thenReturn(Optional.of(tp53Gene));
        when(geneRepository.save(any(Gene.class))).thenReturn(tp53Gene);
        when(geneFactory.toDto(tp53Gene)).thenReturn(expectedResult);

        // ACT
        GeneRecord result = geneService.updateGene(geneId,updatedGene);

        // ASSERT
        assertEquals(expectedResult.prevalence(),result.prevalence());
        assertEquals(expectedResult.researchLinks(), result.researchLinks());

        // VERIFY
        verify(geneRepository).findById(geneId);
        verify(geneRepository).save(any(Gene.class));
        verify(geneFactory).toDto(tp53Gene);
    }

    @Test
    void updateGene_shouldThrowException_whenGeneNotFound() {
        // ARRANGE - Set up test conditions
        Integer nonExistentId = 999;
        
        // Create update data (reuse existing test data)
        GeneRecord updateData = new GeneRecord(
                "TP53",
                "Updated description",
                "Updated function",
                "Updated mutation effect",
                "Updated prevalence",
                "Updated therapies",
                "http://updated-link.com"
        );
        
        // Mock repository to return empty when gene doesn't exist
        when(geneRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT - Test that exception is thrown
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> geneService.updateGene(nonExistentId, updateData),
                "Should throw NoSuchElementException when gene not found"
        );

        // VERIFY - Check that the right methods were called (and not called)
        verify(geneRepository).findById(nonExistentId);
        verify(geneRepository, never()).save(any(Gene.class));
        verifyNoInteractions(geneFactory);
    }
}