#!/bin/bash

# Download real lung cancer mutation data from cBioPortal
# This script fetches mutation data from TCGA Lung Adenocarcinoma study

echo "🔬 Downloading lung cancer mutation data from cBioPortal..."

# Create data directory
mkdir -p ../data

# Study ID: luad_tcga (Lung Adenocarcinoma TCGA)
STUDY_ID="luad_tcga_pan_can_atlas_2018"

echo "📊 Fetching mutations from study: $STUDY_ID"

# Download mutations using cBioPortal API
curl "https://www.cbioportal.org/api/studies/$STUDY_ID/molecular-profiles/mutations/mutations" \
  -H "Accept: application/json" \
  -o ../data/luad_mutations_raw.json

echo "✅ Downloaded mutations data to: ../data/luad_mutations_raw.json"

echo ""
echo "Next steps:"
echo "1. Review the downloaded file"
echo "2. Run the Python parser script to convert to SQL"
echo "3. Import into your database"
