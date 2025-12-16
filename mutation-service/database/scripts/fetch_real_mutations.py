#!/usr/bin/env python3
"""
Fetch lung cancer mutation data from cBioPortal API.
Creates SQL INSERT statements directly.

This script gets REAL mutation data from TCGA studies.
"""

import requests
import sys
from collections import defaultdict

# cBioPortal API base URL
API_BASE = "https://www.cbioportal.org/api"

# Lung cancer studies to query
LUNG_STUDIES = [
    "luad_tcga_pan_can_atlas_2018",  # Lung Adenocarcinoma
]

def fetch_mutations_alternative(study_id, limit=500):
    """
    Alternative approach: Fetch mutated genes statistics instead.
    """
    try:
        profile_id = f"{study_id}_mutations"
        url = f"{API_BASE}/molecular-profiles/{profile_id}/mutated-genes"
        
        params = {
            "pageSize": 100,
            "pageNumber": 0
        }
        
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()
        
        # This gives us gene-level statistics, not individual mutations
        # We'll need to create synthetic mutations based on this
        print(f"  ℹ️  Got gene-level data (not individual mutations)", file=sys.stderr)
        return []
        
    except Exception as e:
        print(f"  ❌ Alternative approach also failed: {e}", file=sys.stderr)
        return []


def fetch_mutations(study_id, limit=500):
    """
    Fetch mutations from a study using cBioPortal API v2.
    """
    print(f"🔬 Fetching mutations from {study_id}...", file=sys.stderr)
    
    # Step 1: Get all sample IDs from the study
    samples_url = f"{API_BASE}/studies/{study_id}/samples"
    
    try:
        print(f"  → Getting sample list...", file=sys.stderr)
        samples_response = requests.get(samples_url, params={"projection": "SUMMARY"}, timeout=30)
        samples_response.raise_for_status()
        samples = samples_response.json()
        
        if not samples:
            print(f"  ⚠️  No samples found in study", file=sys.stderr)
            return []
        
        print(f"  ✓ Found {len(samples)} samples", file=sys.stderr)
        
        # Limit to first N samples to avoid overwhelming the API
        sample_ids = [s['sampleId'] for s in samples[:50]]  # Limit to 50 samples
        
        # Step 2: Get molecular profile ID for mutations
        profile_id = f"{study_id}_mutations"
        
        # Step 3: Fetch mutations for these samples
        print(f"  → Fetching mutations from {len(sample_ids)} samples...", file=sys.stderr)
        mutations_url = f"{API_BASE}/molecular-profiles/{profile_id}/mutations/fetch"
        
        payload = {
            "sampleIds": sample_ids,
            "projection": "DETAILED"
        }
        
        mutations_response = requests.post(
            mutations_url, 
            json=payload,
            headers={"Content-Type": "application/json"},
            timeout=60
        )
        mutations_response.raise_for_status()
        
        mutations = mutations_response.json()
        print(f"  ✓ Fetched {len(mutations)} mutations", file=sys.stderr)
        return mutations[:limit]  # Limit total mutations
        
    except requests.exceptions.RequestException as e:
        print(f"  ❌ Error: {e}", file=sys.stderr)
        # Try alternative approach - get study details
        print(f"  → Trying alternative API endpoint...", file=sys.stderr)
        return fetch_mutations_alternative(study_id, limit)


def generate_sql(mutations):
    """
    Generate SQL INSERT statements from mutation data.
    """
    print("-- =====================================================")
    print("-- REAL Lung Cancer Mutation Data from cBioPortal/TCGA")
    print("-- =====================================================")
    print("-- Study: TCGA Lung Adenocarcinoma PanCancer Atlas")
    print("-- Source: https://www.cbioportal.org/")
    print("-- Total mutations: " + str(len(mutations)))
    print("")
    
    # Group by gene for statistics
    gene_counts = defaultdict(int)
    
    sql_values = []
    seen = set()
    
    for mutation in mutations:
        try:
            gene = mutation.get('gene', {}).get('hugoGeneSymbol', 'UNKNOWN')
            chr_raw = mutation.get('chr', '')
            chromosome = chr_raw.replace('chr', '') if chr_raw else 'UNKNOWN'
            position = mutation.get('startPosition', 0)
            ref = mutation.get('referenceAllele', 'N')[:100]  # Limit length
            alt = mutation.get('variantAllele', 'N')[:100]
            
            # Mutation type mapping
            mut_type_raw = mutation.get('mutationType', 'Unknown')
            if 'Missense' in mut_type_raw or 'Nonsense' in mut_type_raw:
                mut_type = 'SNV'
            elif 'Frame_Shift_Del' in mut_type_raw or 'In_Frame_Del' in mut_type_raw:
                mut_type = 'deletion'
            elif 'Frame_Shift_Ins' in mut_type_raw or 'In_Frame_Ins' in mut_type_raw:
                mut_type = 'insertion'
            else:
                mut_type = 'SNV'
            
            sample_id = mutation.get('sampleId', 'UNKNOWN')
            protein_change = mutation.get('proteinChange', '')
            
            # Clinical significance (simplified)
            if gene in ['EGFR', 'KRAS', 'ALK', 'BRAF', 'ROS1', 'MET', 'RET']:
                if any(hotspot in protein_change for hotspot in ['L858R', 'G12', 'T790M', 'V600E']):
                    clinical_sig = 'Pathogenic'
                else:
                    clinical_sig = 'Likely Pathogenic'
            else:
                clinical_sig = 'Uncertain Significance'
            
            # Create unique key
            unique_key = f"{gene}_{chromosome}_{position}_{ref}_{alt}_{sample_id}"
            if unique_key in seen:
                continue
            seen.add(unique_key)
            
            gene_counts[gene] += 1
            
            # SQL value
            sql_values.append(
                f"('{gene}', '{chromosome}', {position}, '{ref}', '{alt}', "
                f"'{mut_type}', '{sample_id}', 'Lung Adenocarcinoma (TCGA)', '{clinical_sig}', NULL)"
            )
            
        except Exception as e:
            print(f"-- Warning: Skipped mutation due to error: {e}", file=sys.stderr)
            continue
    
    # Print INSERT statement
    if sql_values:
        print("INSERT INTO mutations (gene_name, chromosome, position, reference_allele, alternate_allele,")
        print("                       mutation_type, patient_id, cancer_type, clinical_significance, allele_frequency)")
        print("VALUES")
        
        for i, value in enumerate(sql_values):
            if i < len(sql_values) - 1:
                print("  " + value + ",")
            else:
                print("  " + value + ";")
        
        print("")
        print("-- =====================================================")
        print(f"-- Successfully imported {len(sql_values)} mutations")
        print("-- =====================================================")
        print("")
        
        # Print statistics
        print("-- Top 10 most mutated genes in this dataset:")
        for gene, count in sorted(gene_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
            print(f"--   {gene}: {count} mutations")
        
        print("")
        print("-- Verify import:")
        print("SELECT COUNT(*) as total_mutations FROM mutations;")
        print("SELECT gene_name, COUNT(*) as mut_count FROM mutations GROUP BY gene_name ORDER BY mut_count DESC LIMIT 10;")
    else:
        print("-- No mutations to import")


def main():
    print("🧬 GeneSphere Mutation Data Fetcher", file=sys.stderr)
    print("=" * 50, file=sys.stderr)
    print("", file=sys.stderr)
    
    all_mutations = []
    
    for study in LUNG_STUDIES:
        mutations = fetch_mutations(study, limit=500)
        all_mutations.extend(mutations)
        print(f"✅ Fetched {len(mutations)} mutations from {study}", file=sys.stderr)
    
    print("", file=sys.stderr)
    print(f"📊 Total mutations collected: {len(all_mutations)}", file=sys.stderr)
    print("", file=sys.stderr)
    print("🔄 Generating SQL...", file=sys.stderr)
    print("", file=sys.stderr)
    
    generate_sql(all_mutations)
    
    print("", file=sys.stderr)
    print("✅ Done! SQL output written to stdout", file=sys.stderr)
    print("", file=sys.stderr)
    print("To save to file:", file=sys.stderr)
    print("  python fetch_real_mutations.py > mutations_tcga.sql", file=sys.stderr)


if __name__ == '__main__':
    main()
