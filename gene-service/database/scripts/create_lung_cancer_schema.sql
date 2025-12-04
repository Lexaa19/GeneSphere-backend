-- =====================================================
-- Lung Cancer Genomics Platform - Database Schema
-- Additional tables to complement genes and mutations
-- =====================================================

-- 1. SAMPLES TABLE (Patient/Tumor Samples)
CREATE TABLE IF NOT EXISTS samples (
    id SERIAL PRIMARY KEY,
    sample_id VARCHAR(100) UNIQUE NOT NULL,
    patient_id VARCHAR(100),
    cancer_type VARCHAR(50) DEFAULT 'Lung Cancer',
    cancer_subtype VARCHAR(50),  -- 'Adenocarcinoma', 'Squamous Cell Carcinoma', 'Small Cell', 'Large Cell'
    stage VARCHAR(20),            -- 'IA', 'IB', 'IIA', 'IIB', 'IIIA', 'IIIB', 'IV'
    smoking_status VARCHAR(50),   -- 'Current Smoker', 'Former Smoker', 'Never Smoker'
    pack_years INT,               -- Smoking intensity
    age_at_diagnosis INT,
    gender VARCHAR(10),           -- 'Male', 'Female'
    ethnicity VARCHAR(50),        -- 'Caucasian', 'Asian', 'African American', 'Hispanic', 'Other'
    sample_type VARCHAR(50),      -- 'Primary Tumor', 'Metastatic', 'Recurrent', 'Normal'
    tissue_source VARCHAR(100),   -- 'Lung', 'Lymph Node', 'Brain', 'Liver', 'Bone'
    sequencing_platform VARCHAR(100),
    tumor_purity DECIMAL(5,2),    -- Percentage
    collection_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster queries
CREATE INDEX idx_samples_cancer_subtype ON samples(cancer_subtype);
CREATE INDEX idx_samples_stage ON samples(stage);
CREATE INDEX idx_samples_patient_id ON samples(patient_id);

-- 2. CLINICAL_DATA TABLE (Patient Outcomes & Treatment)
CREATE TABLE IF NOT EXISTS clinical_data (
    id SERIAL PRIMARY KEY,
    patient_id VARCHAR(100) NOT NULL,
    overall_survival_months DECIMAL(10,2),
    survival_status VARCHAR(20),  -- 'Living', 'Deceased', 'Unknown'
    date_of_death DATE,
    progression_free_survival_months DECIMAL(10,2),
    treatment_lines TEXT,         -- JSON or CSV of treatments
    first_line_treatment VARCHAR(200),
    treatment_response VARCHAR(50), -- 'Complete Response', 'Partial Response', 'Stable Disease', 'Progressive Disease'
    best_response VARCHAR(50),
    performance_status INT,       -- ECOG: 0-5
    metastatic_sites TEXT,        -- 'Brain, Liver, Bone'
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES samples(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_clinical_patient ON clinical_data(patient_id);
CREATE INDEX idx_clinical_survival_status ON clinical_data(survival_status);

-- 3. THERAPIES TABLE (Drug/Treatment Information)
CREATE TABLE IF NOT EXISTS therapies (
    id SERIAL PRIMARY KEY,
    drug_name VARCHAR(200) NOT NULL UNIQUE,
    generic_name VARCHAR(200),
    target_gene VARCHAR(100),     -- 'EGFR', 'ALK', 'PD-L1'
    target_mutation VARCHAR(200), -- 'L858R', 'Exon 19 deletion', 'G12C'
    drug_class VARCHAR(100),      -- 'TKI', 'Chemotherapy', 'Immunotherapy', 'Monoclonal Antibody'
    mechanism_of_action TEXT,
    fda_approved BOOLEAN DEFAULT FALSE,
    approval_date DATE,
    indication TEXT,              -- 'First-line NSCLC with EGFR mutations'
    dosage VARCHAR(200),
    common_side_effects TEXT,
    clinical_trial_phase VARCHAR(20), -- 'Phase I', 'Phase II', 'Phase III', 'Approved'
    response_rate DECIMAL(5,2),   -- Percentage
    median_pfs_months DECIMAL(5,2),
    median_os_months DECIMAL(5,2),
    cost_per_month DECIMAL(10,2),
    manufacturer VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_therapies_target_gene ON therapies(target_gene);
CREATE INDEX idx_therapies_fda_approved ON therapies(fda_approved);

-- 4. GENE_THERAPY_ASSOCIATIONS (Link Genes/Mutations to Therapies)
CREATE TABLE IF NOT EXISTS gene_therapy_associations (
    id SERIAL PRIMARY KEY,
    gene_name VARCHAR(100) NOT NULL,
    therapy_id INT NOT NULL,
    mutation_specific VARCHAR(200),  -- Specific mutation if applicable
    evidence_level VARCHAR(10),      -- 'Level 1', 'Level 2A', 'Level 2B', 'Level 3', 'Level 4'
    evidence_source VARCHAR(100),    -- 'OncoKB', 'NCCN', 'FDA Label', 'Clinical Trial'
    biomarker_type VARCHAR(50),      -- 'Predictive', 'Prognostic', 'Diagnostic'
    recommendation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gene FOREIGN KEY (gene_name) REFERENCES genes(name) ON DELETE CASCADE,
    CONSTRAINT fk_therapy FOREIGN KEY (therapy_id) REFERENCES therapies(id) ON DELETE CASCADE
);

CREATE INDEX idx_gta_gene ON gene_therapy_associations(gene_name);
CREATE INDEX idx_gta_therapy ON gene_therapy_associations(therapy_id);
CREATE INDEX idx_gta_evidence ON gene_therapy_associations(evidence_level);

-- 5. PATIENT_TREATMENTS (Track actual treatments given)
CREATE TABLE IF NOT EXISTS patient_treatments (
    id SERIAL PRIMARY KEY,
    patient_id VARCHAR(100) NOT NULL,
    therapy_id INT NOT NULL,
    treatment_line INT,           -- 1 = first-line, 2 = second-line, etc.
    start_date DATE,
    end_date DATE,
    response VARCHAR(50),         -- 'PR', 'CR', 'SD', 'PD'
    pfs_months DECIMAL(10,2),
    discontinued_reason VARCHAR(200),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient_treatment FOREIGN KEY (patient_id) REFERENCES samples(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_therapy_treatment FOREIGN KEY (therapy_id) REFERENCES therapies(id) ON DELETE CASCADE
);

CREATE INDEX idx_pt_patient ON patient_treatments(patient_id);
CREATE INDEX idx_pt_therapy ON patient_treatments(therapy_id);

-- 6. BIOMARKERS TABLE (Additional biomarkers beyond mutations)
CREATE TABLE IF NOT EXISTS biomarkers (
    id SERIAL PRIMARY KEY,
    sample_id VARCHAR(100) NOT NULL,
    biomarker_name VARCHAR(100) NOT NULL,  -- 'PD-L1', 'TMB', 'MSI'
    biomarker_value VARCHAR(100),          -- '50%', 'High', '15 mut/Mb'
    biomarker_category VARCHAR(50),        -- 'Immunotherapy', 'Targeted Therapy'
    test_method VARCHAR(100),              -- 'IHC', 'NGS', 'PCR'
    lab_name VARCHAR(200),
    test_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sample_biomarker FOREIGN KEY (sample_id) REFERENCES samples(sample_id) ON DELETE CASCADE
);

CREATE INDEX idx_biomarkers_sample ON biomarkers(sample_id);
CREATE INDEX idx_biomarkers_name ON biomarkers(biomarker_name);

-- =====================================================
-- CREATE USEFUL VIEWS
-- =====================================================

-- View: Complete Patient Profile
CREATE OR REPLACE VIEW patient_profile AS
SELECT 
    s.patient_id,
    s.sample_id,
    s.cancer_subtype,
    s.stage,
    s.age_at_diagnosis,
    s.gender,
    s.smoking_status,
    s.ethnicity,
    COUNT(DISTINCT m.gene_symbol) as mutated_genes_count,
    STRING_AGG(DISTINCT m.gene_symbol, ', ' ORDER BY m.gene_symbol) as mutated_genes,
    c.overall_survival_months,
    c.survival_status,
    c.first_line_treatment
FROM samples s
LEFT JOIN mutations m ON s.sample_id = m.sample_id
LEFT JOIN clinical_data c ON s.patient_id = c.patient_id
GROUP BY s.patient_id, s.sample_id, s.cancer_subtype, s.stage, s.age_at_diagnosis, 
         s.gender, s.smoking_status, s.ethnicity, c.overall_survival_months, 
         c.survival_status, c.first_line_treatment;

-- View: Actionable Mutations (mutations with FDA-approved therapies)
CREATE OR REPLACE VIEW actionable_mutations AS
SELECT DISTINCT
    m.gene_symbol,
    m.protein_change,
    m.mutation_type,
    COUNT(DISTINCT m.sample_id) as sample_count,
    g.therapies,
    g.prevalence
FROM mutations m
JOIN genes g ON m.gene_symbol = g.name
WHERE g.therapies NOT LIKE '%No direct therapies%'
  AND g.therapies IS NOT NULL
  AND g.therapies != ''
GROUP BY m.gene_symbol, m.protein_change, m.mutation_type, g.therapies, g.prevalence
ORDER BY sample_count DESC;

-- View: Gene Mutation Statistics
CREATE OR REPLACE VIEW gene_mutation_stats AS
SELECT 
    g.name as gene_symbol,
    g.description,
    g.normal_function,
    g.mutation_effect,
    g.prevalence,
    g.therapies,
    COUNT(m.id) as total_mutations,
    COUNT(DISTINCT m.sample_id) as affected_samples,
    COUNT(DISTINCT m.protein_change) as unique_variants,
    STRING_AGG(DISTINCT m.mutation_type, ', ') as mutation_types,
    g.research_links
FROM genes g
LEFT JOIN mutations m ON g.name = m.gene_symbol
GROUP BY g.id, g.name, g.description, g.normal_function, g.mutation_effect, 
         g.prevalence, g.therapies, g.research_links
ORDER BY affected_samples DESC NULLS LAST;

-- View: Sample Mutation Burden
CREATE OR REPLACE VIEW sample_mutation_burden AS
SELECT 
    s.sample_id,
    s.patient_id,
    s.cancer_subtype,
    s.stage,
    s.smoking_status,
    COUNT(m.id) as mutation_count,
    COUNT(DISTINCT m.gene_symbol) as genes_affected,
    STRING_AGG(DISTINCT 
        CASE 
            WHEN g.therapies NOT LIKE '%No direct therapies%' THEN m.gene_symbol 
        END, ', ') as actionable_genes
FROM samples s
LEFT JOIN mutations m ON s.sample_id = m.sample_id
LEFT JOIN genes g ON m.gene_symbol = g.name
GROUP BY s.sample_id, s.patient_id, s.cancer_subtype, s.stage, s.smoking_status
ORDER BY mutation_count DESC;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Show all tables
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- Show all views
SELECT 
    schemaname,
    viewname
FROM pg_views
WHERE schemaname = 'public'
ORDER BY viewname;

SELECT 'Database schema created successfully!' as status;
