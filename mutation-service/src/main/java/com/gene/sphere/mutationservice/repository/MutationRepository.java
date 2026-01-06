package com.gene.sphere.mutationservice.repository;

import com.gene.sphere.mutationservice.model.Mutation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Finds all mutations for a specific gene, case-insensitive.
     * @param geneName HGNC gene symbol (e.g., "EGFR", "KRAS", "TP53")
     * @return list of mutations for the gene
     */
    @Query("SELECT m FROM Mutation m WHERE LOWER(m.geneName) = LOWER(:geneName)")
    List<Mutation> findByGeneName(@Param("geneName") String geneName);

    /**
     * Finds all mutations for a specific gene case-insensitive.
     * @param geneName HGNC gene symbol (e.g., "EGFR", "KRAS", "TP53")
     * @return list of mutations for the gene
     */
    @Query("SELECT m FROM Mutation m WHERE LOWER(m.geneName) = LOWER(:geneName)")
    List<Mutation> findByGeneNameCaseInsensitive(@Param("geneName") String geneName);

    /**
     * Finds all mutations where the gene name contains the given substring, case-insensitive.
     * @param partialGene partial or full gene symbol (e.g., "TP", "egfr")
     * @return list of mutations with gene names containing the substring
     */
    @Query("SELECT m FROM Mutation m WHERE LOWER(m.geneName) LIKE LOWER(CONCAT('%', :partialGene, '%'))")
    List<Mutation> findByGeneNameContainingIgnoreCase(@Param("partialGene") String partialGene);

    /**
     * Finds all occurrences of a specific protein change across patients, case-insensitive.
     * @param proteinChange HGVS notation (e.g., "p.L858R", "p.G12C")
     * @return list of mutations with that protein change
     */
    @Query("SELECT m FROM Mutation m WHERE LOWER(m.proteinChange) = LOWER(:proteinChange)")
    List<Mutation> findByProteinChange(@Param("proteinChange") String proteinChange);

    /**
     * Finds all mutations where the cancer type contains the given substring, case-insensitive.
     * @param partialCancerType partial or full cancer type string (e.g., "lung", "adenocarcinoma")
     * @return list of mutations with cancer types containing the substring
     */
    @Query("SELECT m FROM Mutation m WHERE LOWER(m.cancerType) LIKE LOWER(CONCAT('%', :partialCancerType, '%'))")
    List<Mutation> findByCancerTypeContainingIgnoreCase(@Param("partialCancerType") String partialCancerType);

    /**
     * Finds all mutations for a specific patient.
     * @param patientId TCGA patient identifier (e.g., "TCGA-05-4244-01")
     * @return list of all mutations in that patient
     */
    List<Mutation> findByPatientId(String patientId);

    /**
     * Finds mutations by clinical significance, case-insensitive.
     * @param significance clinical classification (e.g., "Pathogenic", "Likely Pathogenic")
     * @return list of mutations with that significance
     */
    @Query("SELECT m FROM Mutation m WHERE LOWER(m.clinicalSignificance) = LOWER(:significance)")
    List<Mutation> findByClinicalSignificance(@Param("significance") String significance);


    /**
     * Finds pathogenic/actionable mutations in a specific gene, case-insensitive for both fields.
     * @param geneName gene symbol
     * @param significance clinical significance
     * @return list of clinically significant mutations in the gene
     */
    @Query("SELECT m FROM Mutation m WHERE LOWER(m.geneName) = LOWER(:geneName) AND LOWER(m.clinicalSignificance) = LOWER(:significance)")
    List<Mutation> findByGeneNameAndClinicalSignificance(@Param("geneName") String geneName, @Param("significance") String significance);

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
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Mutation m WHERE LOWER(m.geneName) = LOWER(:geneName) AND LOWER(m.proteinChange) = LOWER(:proteinChange)")
    boolean existsByGeneNameAndProteinChange(@Param("geneName") String geneName, @Param("proteinChange") String proteinChange);

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
