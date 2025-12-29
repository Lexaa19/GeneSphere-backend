package com.gene.sphere.mutationservice.repository;

import com.gene.sphere.mutationservice.model.Mutation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for accessing mutation data from the database.
 * Provides CRUD operations and custom query methods for the Mutation entity.
 * 
 * <p>Data source: TCGA (The Cancer Genome Atlas) lung cancer mutation data from cBioPortal.</p>
 */
@Repository
public interface MutationRepository extends JpaRepository<Mutation, Integer> {

    /**
     * Finds all mutations for a specific gene.
     * @param geneName HGNC gene symbol (e.g., "EGFR", "KRAS", "TP53")
     * @return list of mutations for the gene
     */
    List<Mutation> findByGeneName(String geneName);

    /**
     * Finds all occurrences of a specific protein change across patients.
     * @param proteinChange HGVS notation (e.g., "p.L858R", "p.G12C")
     * @return list of mutations with that protein change
     */
    List<Mutation> findByProteinChange(String proteinChange);

    /**
     * Finds all mutations for a cancer type.
     * @param cancerType cancer type (e.g., "Lung Adenocarcinoma (TCGA)")
     * @return list of mutations for that cancer type
     */
    List<Mutation> findByCancerType(String cancerType);

    /**
     * Finds all mutations for a specific patient.
     * @param patientId TCGA patient identifier (e.g., "TCGA-05-4244-01")
     * @return list of all mutations in that patient
     */
    List<Mutation> findByPatientId(String patientId);

    /**
     * Finds mutations by clinical significance.
     * @param significance clinical classification (e.g., "Pathogenic", "Likely Pathogenic")
     * @return list of mutations with that significance
     */
    List<Mutation> findByClinicalSignificance(String significance);

    /**
     * Finds pathogenic/actionable mutations in a specific gene.
     * @param geneName gene symbol
     * @param significance clinical significance
     * @return list of clinically significant mutations in the gene
     */
    List<Mutation> findByGeneNameAndClinicalSignificance(String geneName, String significance);

    /**
     * Finds high-confidence mutations (high allele frequency) in a gene.
     * @param geneName gene symbol
     * @param minimumFrequency minimum VAF (0.0-1.0, e.g., 0.5 = 50%)
     * @return list of high-frequency mutations (likely driver mutations)
     */
    @Query("SELECT m FROM Mutation m WHERE m.geneName = ?1 AND m.alleleFrequency > ?2")
    List<Mutation> findHighFrequencyMutations(String geneName, BigDecimal minimumFrequency);

    /**
     * Counts mutations per gene, ordered by frequency.
     * @return Object[] containing [geneName, count] ordered by count descending
     */
    @Query("SELECT m.geneName, COUNT(m) FROM Mutation m GROUP BY m.geneName ORDER BY COUNT(m) DESC")
    List<Object[]> countMutationsPerGene();

    /**
     * Finds all mutations for a specific sample/tumor.
     * @param sampleId TCGA sample barcode (e.g., "TCGA-05-4244-01")
     * @return list of mutations in that sample
     */
    List<Mutation> findBySampleId(String sampleId);

    /**
     * Finds mutations in a specific gene within a specific sample.
     * @param geneName gene symbol (e.g., "EGFR")
     * @param sampleId TCGA sample barcode
     * @return list of matching mutations
     */
    List<Mutation> findByGeneNameAndSampleId(String geneName, String sampleId);

    /**
     * Counts occurrences of each protein change for a gene (identifies hotspots).
     * @param geneName gene symbol (e.g., "KRAS")
     * @return Object[] containing [proteinChange, count] ordered by frequency
     */
    @Query("SELECT m.proteinChange, COUNT(m) FROM Mutation m WHERE m.geneName = ?1 AND m.proteinChange IS NOT NULL GROUP BY m.proteinChange ORDER BY COUNT(m) DESC")
    List<Object[]> countMutationsByProteinChange(String geneName);

    /**
     * Checks if a specific mutation exists.
     * @param geneName gene symbol
     * @param proteinChange protein change (e.g., "p.L858R")
     * @return true if mutation exists
     */
    boolean existsByGeneNameAndProteinChange(String geneName, String proteinChange);

    /**
     * Finds all mutations on a specific chromosome.
     * @param chromosome chromosome number (e.g., "7", "X")
     * @return list of mutations on that chromosome
     */
    List<Mutation> findByChromosome(String chromosome);
    
    /**
     * Finds mutations by type.
     * @param mutationType type (e.g., "SNV", "deletion", "insertion")
     * @return list of mutations of that type
     */
    List<Mutation> findByMutationType(String mutationType);

    /**
     * Finds mutations for multiple genes at once (more efficient than separate queries).
     * @param geneNames list of gene symbols
     * @return list of mutations in any of the specified genes
     */
    List<Mutation> findByGeneNameIn(List<String> geneNames);


    /**
     * Finds mutations in a genomic region.
     * @param chromosome chromosome number
     * @param startPosition start position (bp)
     * @param endPosition end position (bp)
     * @return list of mutations in the region
     */
    List<Mutation> findByChromosomeAndPositionBetween(String chromosome, Long startPosition, Long endPosition);

    /**
     * Finds actionable/targetable mutations in driver genes.
     * @param geneNames list of driver genes (e.g., EGFR, KRAS, ALK)
     * @param significances list of clinical significances (e.g., "Pathogenic")
     * @return list of clinically actionable mutations
     */
    @Query("SELECT m FROM Mutation m WHERE m.geneName IN ?1 AND m.clinicalSignificance IN ?2")
    List<Mutation> findActionableMutations(List<String> geneNames, List<String> significances);
}
