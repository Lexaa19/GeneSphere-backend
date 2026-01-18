package com.gene.sphere.mutationservice.model;

import java.math.BigDecimal;

/**
 * Immutable DTO returned in API responses (never expose JPA Entity directly).
 *
 * @param name Gene name (e.g., "TP53", "EGFR")
 * @param chromosome Chromosome identifier (e.g., "17", "X")
 * @param position Genomic position on the chromosome
 * @param referenceAllele Reference nucleotide(s) at this position
 * @param alternateAllele Mutated nucleotide(s)
 * @param mutationType Type of mutation (e.g., "SNV", "deletion", "insertion")
 * @param patientId De-identified patient identifier
 * @param sampleId TCGA sample ID (e.g., "TCGA-05-4244-01")
 * @param proteinChange Protein-level change (e.g., "p.R273H")
 * @param cancerType Cancer type (e.g., "Lung Adenocarcinoma")
 * @param clinicalSignificance Clinical impact (e.g., "Pathogenic", "Benign")
 * @param alleleFrequency Frequency of the alternate allele (0.0 to 1.0)
 */
public record MutationRecord(

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

}
