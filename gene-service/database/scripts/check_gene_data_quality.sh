#!/bin/bash
# Test script to validate gene data population
# Run this after populating your database with gene data

echo "==================================="
echo "Gene Data Quality Check"
echo "==================================="
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "❌ Error: psql not found. Please install PostgreSQL client."
    exit 1
fi

# Set your database connection details
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-gene_sphere_db}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

echo "Connecting to database: $DB_NAME"
echo "User: $DB_USER"
echo "Host: $DB_HOST:$DB_PORT"
echo ""

# Test 1: Check total genes
echo "📊 Test 1: Total Gene Count"
psql -U $DB_USER -d $DB_NAME -h $DB_HOST -p $DB_PORT -c \
"SELECT COUNT(*) as total_genes FROM genes;" -t | xargs echo "Total genes:"

# Test 2: Check enriched vs placeholder genes
echo ""
echo "📊 Test 2: Data Quality Distribution"
psql -U $DB_USER -d $DB_NAME -h $DB_HOST -p $DB_PORT << EOF
SELECT 
    COUNT(*) FILTER (WHERE description != 'Gene associated with cancer mutations' 
                      AND description != 'Gene with documented cancer mutations') as enriched_genes,
    COUNT(*) FILTER (WHERE description = 'Gene associated with cancer mutations' 
                      OR description = 'Gene with documented cancer mutations') as placeholder_genes,
    ROUND(100.0 * COUNT(*) FILTER (WHERE description != 'Gene associated with cancer mutations' 
                                    AND description != 'Gene with documented cancer mutations') / COUNT(*), 1) as enrichment_percentage
FROM genes;
EOF

# Test 3: Check key actionable genes
echo ""
echo "📊 Test 3: Key Actionable Genes Status"
psql -U $DB_USER -d $DB_NAME -h $DB_HOST -p $DB_PORT << EOF
SELECT 
    name,
    CASE 
        WHEN therapies LIKE '%Erlotinib%' OR therapies LIKE '%Crizotinib%' OR therapies LIKE '%Sotorasib%'
        THEN '✅ Complete'
        ELSE '❌ Missing therapy data'
    END as status,
    LENGTH(description) as desc_length,
    LENGTH(prevalence) as prev_length
FROM genes
WHERE name IN ('EGFR', 'KRAS', 'ALK', 'ROS1', 'BRAF', 'MET', 'RET', 'TP53')
ORDER BY name;
EOF

# Test 4: Sample enriched genes
echo ""
echo "📊 Test 4: Sample of Enriched Genes"
psql -U $DB_USER -d $DB_NAME -h $DB_HOST -p $DB_PORT << EOF
SELECT 
    name,
    LEFT(description, 60) as description,
    prevalence
FROM genes
WHERE description != 'Gene associated with cancer mutations'
  AND description != 'Gene with documented cancer mutations'
ORDER BY name
LIMIT 5;
EOF

# Test 5: Genes still needing enrichment
echo ""
echo "📊 Test 5: Top 10 Genes Needing Enrichment"
psql -U $DB_USER -d $DB_NAME -h $DB_HOST -p $DB_PORT << EOF
SELECT 
    g.name,
    COUNT(m.id) as mutation_count
FROM genes g
LEFT JOIN mutations m ON g.name = m.gene_symbol
WHERE g.description = 'Gene associated with cancer mutations'
   OR g.description = 'Gene with documented cancer mutations'
GROUP BY g.name
ORDER BY mutation_count DESC
LIMIT 10;
EOF

echo ""
echo "==================================="
echo "✅ Quality check complete!"
echo ""
echo "Next steps:"
echo "1. If enrichment_percentage < 20%, run: curated_lung_cancer_genes.sql"
echo "2. For remaining genes, use: fetch_lung_cancer_gene_data.py"
echo "3. Review gene pages on OncoKB: https://www.oncokb.org/"
echo "==================================="
