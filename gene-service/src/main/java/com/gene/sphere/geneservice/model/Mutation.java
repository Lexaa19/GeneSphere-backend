package com.gene.sphere.geneservice.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Database entity representing one cancer mutation record from TCGA data.
 * Maps to the 'mutations' table in PostgreSQL.
 */
@Data
@Entity
@Table(name = "mutations")
public class Mutation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Gene name (HGNC symbol) where the mutation occurs.
     * Example: "EGFR", "KRAS", "TP53"
     */
    @Column(name = "gene_name", nullable = false, length = 50)
    private String geneName;

    /**
     * Alternative gene symbol (usually same as geneName).
     * Kept for compatibility with different data sources.
     */
    @Column(name = "gene_symbol", length = 50)
    private String geneSymbol;

    /**
     * Chromosome where the mutation is located.
     * Values: 1-22 (autosomes), X, Y (sex chromosomes), MT (mitochondrial)
     */
    @Column(name = "chromosome", nullable = false, length = 5)
    private String chromosome;

    /**
     * Exact genomic position (base pair number) on the chromosome.
     * Example: 55191822 for EGFR L858R mutation
     */
    @Column(name = "position", nullable = false)
    private Long position;

    /**
     * Reference allele - the normal/wildtype DNA sequence at this position.
     * Usually a single base (A, T, C, G) but can be longer for deletions.
     * Example: "G" (normal)
     */
    @Column(name = "reference_allele", nullable = false, length = 1000)
    private String referenceAllele;

    /**
     * Alternate allele - the mutated DNA sequence found in the cancer sample.
     * Example: "T" (mutated from G)
     */
    @Column(name = "alternate_allele", nullable = false, length = 1000)
    private String alternateAllele;

    /**
     * Type of mutation.
     * Values: "SNV" (single nucleotide variant), "deletion", "insertion", "fusion", "CNV"
     */
    @Column(name = "mutation_type", nullable = false, length = 50)
    private String mutationType;

    /**
     * Anonymized patient identifier from TCGA.
     * Example: "TCGA-05-4244-01"
     * Multiple mutations can have the same patient_id.
     */
    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    /**
     * TCGA sample barcode identifying the specific tumor sample.
     * Example: "TCGA-05-4244-01" (01 = primary tumor, 10 = normal tissue)
     */
    @Column(name = "sample_id", length = 100)
    private String sampleId;

    /**
     * Protein-level change caused by the mutation (HGVS notation).
     * Example: "p.L858R" = Leucine at position 858 changed to Arginine
     * Example: "p.G12C" = Glycine at position 12 changed to Cysteine
     */
    @Column(name = "protein_change", length = 100)
    private String proteinChange;

    /**
     * Type of cancer where this mutation was found.
     * Example: "Lung Adenocarcinoma (TCGA)", "Lung Squamous Cell Carcinoma"
     */
    @Column(name = "cancer_type", nullable = false, length = 100)
    private String cancerType;

    /**
     * Clinical significance of the mutation.
     * Values: "Pathogenic", "Likely Pathogenic", "Uncertain Significance", 
     *         "Likely Benign", "Benign"
     * Based on ClinVar/OncoKB classifications.
     */
    @Column(name = "clinical_significance", length = 50)
    private String clinicalSignificance;

    /**
     * Variant Allele Frequency (VAF) - percentage of reads showing the mutation.
     * Range: 0.0000 to 1.0000 (e.g., 0.6523 = 65.23% of reads have the mutation)
     * NULL if not available from sequencing data.
     */
    @Column(name = "allele_frequency", precision = 5, scale = 4)
    private BigDecimal alleleFrequency;
}