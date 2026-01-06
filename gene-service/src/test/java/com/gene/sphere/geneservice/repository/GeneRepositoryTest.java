package com.gene.sphere.geneservice.repository;

import com.gene.sphere.geneservice.model.Gene;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GeneRepository.
 * Uses @DataJpaTest to test repository methods against an in-memory database.
 * Tests both inherited JpaRepository methods and custom query methods.
 */
@DataJpaTest
class GeneRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GeneRepository geneRepository;

    private Gene tp53Gene;
    private Gene krasGene;
    private Gene brcaGene;

    @BeforeEach
    void setUp() {
        // Create test genes
        tp53Gene = new Gene();
        tp53Gene.setName("TP53");
        tp53Gene.setDescription("Tumor suppressor gene");
        tp53Gene.setNormalFunction("DNA repair and cell cycle control");
        tp53Gene.setMutationEffect("Loss of tumor suppression function");
        tp53Gene.setPrevalence("50% of all cancers");
        tp53Gene.setTherapies("None specific; clinical trials ongoing");
        tp53Gene.setResearchLinks("http://example.com/tp53");

        krasGene = new Gene();
        krasGene.setName("KRAS");
        krasGene.setDescription("Oncogene involved in cell signaling");
        krasGene.setNormalFunction("Regulates cell division and growth");
        krasGene.setMutationEffect("Promotes uncontrolled cell growth");
        krasGene.setPrevalence("30% of lung cancers, 40% of colorectal cancers");
        krasGene.setTherapies("KRAS G12C inhibitors (sotorasib, adagrasib)");
        krasGene.setResearchLinks("http://example.com/kras");

        brcaGene = new Gene();
        brcaGene.setName("BRCA1");
        brcaGene.setDescription("Breast cancer gene");
        brcaGene.setNormalFunction("DNA repair");
        brcaGene.setMutationEffect("Increased cancer risk");
        brcaGene.setPrevalence("Hereditary breast cancer");
        brcaGene.setTherapies("PARP inhibitors");
        brcaGene.setResearchLinks("http://example.com/brca1");

        // Persist test data
        entityManager.persistAndFlush(tp53Gene);
        entityManager.persistAndFlush(krasGene);
        entityManager.persistAndFlush(brcaGene);
    }

    @AfterEach
    void tearDown(){
        geneRepository.deleteAll();
    }

    // ===== TESTS FOR findByName() =====

    @Test
    void findByName_shouldReturnGenes_whenExactNameMatches() {
        // ACT
        List<Gene> result = geneRepository.findByName("TP53");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TP53", result.get(0).getName());
        assertEquals("Tumor suppressor gene", result.get(0).getDescription());
    }

    @Test
    void findByName_shouldReturnEmptyList_whenNameNotFound() {
        // ACT
        List<Gene> result = geneRepository.findByName("NONEXISTENT");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_shouldBeCaseSensitive() {
        // ACT
        List<Gene> result = geneRepository.findByName("tp53"); // lowercase

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should not find genes with different case");
    }

    @Test
    void findByName_shouldHandleNullInput() {
        // ACT
        var result = geneRepository.findByName(null);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_shouldHandleEmptyString() {
        // ACT
        var result = geneRepository.findByName("");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===== TESTS FOR findAllByNameContainingIgnoreCase() =====

    @Test
    void findAllByNameContainingIgnoreCase_shouldReturnGene_whenPartialNameMatches() {
        // ACT
        var result = geneRepository.findAllByNameContainingIgnoreCase("P53");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(g -> "TP53".equals(g.getName())));
    }

    @Test
    void findAllByNameContainingIgnoreCase_shouldReturnGene_whenExactNameMatches() {
        // ACT
        var result = geneRepository.findAllByNameContainingIgnoreCase("KRAS");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(g -> "KRAS".equals(g.getName())));
    }

    @Test
    void findAllByNameContainingIgnoreCase_shouldBeCaseInsensitive() {
        // ACT
        var result = geneRepository.findAllByNameContainingIgnoreCase("kras"); // lowercase

        // ASSERT
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(g -> "KRAS".equals(g.getName())));
    }

    @Test
    void findAllByNameContainingIgnoreCase_shouldReturnEmpty_whenNoMatch() {
        // ACT
        var result = geneRepository.findAllByNameContainingIgnoreCase("NONEXISTENT");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findAllByNameContainingIgnoreCase_shouldReturnMultipleGenes_whenMultipleMatch() {
        // ARRANGE - Create another gene with "BRC" in the name to cause multiple matches
        var brca2Gene = new Gene();
        brca2Gene.setName("BRCA2");
        brca2Gene.setDescription("Another breast cancer gene");
        brca2Gene.setNormalFunction("DNA repair");
        brca2Gene.setMutationEffect("Increased cancer risk");
        brca2Gene.setPrevalence("Hereditary breast cancer");
        brca2Gene.setTherapies("PARP inhibitors");
        brca2Gene.setResearchLinks("http://example.com/brca2");
        entityManager.persistAndFlush(brca2Gene);

        // ACT
        var result = geneRepository.findAllByNameContainingIgnoreCase("BRCA");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.size() >= 2);
        assertTrue(result.stream().anyMatch(g -> "BRCA1".equals(g.getName())));
        assertTrue(result.stream().anyMatch(g -> "BRCA2".equals(g.getName())));
    }

    @Test
    void findAllByNameContainingIgnoreCase_shouldHandleEmptyString() {
        // ACT
        var result = geneRepository.findAllByNameContainingIgnoreCase("");

        // ASSERT
        assertNotNull(result);
        // Should match all genes from setup
        assertTrue(result.size() >= 3);
    }

    @Test
    void findAllByNameContainingIgnoreCase_shouldMatchSubstring() {
        // ACT
        var result = geneRepository.findAllByNameContainingIgnoreCase("RCA"); // matches BRCA1

        // ASSERT
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(g -> "BRCA1".equals(g.getName())));
    }

    // ===== TESTS FOR JpaRepository inherited methods =====

    @Test
    void findAll_shouldReturnAllGenes() {
        // ACT
        var result = geneRepository.findAll();

        // ASSERT
        assertNotNull(result);
        assertEquals(3, result.size(), "Should find all 3 genes from setup");
        
        // Verify all genes are present
        var geneNames = result.stream().map(Gene::getName).toList();
        assertTrue(geneNames.contains("TP53"));
        assertTrue(geneNames.contains("KRAS"));
        assertTrue(geneNames.contains("BRCA1"));
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoGenesExist() {
        // ARRANGE - Clear all data
        geneRepository.deleteAll();
        entityManager.flush();

        // ACT
        var result = geneRepository.findAll();

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_shouldReturnGene_whenIdExists() {
        // ACT
        var result = geneRepository.findById(tp53Gene.getId());

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("TP53", result.get().getName());
        assertEquals(tp53Gene.getId(), result.get().getId());
    }

    @Test
    void findById_shouldReturnEmpty_whenIdNotExists() {
        // ACT
        var result = geneRepository.findById(99999);

        // ASSERT
        assertFalse(result.isPresent());
    }

    @Test
    void save_shouldPersistNewGene() {
        // ARRANGE
        var newGene = new Gene();
        newGene.setName("EGFR");
        newGene.setDescription("Epidermal growth factor receptor");
        newGene.setNormalFunction("Cell growth signaling");
        newGene.setMutationEffect("Uncontrolled cell growth");
        newGene.setPrevalence("20% of lung cancers");
        newGene.setTherapies("Tyrosine kinase inhibitors");
        newGene.setResearchLinks("http://example.com/egfr");

        // ACT
        var saved = geneRepository.save(newGene);

        // ASSERT
        assertNotNull(saved.getId(), "Saved gene should have an ID");
        assertEquals("EGFR", saved.getName());
        
        // Verify it's actually persisted
        var found = geneRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("EGFR", found.get().getName());
    }

    @Test
    void save_shouldUpdateExistingGene() {
        // ARRANGE
        tp53Gene.setDescription("Updated tumor suppressor gene");
        tp53Gene.setPrevalence("60% of all cancers"); // Updated value

        // ACT
        var updated = geneRepository.save(tp53Gene);

        // ASSERT
        assertEquals(tp53Gene.getId(), updated.getId(), "ID should remain the same");
        assertEquals("Updated tumor suppressor gene", updated.getDescription());
        assertEquals("60% of all cancers", updated.getPrevalence());
        
        // Verify changes are persisted
        var found = geneRepository.findById(tp53Gene.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated tumor suppressor gene", found.get().getDescription());
        assertEquals("60% of all cancers", found.get().getPrevalence());
    }

    @Test
    void save_shouldHandleAllFieldsCorrectly() {
        // ARRANGE
        var complexGene = new Gene();
        complexGene.setName("COMPLEX_GENE");
        complexGene.setDescription("A gene with all fields populated");
        complexGene.setNormalFunction("Multiple cellular processes");
        complexGene.setMutationEffect("Various effects depending on mutation type");
        complexGene.setPrevalence("Varies by cancer type: 10% lung, 5% breast, 15% colon");
        complexGene.setTherapies("Targeted therapy A, B, and C; immunotherapy; radiation sensitizers");
        complexGene.setResearchLinks("http://example.com/complex;http://pubmed.com/123456;http://clinicaltrials.gov/789");

        // ACT
        var saved = geneRepository.save(complexGene);

        // ASSERT
        assertNotNull(saved.getId());
        assertEquals("COMPLEX_GENE", saved.getName());
        assertEquals("A gene with all fields populated", saved.getDescription());
        assertEquals("Multiple cellular processes", saved.getNormalFunction());
        assertEquals("Various effects depending on mutation type", saved.getMutationEffect());
        assertEquals("Varies by cancer type: 10% lung, 5% breast, 15% colon", saved.getPrevalence());
        assertEquals("Targeted therapy A, B, and C; immunotherapy; radiation sensitizers", saved.getTherapies());
        assertEquals("http://example.com/complex;http://pubmed.com/123456;http://clinicaltrials.gov/789", saved.getResearchLinks());
    }

    @Test
    void deleteById_shouldRemoveGene() {
        // ARRANGE
        var geneIdToDelete = tp53Gene.getId();
        assertTrue(geneRepository.findById(geneIdToDelete).isPresent(), "Gene should exist before deletion");

        // ACT
        geneRepository.deleteById(geneIdToDelete);
        entityManager.flush(); // Ensure deletion is processed

        // ASSERT
        var result = geneRepository.findById(geneIdToDelete);
        assertFalse(result.isPresent(), "Gene should not exist after deletion");
    }

    @Test
    void deleteById_shouldNotThrow_whenIdNotExists() {
        // ACT & ASSERT - Should not throw for non-existent ID (Spring Data JPA default behavior)
        assertDoesNotThrow(() -> geneRepository.deleteById(99999));
    }

    @Test
    void deleteAll_shouldRemoveAllGenes() {
        // ARRANGE
        assertEquals(3, geneRepository.findAll().size(), "Should start with 3 genes");

        // ACT
        geneRepository.deleteAll();
        entityManager.flush();

        // ASSERT
        var result = geneRepository.findAll();
        assertTrue(result.isEmpty(), "Should have no genes after deleteAll");
    }

    @Test
    void count_shouldReturnCorrectCount() {
        // ACT
        var count = geneRepository.count();

        // ASSERT
        assertEquals(3, count, "Should count 3 genes from setup");
    }

    @Test
    void count_shouldReturnZero_whenNoGenesExist() {
        // ARRANGE
        geneRepository.deleteAll();
        entityManager.flush();

        // ACT
        var count = geneRepository.count();

        // ASSERT
        assertEquals(0, count, "Should count 0 genes after deletion");
    }

    @Test
    void existsById_shouldReturnTrue_whenGeneExists() {
        // ACT
        var exists = geneRepository.existsById(tp53Gene.getId());

        // ASSERT
        assertTrue(exists, "Should return true for existing gene ID");
    }

    @Test
    void existsById_shouldReturnFalse_whenGeneNotExists() {
        // ACT
        var exists = geneRepository.existsById(99999);

        // ASSERT
        assertFalse(exists, "Should return false for non-existing gene ID");
    }

    // ===== ADDITIONAL EDGE CASE TESTS =====

    @Test
    void repository_shouldHandleLongStrings() {
        // ARRANGE
        var longDescription = "A".repeat(1000); // Very long description
        var geneWithLongFields = new Gene();
        geneWithLongFields.setName("LONG_GENE");
        geneWithLongFields.setDescription(longDescription);
        geneWithLongFields.setNormalFunction("Normal function");
        geneWithLongFields.setMutationEffect("Mutation effect");
        geneWithLongFields.setPrevalence("Prevalence data");
        geneWithLongFields.setTherapies("Therapy options");
        geneWithLongFields.setResearchLinks("http://example.com/long");

        // ACT
        var saved = geneRepository.save(geneWithLongFields);

        // ASSERT
        assertNotNull(saved.getId());
        assertEquals(longDescription, saved.getDescription());
    }

    @Test
    void repository_shouldHandleSpecialCharacters() {
        // ARRANGE
        var geneWithSpecialChars = new Gene();
        geneWithSpecialChars.setName("GENE-α/β");
        geneWithSpecialChars.setDescription("Gene with special chars: αβγδε & symbols !@#$%");
        geneWithSpecialChars.setNormalFunction("Function with unicode: ™®©");
        geneWithSpecialChars.setMutationEffect("Effect with quotes: \"double\" and 'single'");
        geneWithSpecialChars.setPrevalence("Prevalence: 50% ± 5%");
        geneWithSpecialChars.setTherapies("Therapies: drug-X & drug-Y");
        geneWithSpecialChars.setResearchLinks("http://example.com/gene?param=value&other=123");

        // ACT
        var saved = geneRepository.save(geneWithSpecialChars);

        // ASSERT
        assertNotNull(saved.getId());
        assertEquals("GENE-α/β", saved.getName());
        assertEquals("Gene with special chars: αβγδε & symbols !@#$%", saved.getDescription());
    }
}