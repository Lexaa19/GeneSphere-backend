package com.gene.sphere.mutationservice.model;

import java.math.BigDecimal;

/**
 * Immutable DTO returned in API responses (never expose JPA Entity directly).
 *
 * @param geneName             Gene name (e.g., "TP53", "EGFR")
 * @param chromosome           Chromosome identifier (e.g., "17", "X")
 * @param position             Genomic position on the chromosome
 * @param referenceAllele      Reference nucleotide(s) at this position
 * @param alternateAllele      Mutated nucleotide(s)
 * @param mutationType         Type of mutation (e.g., "SNV", "deletion", "insertion")
 * @param patientId            De-identified patient identifier
 * @param sampleId             TCGA sample ID (e.g., "TCGA-05-4244-01")
 * @param proteinChange        Protein-level change (e.g., "p.R273H")
 * @param cancerType           Cancer type (e.g., "Lung Adenocarcinoma")
 * @param clinicalSignificance Clinical impact (e.g., "Pathogenic", "Benign")
 * @param alleleFrequency      Frequency of the alternate allele (0.0 to 1.0, may be null if unknown)
 */
public record MutationDto(

        String geneName,
        String chromosome,
        Long position,
        String referenceAllele,
        String alternateAllele,
        String mutationType,
        String patientId,
        String sampleId,
        String proteinChange,
        String cancerType,
        String clinicalSignificance,
        BigDecimal alleleFrequency

) {
    public MutationDto {
        if (geneName == null || geneName.isBlank()) {
            throw new IllegalArgumentException("Gene name cannot be null or blank");
        }
        if (chromosome == null || chromosome.isBlank()) {
            throw new IllegalArgumentException("Chromosome cannot be null or blank");
        }
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (referenceAllele == null || referenceAllele.isBlank()) {
            throw new IllegalArgumentException("Reference allele cannot be null or blank");
        }
        if (alternateAllele == null || alternateAllele.isBlank()) {
            throw new IllegalArgumentException("Alternate allele cannot be null or blank");
        }
        if (mutationType == null || mutationType.isBlank()) {
            throw new IllegalArgumentException("Mutation type cannot be null or blank");
        }
        if (patientId == null || patientId.isBlank()) {
            throw new IllegalArgumentException("Patient id cannot be null or blank");
        }
        if (cancerType == null || cancerType.isBlank()) {
            throw new IllegalArgumentException("Cancer type cannot be null or blank");
        }
        if (alleleFrequency != null) {
            if (alleleFrequency.compareTo(BigDecimal.ZERO) < 0 || alleleFrequency.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Allele frequency must be between 0.0 and 1.0 (inclusive)");
            }
        }
    }
}
