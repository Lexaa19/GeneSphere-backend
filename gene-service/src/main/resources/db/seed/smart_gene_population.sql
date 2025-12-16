-- More sophisticated approach: Create detailed entries for top mutated genes
-- and basic entries for the rest

-- First, let's see the top 20 most mutated genes
SELECT 
    gene_symbol,
    COUNT(*) as mutation_count,
    COUNT(DISTINCT sample_id) as affected_samples,
    STRING_AGG(DISTINCT mutation_type, ', ') as mutation_types
FROM mutations 
WHERE gene_symbol IS NOT NULL AND gene_symbol != ''
GROUP BY gene_symbol 
ORDER BY mutation_count DESC 
LIMIT 20;

-- Create detailed entries for the top 10 most mutated genes with real cancer relevance
WITH top_genes AS (
    SELECT gene_symbol, COUNT(*) as mutation_count
    FROM mutations 
    GROUP BY gene_symbol 
    ORDER BY mutation_count DESC 
    LIMIT 10
)
INSERT INTO genes (name, description, normal_function, mutation_effect, prevalence, therapies, research_links)
SELECT 
    tg.gene_symbol,
    CASE 
        WHEN tg.gene_symbol = 'TP53' THEN 'Tumor Protein p53 - Guardian of the Genome'
        WHEN tg.gene_symbol = 'KRAS' THEN 'Kirsten Rat Sarcoma Viral Oncogene'
        WHEN tg.gene_symbol = 'EGFR' THEN 'Epidermal Growth Factor Receptor'
        WHEN tg.gene_symbol = 'PIK3CA' THEN 'Phosphatidylinositol-4,5-Bisphosphate 3-Kinase Catalytic Subunit Alpha'
        WHEN tg.gene_symbol = 'BRAF' THEN 'B-Raf Proto-Oncogene'
        WHEN tg.gene_symbol = 'APC' THEN 'Adenomatous Polyposis Coli Tumor Suppressor'
        WHEN tg.gene_symbol = 'PTEN' THEN 'Phosphatase and Tensin Homolog'
        ELSE CONCAT('Cancer-associated gene with ', tg.mutation_count, ' mutations in dataset')
    END as description,
    CASE 
        WHEN tg.gene_symbol = 'TP53' THEN 'DNA damage response, cell cycle control, apoptosis'
        WHEN tg.gene_symbol = 'KRAS' THEN 'GTPase regulating cell proliferation and differentiation'
        WHEN tg.gene_symbol = 'EGFR' THEN 'Growth factor receptor controlling cell proliferation'
        WHEN tg.gene_symbol = 'PIK3CA' THEN 'PI3K/AKT pathway regulation, cell survival'
        WHEN tg.gene_symbol = 'BRAF' THEN 'MAPK/ERK signaling pathway regulation'
        ELSE 'Cellular function varies - frequently mutated in cancer'
    END as normal_function,
    CONCAT('Mutations found in ', tg.mutation_count, ' cases in this dataset') as mutation_effect,
    CONCAT('Found in ', ROUND((tg.mutation_count::float / (SELECT COUNT(DISTINCT sample_id) FROM mutations) * 100), 1), '% of samples') as prevalence,
    CASE 
        WHEN tg.gene_symbol = 'EGFR' THEN 'Erlotinib, Gefitinib, Osimertinib'
        WHEN tg.gene_symbol = 'KRAS' THEN 'Sotorasib (G12C), combination therapies'
        WHEN tg.gene_symbol = 'BRAF' THEN 'Vemurafenib, Dabrafenib, combination therapies'
        ELSE 'Therapies depend on specific mutation and cancer type'
    END as therapies,
    CONCAT('https://www.ncbi.nlm.nih.gov/gene/?term=', tg.gene_symbol) as research_links
FROM top_genes tg
WHERE tg.gene_symbol NOT IN (SELECT name FROM genes);

-- Then add basic entries for all remaining genes
INSERT INTO genes (name, description, normal_function, mutation_effect, prevalence, therapies, research_links)
SELECT DISTINCT 
    gene_symbol,
    'Gene with documented cancer mutations',
    'Cellular function varies by gene',
    'Associated with cancer development',
    CONCAT('Mutations found in dataset'),
    'Treatment varies by mutation type',
    CONCAT('https://www.ncbi.nlm.nih.gov/gene/?term=', gene_symbol)
FROM mutations 
WHERE gene_symbol IS NOT NULL 
  AND gene_symbol != ''
  AND gene_symbol NOT IN (SELECT name FROM genes);

-- Final verification
SELECT 
    'Total genes in genes table' as metric,
    COUNT(*) as count
FROM genes
UNION ALL
SELECT 
    'Unique genes in mutations table' as metric,
    COUNT(DISTINCT gene_symbol) as count
FROM mutations
WHERE gene_symbol IS NOT NULL AND gene_symbol != '';
