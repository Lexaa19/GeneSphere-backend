#!/usr/bin/env python3
"""
Parse cBioPortal mutation data and generate SQL INSERT statements.

Usage:
    python parse_cbioportal_to_sql.py luad_mutations_raw.json > mutations_insert.sql
"""

import json
import sys
from datetime import datetime

def parse_mutation_json(json_file):
    """
    Parse cBioPortal JSON mutation data and convert to SQL INSERT statements.
    """
    
    print("-- Real Lung Cancer Mutation Data from cBioPortal")
    print("-- Study: TCGA Lung Adenocarcinoma PanCancer Atlas")
    print(f"-- Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("-- Source: https://www.cbioportal.org/")
    print("")
    print("-- Insert mutations into database")
    print("INSERT INTO mutations (gene_name, chromosome, position, reference_allele, alternate_allele,")
    print("                       mutation_type, patient_id, cancer_type, clinical_significance, allele_frequency)")
    print("VALUES")
    
    with open(json_file, 'r') as f:
        data = json.load(f)
    
    # Track unique mutations
    mutations = []
    seen = set()
    
    for idx, mutation in enumerate(data):
        try:
            gene = mutation.get('gene', {}).get('hugoGeneSymbol', 'UNKNOWN')
            chromosome = mutation.get('chr', '').replace('chr', '')
            position = mutation.get('startPosition', 0)
            ref_allele = mutation.get('referenceAllele', '')
            alt_allele = mutation.get('variantAllele', '')
            mutation_type = map_mutation_type(mutation.get('mutationType', ''))
            sample_id = mutation.get('sampleId', 'UNKNOWN')
            protein_change = mutation.get('proteinChange', '')
            
            # Determine clinical significance (simplified)
            clinical_sig = determine_clinical_significance(gene, protein_change)
            
            # Mock allele frequency (real data would come from sequencing)
            # In real cBioPortal data, check for 'tumorAltCount' and 'tumorRefCount'
            allele_freq = 0.0  # Would calculate from read counts in real VCF
            
            # Create unique key to avoid duplicates
            unique_key = f"{gene}_{chromosome}_{position}_{ref_allele}_{alt_allele}_{sample_id}"
            
            if unique_key in seen:
                continue
            
            seen.add(unique_key)
            
            # Generate SQL value tuple
            mutations.append(
                f"  ('{gene}', '{chromosome}', {position}, '{ref_allele}', '{alt_allele}', "
                f"'{mutation_type}', '{sample_id}', 'Lung Adenocarcinoma', '{clinical_sig}', NULL)"
            )
            
        except Exception as e:
            print(f"-- Error processing mutation {idx}: {e}", file=sys.stderr)
            continue
    
    # Print SQL INSERT statements
    for i, mutation_sql in enumerate(mutations[:1000]):  # Limit to first 1000 for initial import
        if i < len(mutations) - 1 and i < 999:
            print(mutation_sql + ",")
        else:
            print(mutation_sql + ";")
    
    print("")
    print(f"-- Total mutations parsed: {len(mutations)}")
    print(f"-- SQL statements generated: {min(len(mutations), 1000)}")
    print("")
    print("-- Verify import")
    print("SELECT COUNT(*) FROM mutations;")
    print("SELECT gene_name, COUNT(*) as mutation_count FROM mutations GROUP BY gene_name ORDER BY mutation_count DESC LIMIT 10;")


def map_mutation_type(cbio_type):
    """
    Map cBioPortal mutation types to our schema.
    """
    type_mapping = {
        'Missense_Mutation': 'SNV',
        'Nonsense_Mutation': 'SNV',
        'Frame_Shift_Del': 'deletion',
        'Frame_Shift_Ins': 'insertion',
        'In_Frame_Del': 'deletion',
        'In_Frame_Ins': 'insertion',
        'Splice_Site': 'SNV',
        'Translation_Start_Site': 'SNV',
        'Fusion': 'fusion',
        'Silent': 'SNV',
    }
    return type_mapping.get(cbio_type, 'SNV')


def determine_clinical_significance(gene, protein_change):
    """
    Simplified clinical significance determination.
    In production, would query ClinVar or OncoKB.
    """
    
    # Known pathogenic mutations in lung cancer
    actionable_genes = ['EGFR', 'KRAS', 'ALK', 'ROS1', 'BRAF', 'MET', 'RET', 'NTRK1', 'ERBB2']
    
    if gene in actionable_genes:
        # Known hotspot mutations
        if 'L858R' in protein_change or 'G12' in protein_change or 'T790M' in protein_change:
            return 'Pathogenic'
        elif 'del' in protein_change.lower() or 'ins' in protein_change.lower():
            return 'Likely Pathogenic'
        else:
            return 'Uncertain Significance'
    else:
        return 'Uncertain Significance'


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python parse_cbioportal_to_sql.py <json_file>")
        sys.exit(1)
    
    json_file = sys.argv[1]
    parse_mutation_json(json_file)
