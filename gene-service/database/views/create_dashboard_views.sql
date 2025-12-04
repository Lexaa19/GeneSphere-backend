-- Enhanced gene statistics query for React dashboard
-- This provides the rich data shown in cBioPortal

-- 1. Gene mutation frequency and statistics
CREATE OR REPLACE VIEW gene_mutation_stats AS
SELECT 
    g.name as gene_symbol,
    g.description,
    COUNT(m.id) as total_mutations,
    COUNT(DISTINCT m.sample_id) as affected_samples,
    ROUND((COUNT(DISTINCT m.sample_id)::float / (SELECT COUNT(DISTINCT sample_id) FROM mutations) * 100), 2) as mutation_frequency_percent,
    STRING_AGG(DISTINCT m.mutation_type, ', ') as mutation_types,
    STRING_AGG(DISTINCT m.protein_change, ', ') as protein_changes,
    g.therapies,
    g.prevalence,
    g.research_links
FROM genes g
LEFT JOIN mutations m ON g.name = m.gene_symbol
GROUP BY g.id, g.name, g.description, g.therapies, g.prevalence, g.research_links
ORDER BY total_mutations DESC;

-- 2. Mutation type distribution (for pie charts)
CREATE OR REPLACE VIEW mutation_type_distribution AS
SELECT 
    mutation_type,
    COUNT(*) as count,
    ROUND((COUNT(*)::float / (SELECT COUNT(*) FROM mutations) * 100), 2) as percentage
FROM mutations 
WHERE mutation_type IS NOT NULL
GROUP BY mutation_type
ORDER BY count DESC;

-- 3. Top mutated genes (for bar charts)
CREATE OR REPLACE VIEW top_mutated_genes AS
SELECT 
    gene_symbol,
    COUNT(*) as mutation_count,
    COUNT(DISTINCT sample_id) as sample_count,
    ROUND((COUNT(DISTINCT sample_id)::float / (SELECT COUNT(DISTINCT sample_id) FROM mutations) * 100), 2) as sample_percentage
FROM mutations 
WHERE gene_symbol IS NOT NULL
GROUP BY gene_symbol
ORDER BY mutation_count DESC
LIMIT 20;

-- 4. Sample mutation burden (for histograms)
CREATE OR REPLACE VIEW sample_mutation_burden AS
SELECT 
    sample_id,
    COUNT(*) as mutation_count,
    COUNT(DISTINCT gene_symbol) as genes_affected
FROM mutations
GROUP BY sample_id
ORDER BY mutation_count DESC;

-- 5. Gene co-occurrence analysis (which genes are mutated together)
CREATE OR REPLACE VIEW gene_cooccurrence AS
SELECT 
    m1.gene_symbol as gene1,
    m2.gene_symbol as gene2,
    COUNT(DISTINCT m1.sample_id) as cooccurrence_count,
    ROUND((COUNT(DISTINCT m1.sample_id)::float / (SELECT COUNT(DISTINCT sample_id) FROM mutations) * 100), 2) as cooccurrence_percentage
FROM mutations m1
JOIN mutations m2 ON m1.sample_id = m2.sample_id
WHERE m1.gene_symbol < m2.gene_symbol  -- Avoid duplicates
GROUP BY m1.gene_symbol, m2.gene_symbol
HAVING COUNT(DISTINCT m1.sample_id) > 5  -- Only show significant co-occurrences
ORDER BY cooccurrence_count DESC
LIMIT 50;
