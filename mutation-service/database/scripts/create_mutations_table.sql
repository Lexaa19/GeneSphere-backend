-- =====================================================
-- Mutations Table Schema for Lung Cancer
-- =====================================================
-- This table stores somatic mutations from tumor sequencing
-- Data source: cBioPortal, TCGA, COSMIC

CREATE TABLE IF NOT EXISTS mutations (
    -- Unique identifier
    id BIGSERIAL PRIMARY KEY,
    
    -- Gene information
    gene_name VARCHAR(50) NOT NULL,
    -- Example: "EGFR", "KRAS", "ALK"
    -- Links to genes table for enriched information
    
    -- Genomic location
    chromosome VARCHAR(5) NOT NULL,
    -- Example: "7", "12", "X"
    -- Chromosomes: 1-22, X, Y, MT
    
    position BIGINT NOT NULL,
    -- Exact base pair position on chromosome
    -- Example: 55191822 for EGFR L858R
    
    -- Sequence information
    reference_allele VARCHAR(1000) NOT NULL,
    -- Normal/wildtype DNA sequence
    -- Usually single base (A, T, C, G)
    -- Can be longer for deletions: "AATTAAGAGAAGC"
    
    alternate_allele VARCHAR(1000) NOT NULL,
    -- Mutated DNA sequence
    -- Single base for SNVs, longer for indels
    
    -- Mutation classification
    mutation_type VARCHAR(50) NOT NULL,
    -- Values: "SNV", "deletion", "insertion", "fusion", "CNV"
    -- CHECK constraint ensures valid values
    
    -- Patient information
    patient_id VARCHAR(100) NOT NULL,
    -- Anonymized patient identifier
    -- Example: "TCGA-05-4384-01"
    -- Links multiple mutations for same patient
    
    cancer_type VARCHAR(100) NOT NULL DEFAULT 'Lung Adenocarcinoma',
    -- Specific cancer subtype
    -- Values: "Lung Adenocarcinoma", "Lung Squamous Cell Carcinoma", "Small Cell Lung Cancer"
    
    -- Clinical annotation
    clinical_significance VARCHAR(50),
    -- Values: "Pathogenic", "Likely Pathogenic", "Uncertain Significance", "Likely Benign", "Benign"
    -- Based on ClinVar/OncoKB annotations
    
    allele_frequency DECIMAL(5,4),
    -- Variant Allele Frequency (VAF)
    -- Range: 0.0000 to 1.0000
    -- Example: 0.6523 = 65.23% of reads show this mutation
    -- NULL if not available from source data
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT check_chromosome CHECK (chromosome ~ '^([1-9]|1[0-9]|2[0-2]|X|Y|MT)$'),
    CONSTRAINT check_position CHECK (position > 0),
    CONSTRAINT check_mutation_type CHECK (mutation_type IN ('SNV', 'deletion', 'insertion', 'fusion', 'CNV', 'duplication')),
    CONSTRAINT check_clinical_significance CHECK (
        clinical_significance IN ('Pathogenic', 'Likely Pathogenic', 'Uncertain Significance', 'Likely Benign', 'Benign')
        OR clinical_significance IS NULL
    ),
    CONSTRAINT check_allele_frequency CHECK (allele_frequency >= 0 AND allele_frequency <= 1),
    
    -- Unique constraint: same mutation can't be recorded twice for same patient
    CONSTRAINT unique_mutation UNIQUE (gene_name, chromosome, position, alternate_allele, patient_id)
);

-- Indexes for performance
CREATE INDEX idx_mutations_gene ON mutations(gene_name);
CREATE INDEX idx_mutations_patient ON mutations(patient_id);
CREATE INDEX idx_mutations_cancer_type ON mutations(cancer_type);
CREATE INDEX idx_mutations_clinical_sig ON mutations(clinical_significance);
CREATE INDEX idx_mutations_location ON mutations(chromosome, position);

-- Foreign key to genes table (optional, add after genes table exists)
-- ALTER TABLE mutations ADD CONSTRAINT fk_gene FOREIGN KEY (gene_name) REFERENCES genes(name);

-- Comments for documentation
COMMENT ON TABLE mutations IS 'Somatic mutations in lung cancer from TCGA/cBioPortal studies';
COMMENT ON COLUMN mutations.gene_name IS 'HGNC gene symbol (e.g., EGFR, KRAS)';
COMMENT ON COLUMN mutations.position IS 'Genomic position in base pairs (hg38 reference)';
COMMENT ON COLUMN mutations.allele_frequency IS 'Variant allele frequency (VAF) from sequencing reads';
COMMENT ON COLUMN mutations.clinical_significance IS 'Clinical pathogenicity classification from ClinVar/OncoKB';

-- Verification query
SELECT COUNT(*) as total_mutations FROM mutations;
