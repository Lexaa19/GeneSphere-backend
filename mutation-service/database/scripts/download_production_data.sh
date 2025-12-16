#!/bin/bash

# =================================================================
# Download Real Production Mutation Data from cBioPortal
# =================================================================
# This downloads actual TCGA Lung Adenocarcinoma mutation data
# Study: luad_tcga_pan_can_atlas_2018 (569 samples)
# =================================================================

echo "🧬 GeneSphere Production Data Downloader"
echo "========================================="
echo ""

# Create data directory
mkdir -p ../data
cd ../data

STUDY_ID="luad_tcga_pan_can_atlas_2018"
echo "📊 Study: $STUDY_ID"
echo "📍 Source: https://www.cbioportal.org/"
echo ""

# Download mutation data (public dataset)
echo "⬇️  Downloading mutation data..."
echo "This may take 2-5 minutes depending on connection..."
echo ""

# cBioPortal provides public access to mutation files
# We'll download from their public data repository
wget -q --show-progress \
  "https://cbioportal-datahub.s3.amazonaws.com/luad_tcga_pan_can_atlas_2018.tar.gz" \
  -O luad_tcga_pan_can_atlas_2018.tar.gz

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Download complete!"
    echo ""
    echo "📦 Extracting files..."
    tar -xzf luad_tcga_pan_can_atlas_2018.tar.gz
    
    echo "✅ Extraction complete!"
    echo ""
    echo "📁 Files downloaded:"
    ls -lh luad_tcga_pan_can_atlas_2018/
    echo ""
    echo "🎯 Mutation file: luad_tcga_pan_can_atlas_2018/data_mutations.txt"
    echo ""
    
    # Count mutations
    MUTATION_COUNT=$(wc -l < luad_tcga_pan_can_atlas_2018/data_mutations.txt)
    echo "📊 Total mutations: $MUTATION_COUNT"
    echo ""
    echo "✅ Ready to parse! Next step:"
    echo "   python3 ../scripts/parse_maf_to_sql.py luad_tcga_pan_can_atlas_2018/data_mutations.txt > mutations_production.sql"
    
else
    echo ""
    echo "❌ Download failed. Alternative method:"
    echo ""
    echo "1. Go to: https://www.cbioportal.org/study/summary?id=luad_tcga_pan_can_atlas_2018"
    echo "2. Click 'Download' → 'Download Data'"
    echo "3. Save as: data_mutations.txt"
    echo "4. Run: python3 ../scripts/parse_maf_to_sql.py data_mutations.txt > mutations_production.sql"
fi
