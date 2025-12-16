
🔬 Fetching real mutation data from TCGA Lung Cancer studies...

  Fetching data from luad_tcga_pan_can_atlas_2018...
  ❌ Error fetching from luad_tcga_pan_can_atlas_2018: 404 Client Error:  for url: https://www.cbioportal.org/api/molecular-profiles/acbc_mskcc_2015_mutations/mutated-genes?pageSize=10000
  Study samples: 1

  Fetching data from lusc_tcga_pan_can_atlas_2018...
  ❌ Error fetching from lusc_tcga_pan_can_atlas_2018: 404 Client Error:  for url: https://www.cbioportal.org/api/molecular-profiles/acbc_mskcc_2015_mutations/mutated-genes?pageSize=10000
  Study samples: 1

✓ Aggregated data for 0 unique genes
✓ Total samples across studies: 2


-- ✓ SQL generation complete!
-- To apply: python fetch_tcga_lung_cancer_data.py > tcga_update.sql
--           psql -U your_user -d your_db -f tcga_update.sql
-- Real TCGA Lung Cancer Gene Data
-- Data source: cBioPortal TCGA PanCancer Atlas
-- Studies: Lung Adenocarcinoma (LUAD) + Lung Squamous Cell Carcinoma (LUSC)
-- Total samples: 2
-- Generated: December 2025
--
-- This updates genes table with REAL mutation frequencies from TCGA

-- Summary statistics
SELECT COUNT(*) as updated_genes FROM genes WHERE prevalence LIKE '%TCGA lung cancer%';

-- Verification query
SELECT name, prevalence, mutation_effect FROM genes WHERE prevalence LIKE '%TCGA%' ORDER BY name LIMIT 10;
