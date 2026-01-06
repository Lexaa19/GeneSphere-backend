package com.gene.sphere.mutationservice.repository;

import com.gene.sphere.mutationservice.model.Mutation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MutationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MutationRepository mutationRepository;

    private Mutation egfrMutation;
    private Mutation krasMutation;
    private Mutation tp53Mutation;

    @BeforeEach
    void setUp() {
        egfrMutation = new Mutation();
        egfrMutation.setGeneName("EGFR");
        egfrMutation.setChromosome("7");
        egfrMutation.setPosition(55191822L);
        egfrMutation.setReferenceAllele("G");
        egfrMutation.setAlternateAllele("T");
        egfrMutation.setMutationType("SNV");
        egfrMutation.setPatientId("TCGA-05-4244-01");
        egfrMutation.setSampleId("TCGA-05-4244-01");
        egfrMutation.setProteinChange("p.L858R");
        egfrMutation.setCancerType("Lung Adenocarcinoma (TCGA)");
        egfrMutation.setClinicalSignificance("Pathogenic");
        egfrMutation.setAlleleFrequency(new java.math.BigDecimal("0.65"));

        krasMutation = new Mutation();
        krasMutation.setGeneName("KRAS");
        krasMutation.setChromosome("12");
        krasMutation.setPosition(25398284L);
        krasMutation.setReferenceAllele("G");
        krasMutation.setAlternateAllele("T");
        krasMutation.setMutationType("SNV");
        krasMutation.setPatientId("TCGA-05-4244-02");
        krasMutation.setSampleId("TCGA-05-4244-02");
        krasMutation.setProteinChange("p.G12C");
        krasMutation.setCancerType("Lung Adenocarcinoma (TCGA)");
        krasMutation.setClinicalSignificance("Pathogenic");
        krasMutation.setAlleleFrequency(new java.math.BigDecimal("0.72"));

        tp53Mutation = new Mutation();
        tp53Mutation.setGeneName("TP53");
        tp53Mutation.setChromosome("17");
        tp53Mutation.setPosition(7579472L);
        tp53Mutation.setReferenceAllele("C");
        tp53Mutation.setAlternateAllele("T");
        tp53Mutation.setMutationType("SNV");
        tp53Mutation.setPatientId("TCGA-05-4244-03");
        tp53Mutation.setSampleId("TCGA-05-4244-03");
        tp53Mutation.setProteinChange("p.R175H");
        tp53Mutation.setCancerType("Lung Adenocarcinoma (TCGA)");
        tp53Mutation.setClinicalSignificance("Pathogenic");
        tp53Mutation.setAlleleFrequency(new java.math.BigDecimal("0.81"));

        entityManager.persistAndFlush(egfrMutation);
        entityManager.persistAndFlush(krasMutation);
        entityManager.persistAndFlush(tp53Mutation);
    }

    @AfterEach
    void tearDown() {
        mutationRepository.deleteAll();
    }

    @Test
    void findByGeneName_shouldReturnMutation_whenExactNameMatches() {
        // ACT
        List<Mutation> result = mutationRepository.findByGeneName("TP53");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TP53", result.get(0).getGeneName());
        assertEquals("17", result.get(0).getChromosome());
    }

    @Test
    void findByGeneName_nonExisting_shouldReturnEmptyList() {
        // ACT
        List<Mutation> nonExistingGene = mutationRepository.findByGeneName("NONEXISTINGGENE");

        // ASSERT
        assertNotNull(nonExistingGene);
        assertEquals(0, nonExistingGene.size());
    }

    /**
     * It is absolutely possible—and common—for the same gene to have multiple distinct mutations in real life.
     * A single gene (like TP53 or EGFR) can have many different mutations (at different positions, with different nucleotide or amino acid changes)
     * across different patients, or even within the same patient or sample. This is a fundamental concept in genetics and cancer genomics.
     */
    @Test
    void findByGeneName_multipleMatches_returnsAll() {
        // ARRANGE
        Mutation mutation1 = new Mutation();
        mutation1.setGeneName("TP53");
        mutation1.setChromosome("17");
        mutation1.setPosition(7579472L);
        mutation1.setReferenceAllele("C");
        mutation1.setAlternateAllele("T");
        mutation1.setMutationType("SNV");
        mutation1.setPatientId("PATIENT-1");
        mutation1.setSampleId("SAMPLE-1");
        mutation1.setProteinChange("p.R175H");
        mutation1.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation1.setClinicalSignificance("Pathogenic");
        mutation1.setAlleleFrequency(new BigDecimal("0.81"));
        entityManager.persistAndFlush(mutation1);

        // ACT
        List<Mutation> result = mutationRepository.findByGeneName("TP53");

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        for (Mutation m : result) {
            assertEquals("TP53", m.getGeneName());
        }
    }

    @Test
    void findByGeneNameIgnoreCase_shouldReturnMutation() {
        // ARRANGE
        Mutation brafMutation = new Mutation();
        brafMutation.setGeneName("braf");
        brafMutation.setChromosome("7");
        brafMutation.setPosition(140453136L);
        brafMutation.setReferenceAllele("A");
        brafMutation.setAlternateAllele("T");
        brafMutation.setMutationType("SNV");
        brafMutation.setPatientId("TCGA-05-4244-04");
        brafMutation.setSampleId("TCGA-05-4244-04");
        brafMutation.setProteinChange("p.V600E");
        brafMutation.setCancerType("Lung Adenocarcinoma (TCGA)");
        brafMutation.setClinicalSignificance("Pathogenic");
        brafMutation.setAlleleFrequency(new BigDecimal("0.60"));

        entityManager.persistAndFlush(brafMutation);

        // ACT
        List<Mutation> result = mutationRepository.findByGeneNameCaseInsensitive("BRAF");
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void findByGeneNameContaining_shouldReturnMatches() {
        // ACT
        List<Mutation> result = mutationRepository.findByGeneNameContainingIgnoreCase("53");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    void addDuplicatedChromosome() {
        Mutation mutation1 = new Mutation();
        mutation1.setGeneName("MET");
        mutation1.setChromosome("8");
        mutation1.setPosition(123456L);
        mutation1.setReferenceAllele("A");
        mutation1.setAlternateAllele("T");
        mutation1.setMutationType("SNV");
        mutation1.setPatientId("PATIENT-5");
        mutation1.setSampleId("SAMPLE-5");
        mutation1.setProteinChange("p.M123T");
        mutation1.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation1.setClinicalSignificance("Pathogenic");
        mutation1.setAlleleFrequency(new BigDecimal("0.50"));

        Mutation mutation2 = new Mutation();
        mutation2.setGeneName("CDC6");
        mutation2.setChromosome("8");
        mutation2.setPosition(654321L);
        mutation2.setReferenceAllele("G");
        mutation2.setAlternateAllele("C");
        mutation2.setMutationType("SNV");
        mutation2.setPatientId("PATIENT-6");
        mutation2.setSampleId("SAMPLE-6");
        mutation2.setProteinChange("p.C456G");
        mutation2.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation2.setClinicalSignificance("Pathogenic");
        mutation2.setAlleleFrequency(new BigDecimal("0.45"));

        entityManager.persistAndFlush(mutation1);
        entityManager.persistAndFlush(mutation2);
    }

    @Test
    void findByChromosome_shouldReturnMutationsOnChromosome() {
        addDuplicatedChromosome();

        // ACT
        List<Mutation> result = mutationRepository.findByChromosome("8");

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void findByPatientId_shouldReturnSingle() {
        // ACT
        List<Mutation> result = mutationRepository.findByPatientId("TCGA-05-4244-02");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void findByCancerType_shouldReturnAllForCancerType() {
        // ACT
        List<Mutation> result = mutationRepository.findByCancerTypeContainingIgnoreCase("Lung Adenocarcinoma");

        // ASSERT
        assertNotNull(result);
        assertEquals(3, result.size());
    }


    void addDuplicatedProteinChange() {
        Mutation mutation1 = new Mutation();
        mutation1.setGeneName("MET");
        mutation1.setChromosome("8");
        mutation1.setPosition(123456L);
        mutation1.setReferenceAllele("A");
        mutation1.setAlternateAllele("T");
        mutation1.setMutationType("SNV");
        mutation1.setPatientId("PATIENT-5");
        mutation1.setSampleId("SAMPLE-5");
        mutation1.setProteinChange("p.M123T");
        mutation1.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation1.setClinicalSignificance("Pathogenic");
        mutation1.setAlleleFrequency(new BigDecimal("0.50"));

        Mutation mutation2 = new Mutation();
        mutation2.setGeneName("CDC6");
        mutation2.setChromosome("8");
        mutation2.setPosition(654321L);
        mutation2.setReferenceAllele("G");
        mutation2.setAlternateAllele("C");
        mutation2.setMutationType("SNV");
        mutation2.setPatientId("PATIENT-6");
        mutation2.setSampleId("SAMPLE-6");
        mutation2.setProteinChange("p.M123T");
        mutation2.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation2.setClinicalSignificance("Pathogenic");
        mutation2.setAlleleFrequency(new BigDecimal("0.45"));

        entityManager.persistAndFlush(mutation1);
        entityManager.persistAndFlush(mutation2);
    }

    @Test
    void findByProteinChange_shouldReturnProtein() {
        // ARRANGE
        addDuplicatedProteinChange();
        // ACT
        List<Mutation> result = mutationRepository.findByProteinChange("p.M123T");

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void findByClinicalSignificance_shouldReturnAllWithSignificance() {
        // ARRANGE
        // Data setup is performed in test fixtures
        // ACT
        List<Mutation> result = mutationRepository.findByClinicalSignificance("Pathogenic");

        // ASSERT
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void findByGeneNameAndClinicalSignificance_shouldReturnMatch_caseInsensitive() {
        // Arrange
        Mutation mutation = new Mutation();
        mutation.setGeneName("BRAF");
        mutation.setChromosome("7");
        mutation.setPosition(140453136L);
        mutation.setReferenceAllele("A");
        mutation.setAlternateAllele("T");
        mutation.setMutationType("SNV");
        mutation.setPatientId("PATIENT-12");
        mutation.setSampleId("SAMPLE-12");
        mutation.setProteinChange("p.V600E");
        mutation.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation.setClinicalSignificance("Pathogenic");
        mutation.setAlleleFrequency(new BigDecimal("0.60"));

        entityManager.persistAndFlush(mutation);

        // ACT
        List<Mutation> result = mutationRepository.findByGeneNameAndClinicalSignificance("braf", "pathogenic");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BRAF", result.get(0).getGeneName());
        assertEquals("Pathogenic", result.get(0).getClinicalSignificance());
    }

    void addDifferentAlleleFrequency() {
        Mutation mutation1 = new Mutation();
        mutation1.setGeneName("MET");
        mutation1.setChromosome("8");
        mutation1.setPosition(123456L);
        mutation1.setReferenceAllele("A");
        mutation1.setAlternateAllele("T");
        mutation1.setMutationType("SNV");
        mutation1.setPatientId("PATIENT-5");
        mutation1.setSampleId("SAMPLE-5");
        mutation1.setProteinChange("p.M123T");
        mutation1.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation1.setClinicalSignificance("Pathogenic");
        mutation1.setAlleleFrequency(new BigDecimal("0.55"));

        Mutation mutation2 = new Mutation();
        mutation2.setGeneName("CDC6");
        mutation2.setChromosome("8");
        mutation2.setPosition(654321L);
        mutation2.setReferenceAllele("G");
        mutation2.setAlternateAllele("C");
        mutation2.setMutationType("SNV");
        mutation2.setPatientId("PATIENT-6");
        mutation2.setSampleId("SAMPLE-6");
        mutation2.setProteinChange("p.M123T");
        mutation2.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation2.setClinicalSignificance("Pathogenic");
        mutation2.setAlleleFrequency(new BigDecimal("0.45"));


        Mutation mutation3 = new Mutation();
        mutation3.setGeneName("CDC6");
        mutation3.setChromosome("7");
        mutation3.setPosition(140453136L);
        mutation3.setReferenceAllele("A");
        mutation3.setAlternateAllele("T");
        mutation3.setMutationType("SNV");
        mutation3.setPatientId("PATIENT-12");
        mutation3.setSampleId("SAMPLE-12");
        mutation3.setProteinChange("p.V600E");
        mutation3.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation3.setClinicalSignificance("Pathogenic");
        mutation3.setAlleleFrequency(new BigDecimal("0.60"));

        entityManager.persistAndFlush(mutation1);
        entityManager.persistAndFlush(mutation2);
        entityManager.persistAndFlush(mutation3);
    }

    @Test
    void findHighFrequencyMutations_shouldReturnAboveThreshold() {
        addDifferentAlleleFrequency();

        // ACT
        List<Mutation> result = mutationRepository.findHighFrequencyMutations("CDC6", BigDecimal.valueOf(0.50));

        // ASSERT
        assertNotNull(result);

        assertEquals(1, result.size());
    }

    @Test
    void findByGeneName_shouldReturnEmptyList_whenNullOrEmpty() {
        List<Mutation> nullResult = mutationRepository.findByGeneName(null);
        List<Mutation> emptyResult = mutationRepository.findByGeneName("");
        assertTrue(nullResult == null || nullResult.isEmpty());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void findByGeneNameContainingIgnoreCase_shouldReturnEmpty_whenNoMatchOrNull() {
        List<Mutation> noMatch = mutationRepository.findByGeneNameContainingIgnoreCase("ZZZ");
        assertTrue(noMatch.isEmpty());

        // For null, expect empty result
        List<Mutation> nullResult = mutationRepository.findByGeneNameContainingIgnoreCase(null);
        assertTrue(nullResult == null || nullResult.isEmpty());

        // For empty string, expect all results (3 seeded mutations)
        List<Mutation> emptyResult = mutationRepository.findByGeneNameContainingIgnoreCase("");
        assertEquals(3, emptyResult.size());
    }

    @Test
    void findByChromosome_shouldReturnEmpty_whenNoMatchOrNull() {
        List<Mutation> noMatch = mutationRepository.findByChromosome("99");
        List<Mutation> nullResult = mutationRepository.findByChromosome(null);
        List<Mutation> emptyResult = mutationRepository.findByChromosome("");
        assertTrue(noMatch.isEmpty());
        assertTrue(nullResult == null || nullResult.isEmpty());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void findByClinicalSignificance_shouldReturnEmpty_whenNoMatchOrNull() {
        List<Mutation> noMatch = mutationRepository.findByClinicalSignificance("Benign");
        List<Mutation> nullResult = mutationRepository.findByClinicalSignificance(null);
        List<Mutation> emptyResult = mutationRepository.findByClinicalSignificance("");
        assertTrue(noMatch.isEmpty());
        assertTrue(nullResult == null || nullResult.isEmpty());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void findHighFrequencyMutations_shouldReturnEmpty_whenThresholdTooHighOrGeneMissing() {
        List<Mutation> tooHigh = mutationRepository.findHighFrequencyMutations("EGFR", BigDecimal.valueOf(1.0));
        List<Mutation> negative = mutationRepository.findHighFrequencyMutations("EGFR", BigDecimal.valueOf(-1.0));
        List<Mutation> missingGene = mutationRepository.findHighFrequencyMutations("NONEXISTENT", BigDecimal.valueOf(0.5));
        assertNotNull(tooHigh);
        assertEquals(0, tooHigh.size());
        assertNotNull(negative);
        assertEquals(1, negative.size()); // You have 1 EGFR mutation seeded
        assertNotNull(missingGene);
        assertEquals(0, missingGene.size());
    }

    @Test
    void findByPatientId_shouldReturnEmpty_whenNoMatchOrNull() {
        List<Mutation> noMatch = mutationRepository.findByPatientId("PATIENT-XYZ");
        List<Mutation> nullResult = mutationRepository.findByPatientId(null);
        List<Mutation> emptyResult = mutationRepository.findByPatientId("");
        assertTrue(noMatch.isEmpty());
        assertTrue(nullResult == null || nullResult.isEmpty());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void findByGeneNameAndClinicalSignificance_shouldReturnEmpty_whenNoMatchOrNull() {
        List<Mutation> noMatch = mutationRepository.findByGeneNameAndClinicalSignificance("EGFR", "Benign");
        List<Mutation> nullResult = mutationRepository.findByGeneNameAndClinicalSignificance(null, null);
        List<Mutation> emptyResult = mutationRepository.findByGeneNameAndClinicalSignificance("", "");
        assertTrue(noMatch.isEmpty());
        assertTrue(nullResult == null || nullResult.isEmpty());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void findByProteinChange_shouldReturnEmpty_whenNoMatchOrNull() {
        List<Mutation> noMatch = mutationRepository.findByProteinChange("p.XYZ123");
        List<Mutation> nullResult = mutationRepository.findByProteinChange(null);
        List<Mutation> emptyResult = mutationRepository.findByProteinChange("");
        assertTrue(noMatch.isEmpty());
        assertTrue(nullResult == null || nullResult.isEmpty());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void existsByGeneNameAndProteinChange() {
        assertTrue(mutationRepository.existsByGeneNameAndProteinChange("EGFR", "p.L858R"));
        assertTrue(mutationRepository.existsByGeneNameAndProteinChange("egfr", "p.l858r"));
        assertFalse(mutationRepository.existsByGeneNameAndProteinChange("EGFR", "treefr"));
    }

    @Test
    void countMutationsPerGene() {
        // ARRANGE: Add extra mutations to have multiple per gene
        Mutation extraEgfr = new Mutation();
        extraEgfr.setGeneName("EGFR");
        extraEgfr.setChromosome("7");
        extraEgfr.setPosition(55191900L);
        extraEgfr.setReferenceAllele("A");
        extraEgfr.setAlternateAllele("T");
        extraEgfr.setMutationType("SNV");
        extraEgfr.setPatientId("PATIENT-20");
        extraEgfr.setSampleId("SAMPLE-20");
        extraEgfr.setProteinChange("p.T790M");
        extraEgfr.setCancerType("Lung Adenocarcinoma (TCGA)");
        extraEgfr.setClinicalSignificance("Pathogenic");
        extraEgfr.setAlleleFrequency(new BigDecimal("0.70"));
        entityManager.persistAndFlush(extraEgfr);

        // ACT
        List<Object[]> result = mutationRepository.countMutationsPerGene();

        // ASSERT
        assertNotNull(result);
        assertTrue(result.size() >= 3); // At least EGFR(2), KRAS(1), TP53(1)
        
        // Find EGFR count
        Object[] egfrCount = result.stream()
            .filter(r -> "EGFR".equals(r[0]))
            .findFirst()
            .orElse(null);
        assertNotNull(egfrCount);
        assertEquals(2L, egfrCount[1]);
        
        // Verify descending order: first should have highest or equal count
        long firstCount = (Long) result.get(0)[1];
        long secondCount = (Long) result.get(1)[1];
        assertTrue(firstCount >= secondCount);
    }

    @Test
    void findByGeneNameIn_shouldReturnMatchingGenes() {
        // ARRANGE: Seed a BRAF mutation
        Mutation brafMutation = new Mutation();
        brafMutation.setGeneName("BRAF");
        brafMutation.setChromosome("7");
        brafMutation.setPosition(140453136L);
        brafMutation.setReferenceAllele("A");
        brafMutation.setAlternateAllele("T");
        brafMutation.setMutationType("SNV");
        brafMutation.setPatientId("PATIENT-30");
        brafMutation.setSampleId("SAMPLE-30");
        brafMutation.setProteinChange("p.V600E");
        brafMutation.setCancerType("Lung Adenocarcinoma (TCGA)");
        brafMutation.setClinicalSignificance("Pathogenic");
        brafMutation.setAlleleFrequency(new BigDecimal("0.55"));
        entityManager.persistAndFlush(brafMutation);

        // ACT
        List<Mutation> result = mutationRepository.findByGeneNameIn(List.of("EGFR", "KRAS"));

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size()); // EGFR and KRAS from setUp
        assertTrue(result.stream().allMatch(m -> 
            m.getGeneName().equals("EGFR") || m.getGeneName().equals("KRAS")));
    }

    @Test
    void findByChromosomeAndPositionBetween_shouldReturnInRange() {
        // ARRANGE: Add mutations on chromosome 7 with different positions
        Mutation pos100 = new Mutation();
        pos100.setGeneName("GENE1");
        pos100.setChromosome("7");
        pos100.setPosition(100L);
        pos100.setReferenceAllele("A");
        pos100.setAlternateAllele("T");
        pos100.setMutationType("SNV");
        pos100.setPatientId("PATIENT-40");
        pos100.setSampleId("SAMPLE-40");
        pos100.setProteinChange("p.X1Y");
        pos100.setCancerType("Lung Adenocarcinoma (TCGA)");
        pos100.setClinicalSignificance("Pathogenic");
        pos100.setAlleleFrequency(new BigDecimal("0.50"));
        entityManager.persistAndFlush(pos100);

        Mutation pos200 = new Mutation();
        pos200.setGeneName("GENE2");
        pos200.setChromosome("7");
        pos200.setPosition(200L);
        pos200.setReferenceAllele("G");
        pos200.setAlternateAllele("C");
        pos200.setMutationType("SNV");
        pos200.setPatientId("PATIENT-41");
        pos200.setSampleId("SAMPLE-41");
        pos200.setProteinChange("p.X2Y");
        pos200.setCancerType("Lung Adenocarcinoma (TCGA)");
        pos200.setClinicalSignificance("Pathogenic");
        pos200.setAlleleFrequency(new BigDecimal("0.50"));
        entityManager.persistAndFlush(pos200);

        Mutation pos300 = new Mutation();
        pos300.setGeneName("GENE3");
        pos300.setChromosome("7");
        pos300.setPosition(300L);
        pos300.setReferenceAllele("C");
        pos300.setAlternateAllele("T");
        pos300.setMutationType("SNV");
        pos300.setPatientId("PATIENT-42");
        pos300.setSampleId("SAMPLE-42");
        pos300.setProteinChange("p.X3Y");
        pos300.setCancerType("Lung Adenocarcinoma (TCGA)");
        pos300.setClinicalSignificance("Pathogenic");
        pos300.setAlleleFrequency(new BigDecimal("0.50"));
        entityManager.persistAndFlush(pos300);

        // ACT: Query for positions between 150 and 350
        List<Mutation> result = mutationRepository.findByChromosomeAndPositionBetween("7", 150L, 350L);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size()); // Should return pos200 and pos300
        assertTrue(result.stream().allMatch(m -> 
            m.getPosition() >= 150L && m.getPosition() <= 350L));
    }

    @Test
    void findBySampleId_shouldReturnMutationsForSample() {
        // ACT
        List<Mutation> result = mutationRepository.findBySampleId("TCGA-05-4244-01");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EGFR", result.get(0).getGeneName());
    }

    @Test
    void findByGeneNameAndSampleId_shouldReturnMatch() {
        // ACT
        List<Mutation> result = mutationRepository.findByGeneNameAndSampleId("KRAS", "TCGA-05-4244-02");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("KRAS", result.get(0).getGeneName());
        assertEquals("TCGA-05-4244-02", result.get(0).getSampleId());
    }

    @Test
    void findByGeneNameAndSampleId_shouldReturnEmpty_whenNoMatch() {
        // ACT
        List<Mutation> result = mutationRepository.findByGeneNameAndSampleId("EGFR", "WRONG-SAMPLE");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findActionableMutations_shouldReturnOnlyActionable() {
        // ARRANGE: Add mutations with different significances
        Mutation likelyPathogenic = new Mutation();
        likelyPathogenic.setGeneName("ALK");
        likelyPathogenic.setChromosome("2");
        likelyPathogenic.setPosition(29415640L);
        likelyPathogenic.setReferenceAllele("A");
        likelyPathogenic.setAlternateAllele("T");
        likelyPathogenic.setMutationType("SNV");
        likelyPathogenic.setPatientId("PATIENT-50");
        likelyPathogenic.setSampleId("SAMPLE-50");
        likelyPathogenic.setProteinChange("p.F1174L");
        likelyPathogenic.setCancerType("Lung Adenocarcinoma (TCGA)");
        likelyPathogenic.setClinicalSignificance("Likely Pathogenic");
        likelyPathogenic.setAlleleFrequency(new BigDecimal("0.60"));
        entityManager.persistAndFlush(likelyPathogenic);

        Mutation benign = new Mutation();
        benign.setGeneName("EGFR");
        benign.setChromosome("7");
        benign.setPosition(55191900L);
        benign.setReferenceAllele("G");
        benign.setAlternateAllele("A");
        benign.setMutationType("SNV");
        benign.setPatientId("PATIENT-51");
        benign.setSampleId("SAMPLE-51");
        benign.setProteinChange("p.P848L");
        benign.setCancerType("Lung Adenocarcinoma (TCGA)");
        benign.setClinicalSignificance("Benign");
        benign.setAlleleFrequency(new BigDecimal("0.10"));
        entityManager.persistAndFlush(benign);

        // ACT
        List<Mutation> result = mutationRepository.findActionableMutations(
            List.of("EGFR", "KRAS", "ALK"),
            List.of("Pathogenic", "Likely Pathogenic")
        );

        // ASSERT
        assertNotNull(result);
        assertEquals(3, result.size()); // EGFR(Pathogenic), KRAS(Pathogenic), ALK(Likely Pathogenic)
        assertTrue(result.stream().allMatch(m -> 
            m.getClinicalSignificance().equals("Pathogenic") || 
            m.getClinicalSignificance().equals("Likely Pathogenic")));
        assertFalse(result.stream().anyMatch(m -> 
            m.getClinicalSignificance().equals("Benign")));
    }

    @Test
    void findByMutationType_shouldReturnMatchingType() {
        // ACT
        List<Mutation> result = mutationRepository.findByMutationType("SNV");

        // ASSERT
        assertNotNull(result);
        assertEquals(3, result.size()); // All seeded mutations are SNV
        assertTrue(result.stream().allMatch(m -> m.getMutationType().equals("SNV")));
    }

    @Test
    void countMutationsByProteinChange_shouldReturnCounts() {
        // ARRANGE: Add another mutation with the same protein change as TP53
        Mutation duplicateProtein = new Mutation();
        duplicateProtein.setGeneName("TP53");
        duplicateProtein.setChromosome("17");
        duplicateProtein.setPosition(7579473L);
        duplicateProtein.setReferenceAllele("G");
        duplicateProtein.setAlternateAllele("A");
        duplicateProtein.setMutationType("SNV");
        duplicateProtein.setPatientId("PATIENT-60");
        duplicateProtein.setSampleId("SAMPLE-60");
        duplicateProtein.setProteinChange("p.R175H"); // Same as existing TP53
        duplicateProtein.setCancerType("Lung Adenocarcinoma (TCGA)");
        duplicateProtein.setClinicalSignificance("Pathogenic");
        duplicateProtein.setAlleleFrequency(new BigDecimal("0.75"));
        entityManager.persistAndFlush(duplicateProtein);

        // ACT
        List<Object[]> result = mutationRepository.countMutationsByProteinChange("TP53");

        // ASSERT
        assertNotNull(result);
        assertTrue(result.size() >= 1);
        
        // Find p.R175H count
        Object[] r175hCount = result.stream()
            .filter(r -> "p.R175H".equals(r[0]))
            .findFirst()
            .orElse(null);
        assertNotNull(r175hCount);
        assertEquals(2L, r175hCount[1]);
    }
}