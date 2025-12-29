-- Bulk Update for protein_change and sample_id columns
-- This is more efficient than individual UPDATE statements
-- 
-- IMPORTANT: Create a backup first!
-- CREATE TABLE mutations_backup AS SELECT * FROM mutations;

BEGIN;

-- Step 1: Create a temporary table with the cBioPortal data
-- You'll need to load the data first using COPY or a script

CREATE TEMP TABLE temp_mutations_update (
    gene_name VARCHAR(50),
    chromosome VARCHAR(5),
    position BIGINT,
    reference_allele VARCHAR(1000),
    alternate_allele VARCHAR(1000),
    sample_id VARCHAR(100),
    protein_change VARCHAR(100)
);

-- Step 2: Load data from cBioPortal file
-- Run this using the Python script or manually load the TSV file

-- For manual loading via COPY:
-- \copy temp_mutations_update(gene_name, chromosome, position, reference_allele, alternate_allele, sample_id, protein_change) FROM 'parsed_data.csv' WITH CSV HEADER;

-- Step 3: Update mutations table from temporary table
UPDATE mutations m
SET 
    sample_id = t.sample_id,
    protein_change = t.protein_change
FROM temp_mutations_update t
WHERE m.gene_name = t.gene_name
  AND m.chromosome = t.chromosome
  AND m.position = t.position
  AND m.reference_allele = t.reference_allele
  AND m.alternate_allele = t.alternate_allele
  AND (m.sample_id IS NULL OR m.protein_change IS NULL);

-- Step 4: Show results
SELECT 'Updates completed!' as status;

SELECT 
    'Total mutations' as metric,
    COUNT(*) as count
FROM mutations
UNION ALL
SELECT 
    'With protein_change',
    COUNT(*)
FROM mutations
WHERE protein_change IS NOT NULL
UNION ALL
SELECT 
    'With sample_id',
    COUNT(*)
FROM mutations
WHERE sample_id IS NOT NULL;

COMMIT;

-- Verification query
SELECT gene_name, protein_change, sample_id, chromosome, position
FROM mutations
WHERE protein_change IS NOT NULL
LIMIT 20;
