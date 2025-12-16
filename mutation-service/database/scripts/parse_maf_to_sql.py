#!/usr/bin/env python3
"""
Parse MAF (Mutation Annotation Format) file from cBioPortal/TCGA
and generate SQL INSERT statements for production use.

MAF format is the standard for cancer mutation data.
"""

import sys
import csv
from collections import defaultdict

def parse_maf_file(maf_file):
    """
    Parse MAF file and generate SQL statements.
    MAF columns we care about:
    - Hugo_Symbol: Gene name
    - Chromosome: Chromosome number
    - Start_Position: Genomic position
    - Reference_Allele: Normal DNA
    - Tumor_Seq_Allele2: Mutated DNA
    - Variant_Classification: Mutation type
    - Variant_Type: SNP, DEL, INS
    - Protein_Change: Amino acid change
    - Tumor_Sample_Barcode: Patient ID
    - t_alt_count, t_ref_count: For VAF calculation
    """
    
    print("-- =====================================================", file=sys.stderr)
    print("-- PRODUCTION Lung Cancer Mutation Data", file=sys.stderr)
    print("-- =====================================================", file=sys.stderr)
    print("-- Source: cBioPortal TCGA Lung Adenocarcinoma", file=sys.stderr)
    print("-- Study: luad_tcga_pan_can_atlas_2018", file=sys.stderr)
    print("-- Data Type: Real patient sequencing data", file=sys.stderr)
    print("-- Quality: Production-grade", file=sys.stderr)
    print("-- =====================================================", file=sys.stderr)
    print("", file=sys.stderr)
    
    mutations = []
    gene_counts = defaultdict(int)
    
    with open(maf_file, 'r') as f:
        # MAF files are tab-separated
        reader = csv.DictReader(f, delimiter='\t')
        
        print(f"📖 Reading MAF file: {maf_file}", file=sys.stderr)
        print("⏳ Parsing mutations...", file=sys.stderr)
        
        for row in reader:
            try:
                # Extract fields
                gene = row.get('Hugo_Symbol', 'UNKNOWN')
                chromosome = row.get('Chromosome', '').replace('chr', '')
                position = row.get('Start_Position', '0')
                ref_allele = row.get('Reference_Allele', 'N')
                alt_allele = row.get('Tumor_Seq_Allele2', 'N')
                variant_class = row.get('Variant_Classification', 'Unknown')
                protein_change = row.get('HGVSp_Short', row.get('Protein_Change', ''))
                sample_id = row.get('Tumor_Sample_Barcode', 'UNKNOWN')
                
                # Calculate VAF if available
                try:
                    t_alt = int(row.get('t_alt_count', 0))
                    t_ref = int(row.get('t_ref_count', 0))
                    if t_alt + t_ref > 0:
                        vaf = round(t_alt / (t_alt + t_ref), 4)
                    else:
                        vaf = None
                except:
                    vaf = None
                
                # Map variant classification to our schema
                mutation_type = map_variant_class(variant_class)
                
                # Determine clinical significance
                clinical_sig = determine_clinical_significance(gene, protein_change, mutation_type)
                
                # Skip if essential data is missing
                if not gene or gene == 'Unknown' or not chromosome:
                    continue
                
                # Truncate long alleles
                ref_allele = ref_allele[:100] if ref_allele else 'N'
                alt_allele = alt_allele[:100] if alt_allele else 'N'
                protein_change = protein_change[:50] if protein_change else ''
                
                gene_counts[gene] += 1
                
                mutations.append({
                    'gene': gene,
                    'chr': chromosome,
                    'pos': position,
                    'ref': ref_allele,
                    'alt': alt_allele,
                    'type': mutation_type,
                    'protein': protein_change,
                    'sample': sample_id,
                    'clinical_sig': clinical_sig,
                    'vaf': vaf
                })
                
            except Exception as e:
                print(f"⚠️  Skipped row due to error: {e}", file=sys.stderr)
                continue
    
    print(f"", file=sys.stderr)
    print(f"✅ Parsed {len(mutations)} mutations", file=sys.stderr)
    print(f"📊 Unique genes: {len(gene_counts)}", file=sys.stderr)
    print(f"", file=sys.stderr)
    
    # Generate SQL
    generate_sql(mutations, gene_counts)


def map_variant_class(variant_class):
    """Map MAF variant classification to our schema"""
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
    }
    return mapping.get(variant_class, 'SNV')


def determine_clinical_significance(gene, protein_change, mut_type):
    """Determine clinical significance based on gene and mutation"""
    
    # Tier 1: FDA-approved targetable genes
    actionable_genes = ['EGFR', 'ALK', 'ROS1', 'BRAF', 'MET', 'RET', 'NTRK1', 'NTRK2', 'NTRK3', 'ERBB2']
    
    # Known pathogenic hotspots
    hotspots = ['L858R', 'T790M', 'V600E', 'G12', 'G13', 'Q61', 'del', 'ins']
    
    if gene in actionable_genes:
        if any(hotspot in protein_change for hotspot in hotspots):
            return 'Pathogenic'
        elif mut_type in ['deletion', 'insertion']:
            return 'Likely Pathogenic'
        else:
            return 'Uncertain Significance'
    elif gene == 'TP53':
        if mut_type in ['deletion', 'SNV']:
            return 'Likely Pathogenic'
    
    return 'Uncertain Significance'


def generate_sql(mutations, gene_counts):
    """Generate SQL INSERT statements"""
    
    print("-- =====================================================")
    print("-- PRODUCTION Mutation Data SQL Import")
    print("-- =====================================================")
    print(f"-- Total mutations: {len(mutations)}")
    print(f"-- Unique genes: {len(gene_counts)}")
    print("--")
    print("-- Top 10 mutated genes:")
    for gene, count in sorted(gene_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
        print(f"--   {gene}: {count} mutations")
    print("-- =====================================================")
    print("")
    
    if not mutations:
        print("-- No mutations to import")
        return
    
    # Limit to reasonable size for initial import
    max_mutations = min(len(mutations), 10000)
    
    print(f"-- Importing first {max_mutations} mutations (limit for initial load)")
    print("")
    print("INSERT INTO mutations (gene_name, chromosome, position, reference_allele, alternate_allele,")
    print("                       mutation_type, patient_id, cancer_type, clinical_significance, allele_frequency)")
    print("VALUES")
    
    for i, mut in enumerate(mutations[:max_mutations]):
        vaf_str = str(mut['vaf']) if mut['vaf'] else 'NULL'
        
        sql_line = (
            f"  ('{mut['gene']}', '{mut['chr']}', {mut['pos']}, "
            f"'{mut['ref']}', '{mut['alt']}', '{mut['type']}', "
            f"'{mut['sample']}', 'Lung Adenocarcinoma (TCGA)', "
            f"'{mut['clinical_sig']}', {vaf_str})"
        )
        
        if i < len(mutations[:max_mutations]) - 1:
            print(sql_line + ",")
        else:
            print(sql_line + ";")
    
    print("")
    print("-- =====================================================")
    print(f"-- Import complete: {max_mutations} mutations")
    print("-- =====================================================")
    print("")
    print("-- Verify:")
    print("SELECT COUNT(*) FROM mutations;")
    print("SELECT gene_name, COUNT(*) FROM mutations GROUP BY gene_name ORDER BY COUNT(*) DESC LIMIT 10;")


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 parse_maf_to_sql.py <maf_file>")
        print("")
        print("Example:")
        print("  python3 parse_maf_to_sql.py data_mutations.txt > mutations_production.sql")
        sys.exit(1)
    
    maf_file = sys.argv[1]
    parse_maf_file(maf_file)
