#!/usr/bin/env python3
"""
Parse TCGA mutation data with complete columns including gene_symbol, protein_change, and sample_id.

Usage:
    python parse_tcga_mutations_complete.py data_mutations.txt > mutations_complete.sql
"""

import sys
from datetime import datetime

def parse_tcga_mutations(input_file):
    """
    Parse TCGA mutation data and generate SQL INSERT statements with all columns.
    """
    
    print("-- Complete Lung Cancer Mutation Data from TCGA")
    print("-- Study: LUAD TCGA PanCancer Atlas 2018")
    print(f"-- Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("-- Source: cBioPortal / TCGA")
    print("")
    
    mutations = []
    seen = set()
    
    with open(input_file, 'r') as f:
        # Read header
        header = f.readline().strip().split('\t')
        
        # Find column indices
        required_columns = [
            'Hugo_Symbol',
            'Chromosome',
            'Start_Position',
            'Reference_Allele',
            'Tumor_Seq_Allele2',
            'Variant_Classification',
            'Tumor_Sample_Barcode',
            'HGVSp_Short',
        ]
        missing_columns = [col for col in required_columns if col not in header]
        if missing_columns:
            print(
                f"-- ERROR: Missing required column(s): {', '.join(missing_columns)}",
                file=sys.stderr,
            )
            sys.exit(1)

        hugo_idx = header.index('Hugo_Symbol')
        chr_idx = header.index('Chromosome')
        start_idx = header.index('Start_Position')
        ref_idx = header.index('Reference_Allele')
        tumor_allele_idx = header.index('Tumor_Seq_Allele2')
        variant_class_idx = header.index('Variant_Classification')
        sample_idx = header.index('Tumor_Sample_Barcode')
        protein_idx = header.index('HGVSp_Short')
        
        line_num = 1
        for line in f:
            line_num += 1
            fields = line.strip().split('\t')
            
            if len(fields) < len(header):
                continue
            
            try:
                gene_symbol = fields[hugo_idx].strip()
                chromosome = fields[chr_idx].strip()
                position = int(fields[start_idx])
                ref_allele = fields[ref_idx].strip()
                alt_allele = fields[tumor_allele_idx].strip()
                variant_class = fields[variant_class_idx].strip()
                sample_id = fields[sample_idx].strip()
                protein_change = fields[protein_idx].strip() if len(fields) > protein_idx and fields[protein_idx].strip() else ''
                
                # Skip invalid entries
                if not gene_symbol or gene_symbol == 'Unknown' or not chromosome:
                    continue
                
                # Map variant classification to mutation type
                mutation_type = map_variant_type(variant_class)
                
                # Determine clinical significance
                clinical_sig = determine_clinical_significance(gene_symbol, protein_change)
                
                # Create unique key
                unique_key = f"{gene_symbol}_{chromosome}_{position}_{ref_allele}_{alt_allele}_{sample_id}"
                
                if unique_key in seen:
                    continue
                
                seen.add(unique_key)
                
                # SQL-safe strings
                gene_symbol_safe = gene_symbol.replace("'", "''")
                ref_allele_safe = ref_allele.replace("'", "''")
                alt_allele_safe = alt_allele.replace("'", "''")
                sample_id_safe = sample_id.replace("'", "''")
                protein_change_safe = protein_change.replace("'", "''") if protein_change else ''
                
                # Build SQL INSERT
                mutations.append({
                    'gene_name': gene_symbol_safe,
                    'gene_symbol': gene_symbol_safe,
                    'chromosome': chromosome,
                    'position': position,
                    'ref_allele': ref_allele_safe,
                    'alt_allele': alt_allele_safe,
                    'mutation_type': mutation_type,
                    'patient_id': sample_id_safe,
                    'sample_id': sample_id_safe,
                    'protein_change': protein_change_safe,
                    'clinical_sig': clinical_sig
                })
                
            except (ValueError, IndexError) as e:
                print(f"-- Warning: Skipping line {line_num}: {e}", file=sys.stderr)
                continue
    
    # Generate SQL
    print("INSERT INTO mutations (gene_name, gene_symbol, chromosome, position, reference_allele, alternate_allele,")
    print("                       mutation_type, patient_id, sample_id, protein_change, cancer_type, clinical_significance)")
    print("VALUES")
    
    for i, mut in enumerate(mutations[:1000]):  # Limit to 1000 for initial import
        protein_val = f"'{mut['protein_change']}'" if mut['protein_change'] else 'NULL'
        
        sql_line = (
            f"  ('{mut['gene_name']}', '{mut['gene_symbol']}', '{mut['chromosome']}', {mut['position']}, "
            f"'{mut['ref_allele']}', '{mut['alt_allele']}', '{mut['mutation_type']}', "
            f"'{mut['patient_id']}', '{mut['sample_id']}', {protein_val}, "
            f"'Lung Adenocarcinoma (TCGA)', '{mut['clinical_sig']}')"
        )
        
        if i < len(mutations) - 1 and i < 999:
            print(sql_line + ",")
        else:
            print(sql_line + ";")
    
    print("")
    print(f"-- Total mutations parsed: {len(mutations)}")
    print(f"-- Mutations inserted: {min(len(mutations), 1000)}")
    print("")
    print("-- Verification queries")
    print("SELECT COUNT(*) as total FROM mutations;")
    print("SELECT gene_symbol, COUNT(*) as count FROM mutations GROUP BY gene_symbol ORDER BY count DESC LIMIT 10;")
    print("SELECT protein_change, COUNT(*) as count FROM mutations WHERE protein_change IS NOT NULL GROUP BY protein_change ORDER BY count DESC LIMIT 10;")


def map_variant_type(variant_class):
    """Map TCGA variant classification to our mutation_type."""
    mapping = {
        'Missense_Mutation': 'SNV',
        'Nonsense_Mutation': 'SNV',
        'Silent': 'SNV',
        'Frame_Shift_Del': 'deletion',
        'Frame_Shift_Ins': 'insertion',
        'In_Frame_Del': 'deletion',
        'In_Frame_Ins': 'insertion',
        'Splice_Site': 'SNV',
        'Translation_Start_Site': 'SNV',
        'Nonstop_Mutation': 'SNV',
        'RNA': 'SNV',
        '3\'UTR': 'SNV',
        '5\'UTR': 'SNV',
        'Intron': 'SNV',
        'IGR': 'SNV',
    }
    return mapping.get(variant_class, 'SNV')


def determine_clinical_significance(gene, protein_change):
    """Determine clinical significance based on gene and protein change."""
    
    # Known actionable genes in lung cancer
    actionable = {
        'EGFR': ['L858R', 'T790M', 'L861Q', 'G719', 'S768I', 'del19'],
        'KRAS': ['G12C', 'G12D', 'G12V', 'G13'],
        'ALK': ['fusion'],
        'ROS1': ['fusion'],
        'BRAF': ['V600E'],
        'MET': ['exon14'],
        'RET': ['fusion'],
        'ERBB2': ['insertion'],
        'NTRK1': ['fusion'],
    }
    
    if gene in actionable:
        # Check for known hotspots
        hotspots = actionable[gene]
        for hotspot in hotspots:
            if hotspot.lower() in protein_change.lower():
                return 'Pathogenic'
        
        # Gene is actionable but not a known hotspot
        if 'del' in protein_change.lower() or 'ins' in protein_change.lower():
            return 'Likely Pathogenic'
        else:
            return 'Uncertain Significance'
    
    # Tumor suppressor genes
    tumor_suppressors = ['TP53', 'STK11', 'KEAP1', 'RB1', 'CDKN2A', 'PTEN', 'NF1', 'SMARCA4']
    if gene in tumor_suppressors:
        if 'Ter' in protein_change or '*' in protein_change or 'fs' in protein_change:
            return 'Pathogenic'
        else:
            return 'Likely Pathogenic'
    
    return 'Uncertain Significance'


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python parse_tcga_mutations_complete.py <data_mutations.txt>")
        print("\nExample:")
        print("  python parse_tcga_mutations_complete.py data_mutations.txt > mutations_complete.sql")
        sys.exit(1)
    
    input_file = sys.argv[1]
    parse_tcga_mutations(input_file)
