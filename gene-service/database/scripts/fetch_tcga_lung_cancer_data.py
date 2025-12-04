#!/usr/bin/env python3
"""
Fetch real mutation data from cBioPortal TCGA Lung Cancer studies
and populate the genes table with accurate prevalence and mutation data.

This script fetches REAL data from:
- TCGA Lung Adenocarcinoma (LUAD) - 230 samples
- TCGA Lung Squamous Cell Carcinoma (LUSC) - 178 samples

Requirements:
    pip install requests

Usage:
    python fetch_tcga_lung_cancer_data.py
"""

import requests
import json
import sys
from typing import Dict, List, Optional
from collections import defaultdict

class TCGALungCancerFetcher:
    """Fetch real mutation data from cBioPortal for lung cancer."""
    
    def __init__(self):
        self.base_url = "https://www.cbioportal.org/api"
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json'
        })
        
        # TCGA Lung Cancer Studies
        self.studies = {
            'luad': 'luad_tcga_pan_can_atlas_2018',  # Lung Adenocarcinoma
            'lusc': 'lusc_tcga_pan_can_atlas_2018',  # Lung Squamous Cell Carcinoma
        }
        
        self.gene_data = {}
        self.therapy_map = self._load_therapy_map()
        
    def _load_therapy_map(self) -> Dict[str, str]:
        """Known therapies for lung cancer genes."""
        return {
            'EGFR': 'Erlotinib, Gefitinib, Osimertinib, Afatinib',
            'KRAS': 'Sotorasib (G12C), Adagrasib (G12C)',
            'ALK': 'Crizotinib, Alectinib, Ceritinib, Brigatinib',
            'ROS1': 'Crizotinib, Entrectinib, Ceritinib',
            'BRAF': 'Dabrafenib + Trametinib',
            'MET': 'Capmatinib, Tepotinib (exon 14 skipping)',
            'RET': 'Selpercatinib, Pralsetinib',
            'ERBB2': 'Trastuzumab, Ado-trastuzumab emtansine',
            'NTRK1': 'Larotrectinib, Entrectinib',
            'NTRK2': 'Larotrectinib, Entrectinib',
            'NTRK3': 'Larotrectinib, Entrectinib',
            'PIK3CA': 'PI3K inhibitors in clinical trials',
            'FGFR1': 'FGFR inhibitors in clinical trials',
            'FGFR2': 'Erdafitinib, Pemigatinib',
            'FGFR3': 'Erdafitinib',
            'STK11': 'No direct therapies; metabolic targeting under investigation',
            'KEAP1': 'No direct therapies; combination strategies under study',
            'PTEN': 'PI3K/AKT/mTOR inhibitors',
            'TP53': 'No direct therapies; p53 reactivation in trials',
            'RB1': 'CDK4/6 inhibitors in trials',
            'CDKN2A': 'CDK4/6 inhibitors under investigation',
            'NF1': 'MEK inhibitors, combination therapies',
            'ATM': 'PARP inhibitors, platinum chemotherapy',
            'BRCA1': 'PARP inhibitors, platinum chemotherapy',
            'BRCA2': 'PARP inhibitors, platinum chemotherapy',
        }
    
    def fetch_mutated_genes(self, study_id: str) -> List[Dict]:
        """Fetch mutated genes from a specific study."""
        try:
            print(f"  Fetching data from {study_id}...", file=sys.stderr)
            
            # Use the correct endpoint: studies/{studyId}/mutations with gene aggregation
            # First, get all mutations for the study
            url = f"{self.base_url}/studies/{study_id}/mutations"
            params = {
                "projection": "SUMMARY",
                "pageSize": 100000,
                "pageNumber": 0
            }
            response = self.session.get(url, params=params)
            response.raise_for_status()
            
            mutations = response.json()
            
            # Aggregate by gene
            gene_stats = defaultdict(lambda: {
                'samples': set(),
                'mutation_count': 0,
                'entrez_id': None,
                'gene_symbol': None
            })
            
            for mut in mutations:
                gene_symbol = mut.get('gene', {}).get('hugoGeneSymbol')
                if not gene_symbol:
                    continue
                
                entrez_id = mut.get('gene', {}).get('entrezGeneId')
                sample_id = mut.get('sampleId')
                
                gene_stats[gene_symbol]['gene_symbol'] = gene_symbol
                gene_stats[gene_symbol]['entrez_id'] = entrez_id
                gene_stats[gene_symbol]['mutation_count'] += 1
                if sample_id:
                    gene_stats[gene_symbol]['samples'].add(sample_id)
            
            # Convert to list format
            genes = []
            for gene_symbol, stats in gene_stats.items():
                genes.append({
                    'hugoGeneSymbol': gene_symbol,
                    'entrezGeneId': stats['entrez_id'],
                    'numberOfMutations': len(stats['samples']),  # Number of samples
                    'totalMutations': stats['mutation_count']
                })
            
            print(f"  ✓ Found {len(genes)} mutated genes with {len(mutations)} total mutations", file=sys.stderr)
            
            return genes
            
        except Exception as e:
            print(f"  ❌ Error fetching from {study_id}: {e}", file=sys.stderr)
            import traceback
            traceback.print_exc(file=sys.stderr)
            return []
    
    def aggregate_gene_data(self):
        """Fetch and aggregate data from all lung cancer studies."""
        print("\n🔬 Fetching real mutation data from TCGA Lung Cancer studies...\n", file=sys.stderr)
        
        all_genes = defaultdict(lambda: {
            'samples_mutated': 0,
            'total_mutations': 0,
            'studies': [],
            'entrez_id': None,
            'gene_symbol': None
        })
        
        total_samples = 0
        
        for study_name, study_id in self.studies.items():
            genes = self.fetch_mutated_genes(study_id)
            
            # Get study info for sample count
            try:
                url = f"{self.base_url}/studies/{study_id}"
                response = self.session.get(url)
                response.raise_for_status()
                study_info = response.json()
                study_samples = study_info.get('allSampleCount', 0)
                total_samples += study_samples
                print(f"  Study samples: {study_samples}\n", file=sys.stderr)
            except:
                study_samples = 0
            
            for gene in genes:
                gene_symbol = gene.get('hugoGeneSymbol')
                if not gene_symbol:
                    continue
                
                all_genes[gene_symbol]['samples_mutated'] += gene.get('numberOfMutations', 0)
                all_genes[gene_symbol]['total_mutations'] += gene.get('numberOfMutations', 0)
                all_genes[gene_symbol]['studies'].append(study_name)
                all_genes[gene_symbol]['entrez_id'] = gene.get('entrezGeneId')
                all_genes[gene_symbol]['gene_symbol'] = gene_symbol
        
        self.gene_data = all_genes
        self.total_samples = total_samples
        
        print(f"✓ Aggregated data for {len(all_genes)} unique genes", file=sys.stderr)
        print(f"✓ Total samples across studies: {total_samples}\n", file=sys.stderr)
    
    def generate_sql(self):
        """Generate SQL UPDATE statements with real data."""
        
        print("-- Real TCGA Lung Cancer Gene Data")
        print("-- Data source: cBioPortal TCGA PanCancer Atlas")
        print("-- Studies: Lung Adenocarcinoma (LUAD) + Lung Squamous Cell Carcinoma (LUSC)")
        print(f"-- Total samples: {self.total_samples}")
        print("-- Generated: December 2025")
        print("--")
        print("-- This updates genes table with REAL mutation frequencies from TCGA")
        print()
        
        # Sort genes by mutation frequency
        sorted_genes = sorted(
            self.gene_data.items(),
            key=lambda x: x[1]['samples_mutated'],
            reverse=True
        )
        
        # Generate updates for top genes
        count = 0
        for gene_symbol, data in sorted_genes[:100]:  # Top 100 genes
            count += 1
            
            samples_mutated = data['samples_mutated']
            total_mutations = data['total_mutations']
            entrez_id = data['entrez_id']
            
            # Calculate percentage
            percentage = (samples_mutated / self.total_samples * 100) if self.total_samples > 0 else 0
            
            # Get therapy info
            therapies = self.therapy_map.get(gene_symbol, 'Therapies depend on specific mutations and cancer type')
            
            # Determine if it's a known driver or tumor suppressor
            description = self._get_gene_description(gene_symbol)
            normal_function = self._get_gene_function(gene_symbol)
            mutation_effect = f"Mutated in {samples_mutated} of {self.total_samples} lung cancer samples ({percentage:.1f}%)"
            
            prevalence = f"{percentage:.1f}% of TCGA lung cancer samples (LUAD + LUSC)"
            
            research_link = f"https://www.ncbi.nlm.nih.gov/gene/{entrez_id}" if entrez_id else f"https://www.ncbi.nlm.nih.gov/gene/?term={gene_symbol}"
            
            # Escape single quotes
            def escape_sql(text):
                return text.replace("'", "''") if text else ""
            
            print(f"-- {count}. {gene_symbol} ({percentage:.1f}% of samples)")
            print("UPDATE genes SET")
            print(f"    description = '{escape_sql(description)}',")
            print(f"    normal_function = '{escape_sql(normal_function)}',")
            print(f"    mutation_effect = '{escape_sql(mutation_effect)}',")
            print(f"    prevalence = '{escape_sql(prevalence)}',")
            print(f"    therapies = '{escape_sql(therapies)}',")
            print(f"    research_links = '{escape_sql(research_link)}'")
            print(f"WHERE name = '{gene_symbol}';")
            print()
        
        print("-- Summary statistics")
        print(f"SELECT COUNT(*) as updated_genes FROM genes WHERE prevalence LIKE '%TCGA lung cancer%';")
        print()
        print(f"-- Verification query")
        print("SELECT name, prevalence, mutation_effect FROM genes WHERE prevalence LIKE '%TCGA%' ORDER BY name LIMIT 10;")
    
    def _get_gene_description(self, gene_symbol: str) -> str:
        """Get description for known cancer genes."""
        descriptions = {
            'TP53': 'Tumor Protein p53 - Guardian of the Genome',
            'KRAS': 'Kirsten Rat Sarcoma Viral Oncogene',
            'EGFR': 'Epidermal Growth Factor Receptor',
            'ALK': 'Anaplastic Lymphoma Kinase',
            'BRAF': 'B-Raf Proto-Oncogene',
            'PIK3CA': 'Phosphatidylinositol-4,5-Bisphosphate 3-Kinase Catalytic Subunit Alpha',
            'STK11': 'Serine/Threonine Kinase 11 (LKB1)',
            'KEAP1': 'Kelch Like ECH Associated Protein 1',
            'ROS1': 'ROS Proto-Oncogene 1',
            'RET': 'Rearranged during Transfection',
            'MET': 'MET Proto-Oncogene',
            'ERBB2': 'Erb-B2 Receptor Tyrosine Kinase 2 (HER2)',
            'PTEN': 'Phosphatase and Tensin Homolog',
            'NF1': 'Neurofibromin 1',
            'RB1': 'RB Transcriptional Corepressor 1',
        }
        return descriptions.get(gene_symbol, f'{gene_symbol} - Gene frequently mutated in lung cancer')
    
    def _get_gene_function(self, gene_symbol: str) -> str:
        """Get normal function for known cancer genes."""
        functions = {
            'TP53': 'Tumor suppressor, DNA damage response, cell cycle control, apoptosis',
            'KRAS': 'GTPase regulating cell proliferation, differentiation, and survival',
            'EGFR': 'Cell surface receptor regulating cell proliferation and survival',
            'ALK': 'Receptor tyrosine kinase involved in nervous system development',
            'BRAF': 'Serine/threonine kinase in MAPK/ERK signaling pathway',
            'PIK3CA': 'Catalytic subunit of PI3K, regulates cell growth and survival',
            'STK11': 'Tumor suppressor regulating cell metabolism and polarity',
            'KEAP1': 'Regulates NRF2-mediated oxidative stress response',
            'ROS1': 'Receptor tyrosine kinase involved in cellular differentiation',
            'RET': 'Receptor tyrosine kinase important for neural development',
            'MET': 'Receptor tyrosine kinase for hepatocyte growth factor',
            'ERBB2': 'Growth factor receptor involved in cell proliferation',
            'PTEN': 'Tumor suppressor, negative regulator of PI3K/AKT pathway',
            'NF1': 'Tumor suppressor, negative regulator of RAS signaling',
            'RB1': 'Tumor suppressor, regulates cell cycle progression',
        }
        return functions.get(gene_symbol, 'Function varies by gene - see research links for details')

def main():
    """Main execution."""
    try:
        fetcher = TCGALungCancerFetcher()
        fetcher.aggregate_gene_data()
        fetcher.generate_sql()
        
        print("\n-- ✓ SQL generation complete!", file=sys.stderr)
        print("-- To apply: python fetch_tcga_lung_cancer_data.py > tcga_update.sql", file=sys.stderr)
        print("--           psql -U your_user -d your_db -f tcga_update.sql", file=sys.stderr)
        
    except KeyboardInterrupt:
        print("\n\n-- Interrupted by user", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"\n-- ❌ Error: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
