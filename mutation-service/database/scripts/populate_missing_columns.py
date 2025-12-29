#!/usr/bin/env python3
"""
Populate missing protein_change and sample_id columns in the mutations table.
Matches existing records by gene_name, chromosome, position, and alleles.

This script generates SQL UPDATE statements from cBioPortal TCGA data.
It reads the data_mutations.txt file (TSV format) and creates UPDATE statements
that can be executed via psql or pgAdmin.

⚠️  SECURITY NOTE: This script uses string formatting for SQL generation, which
is acceptable for generating static SQL files from trusted data sources (TCGA/cBioPortal).
The output should be reviewed before execution. For runtime database updates with
untrusted input, use parameterized queries (psycopg2, SQLAlchemy) instead.

RECOMMENDED APPROACH: Use bulk_update_missing_columns.sql with PostgreSQL's COPY
command for better performance and safety. This script is provided as a fallback
for environments where direct COPY is not available.

Usage:
    python populate_missing_columns.py data_mutations.txt > update_mutations.sql
    psql -U gene_user -d gene_db -f update_mutations.sql

Input file format: TSV file from cBioPortal with columns:
    - Hugo_Symbol (gene name)
    - Chromosome, Start_Position, Reference_Allele, Tumor_Seq_Allele2
    - Tumor_Sample_Barcode (sample_id)
    - HGVSp_Short (protein_change)
"""

import sys
from datetime import datetime

def generate_update_statements(input_file):
    """
    Parse TCGA mutation data and generate SQL UPDATE statements for missing columns.
    """
    
    print("-- Update missing protein_change and sample_id columns")
    print("-- Study: LUAD TCGA PanCancer Atlas 2018")
    print(f"-- Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("")
    print("-- Backup your data first:")
    print("-- CREATE TABLE mutations_backup AS SELECT * FROM mutations;")
    print("")
    
    updates = []
    
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
            'Tumor_Sample_Barcode',
            'HGVSp_Short',
        ]
        missing_columns = [col for col in required_columns if col not in header]
        if missing_columns:
            missing_str = ", ".join(missing_columns)
            print(f"-- ERROR: Missing required column(s): {missing_str}", file=sys.stderr)
            sys.exit(1)

        hugo_idx = header.index('Hugo_Symbol')
        chr_idx = header.index('Chromosome')
        start_idx = header.index('Start_Position')
        ref_idx = header.index('Reference_Allele')
        tumor_allele_idx = header.index('Tumor_Seq_Allele2')
        sample_idx = header.index('Tumor_Sample_Barcode')
        protein_idx = header.index('HGVSp_Short')
        
        print(f"-- Found columns: Hugo_Symbol, Chromosome, Start_Position, Reference_Allele, Tumor_Seq_Allele2")
        print(f"-- Target columns: HGVSp_Short (protein_change), Tumor_Sample_Barcode (sample_id)")
        print("")
        
        line_num = 1
        for line in f:
            line_num += 1
            fields = line.strip().split('\t')
            
            if len(fields) < len(header):
                continue
            
            try:
                gene_name = fields[hugo_idx].strip()
                chromosome = fields[chr_idx].strip()
                position = int(fields[start_idx])
                ref_allele = fields[ref_idx].strip()
                alt_allele = fields[tumor_allele_idx].strip()
                sample_id = fields[sample_idx].strip()
                protein_change = fields[protein_idx].strip() or None
                
                # Skip invalid entries
                if not gene_name or gene_name == 'Unknown' or not chromosome:
                    continue
                
                # SQL-safe strings - escape single quotes for SQL
                gene_name_safe = gene_name.replace("'", "''")
                ref_allele_safe = ref_allele.replace("'", "''")
                alt_allele_safe = alt_allele.replace("'", "''")
                sample_id_safe = sample_id.replace("'", "''")
                # Use None/NULL for missing protein_change instead of empty string
                protein_change_safe = protein_change.replace("'", "''") if protein_change else None
                
                updates.append({
                    'gene_name': gene_name_safe,
                    'chromosome': chromosome,
                    'position': position,
                    'ref_allele': ref_allele_safe,
                    'alt_allele': alt_allele_safe,
                    'sample_id': sample_id_safe,
                    'protein_change': protein_change_safe
                })
                
            except (ValueError, IndexError) as e:
                print(f"-- Warning: Skipping line {line_num}: {e}", file=sys.stderr)
                continue
    
    # Generate UPDATE statements
    # NOTE: This script outputs SQL for review before execution. 
    # For production use, consider using parameterized queries with psycopg2 or SQLAlchemy.
    print("-- Begin transaction for safety")
    print("BEGIN;")
    print("")
    
    max_updates = 1000  # Configurable limit
    for i, upd in enumerate(updates[:max_updates]):
        protein_val = f"'{upd['protein_change']}'" if upd['protein_change'] else 'NULL'
        
        update_stmt = f"""UPDATE mutations
SET sample_id = '{upd['sample_id']}',
    protein_change = {protein_val}
WHERE gene_name = '{upd['gene_name']}'
  AND chromosome = '{upd['chromosome']}'
  AND position = {upd['position']}
  AND reference_allele = '{upd['ref_allele']}'
  AND alternate_allele = '{upd['alt_allele']}'
  AND (sample_id IS NULL OR protein_change IS NULL);
"""
        print(update_stmt)
        
        # Add progress indicator every 100 records
        if (i + 1) % 100 == 0:
            print(f"-- Progress: {i + 1}/{min(len(updates), max_updates)} updates generated")
            print("")
    
    print("")
    print("-- Commit the transaction")
    print("COMMIT;")
    print("")
    print(f"-- Total mutations parsed: {len(updates)}")
    print(f"-- SQL statements generated: {min(len(updates), max_updates)}")
    print("")
    print("-- Verification queries")
    print("SELECT COUNT(*) as total_mutations FROM mutations;")
    print("SELECT COUNT(*) as with_protein_change FROM mutations WHERE protein_change IS NOT NULL;")
    print("SELECT COUNT(*) as with_sample_id FROM mutations WHERE sample_id IS NOT NULL;")
    print("SELECT gene_name, protein_change, sample_id FROM mutations WHERE protein_change IS NOT NULL LIMIT 10;")


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python populate_missing_columns.py <data_mutations.txt>")
        print("")
        print("Example:")
        print("  python populate_missing_columns.py data_mutations.txt > update_mutations.sql")
        print("  psql -U your_user -d your_database -f update_mutations.sql")
        sys.exit(1)
    
    input_file = sys.argv[1]
    generate_update_statements(input_file)
