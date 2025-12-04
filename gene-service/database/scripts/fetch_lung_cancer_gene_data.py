#!/usr/bin/env python3
"""
Fetch lung cancer gene data from public APIs to populate the genes table.
This script queries cBioPortal and other public databases to get comprehensive gene information.

Requirements:
    pip install requests pandas

Usage:
    python fetch_lung_cancer_gene_data.py > enriched_genes.sql
"""

import requests
import json
from typing import Dict, List, Optional
from urllib.parse import quote

class GeneDataFetcher:
    """Fetches gene data from multiple public cancer databases."""
    
    def __init__(self):
        self.cbioportal_base = "https://www.cbioportal.org/api"
        self.ncbi_gene_base = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"
        self.session = requests.Session()
        
    def get_lung_cancer_genes_from_cbioportal(self, study_id: str = "luad_tcga_pan_can_atlas_2018") -> List[Dict]:
        """
        Fetch frequently mutated genes from a lung cancer study in cBioPortal.
        
        Args:
            study_id: cBioPortal study identifier (default: TCGA Lung Adenocarcinoma)
        
        Returns:
            List of gene dictionaries with mutation frequencies
        """
        try:
            # Get molecular profile ID for mutations
            url = f"{self.cbioportal_base}/molecular-profiles"
            params = {"studyId": study_id}
            response = self.session.get(url, params=params)
            response.raise_for_status()
            
            profiles = response.json()
            mutation_profile = next((p for p in profiles if p.get('molecularAlterationType') == 'MUTATION_EXTENDED'), None)
            
            if not mutation_profile:
                print(f"-- Warning: No mutation profile found for study {study_id}")
                return []
            
            # Get mutated genes
            url = f"{self.cbioportal_base}/molecular-profiles/{mutation_profile['molecularProfileId']}/mutated-genes"
            response = self.session.get(url)
            response.raise_for_status()
            
            return response.json()
        except Exception as e:
            print(f"-- Error fetching from cBioPortal: {e}")
            return []
    
    def get_ncbi_gene_info(self, gene_symbol: str) -> Optional[Dict]:
        """
        Fetch gene information from NCBI Gene database.
        
        Args:
            gene_symbol: Gene symbol (e.g., 'TP53')
        
        Returns:
            Dictionary with gene information or None
        """
        try:
            # Search for gene ID
            search_url = f"{self.ncbi_gene_base}/esearch.fcgi"
            search_params = {
                "db": "gene",
                "term": f"{gene_symbol}[Gene Name] AND Homo sapiens[Organism]",
                "retmode": "json"
            }
            response = self.session.get(search_url, params=search_params)
            response.raise_for_status()
            search_data = response.json()
            
            if not search_data.get('esearchresult', {}).get('idlist'):
                return None
            
            gene_id = search_data['esearchresult']['idlist'][0]
            
            # Fetch gene summary
            summary_url = f"{self.ncbi_gene_base}/esummary.fcgi"
            summary_params = {
                "db": "gene",
                "id": gene_id,
                "retmode": "json"
            }
            response = self.session.get(summary_url, params=summary_params)
            response.raise_for_status()
            summary_data = response.json()
            
            result = summary_data.get('result', {}).get(gene_id, {})
            return {
                "gene_id": gene_id,
                "symbol": result.get('name', gene_symbol),
                "description": result.get('description', ''),
                "summary": result.get('summary', ''),
                "chromosome": result.get('chromosome', ''),
            }
        except Exception as e:
            print(f"-- Error fetching NCBI data for {gene_symbol}: {e}")
            return None
    
    def generate_sql_insert(self, gene_symbol: str, mutation_data: Dict, ncbi_data: Optional[Dict] = None) -> str:
        """
        Generate SQL INSERT statement for a gene.
        
        Args:
            gene_symbol: Gene symbol
            mutation_data: Mutation frequency data from cBioPortal
            ncbi_data: Gene information from NCBI
        
        Returns:
            SQL INSERT statement
        """
        # Extract data
        description = ""
        normal_function = ""
        
        if ncbi_data:
            description = ncbi_data.get('description', '')
            summary = ncbi_data.get('summary', '')
            if summary and len(summary) < 2000:
                normal_function = summary[:500] + "..." if len(summary) > 500 else summary
        
        if not description:
            description = f"Gene frequently mutated in lung cancer"
        
        if not normal_function:
            normal_function = "Function varies - see research links for details"
        
        # Calculate prevalence from mutation data
        frequency = mutation_data.get('qValue', 0)
        num_mutations = mutation_data.get('numberOfMutations', 0)
        num_samples = mutation_data.get('numberOfSamples', 0)
        
        prevalence = f"Mutated in {num_samples} samples"
        if frequency:
            prevalence += f" (q-value: {frequency:.4f})"
        
        mutation_effect = f"{num_mutations} mutations documented in lung cancer studies"
        
        # Therapy info (you'll need to manually curate or use OncoKB API)
        therapies = "Therapies depend on specific mutations and cancer type"
        
        research_links = f"https://www.ncbi.nlm.nih.gov/gene/{ncbi_data.get('gene_id', '?term=' + gene_symbol) if ncbi_data else '?term=' + gene_symbol}"
        
        # Escape single quotes for SQL
        def escape_sql(text):
            return text.replace("'", "''") if text else ""
        
        sql = f"""
INSERT INTO genes (name, description, normal_function, mutation_effect, prevalence, therapies, research_links)
VALUES (
    '{escape_sql(gene_symbol)}',
    '{escape_sql(description[:2000])}',
    '{escape_sql(normal_function[:500])}',
    '{escape_sql(mutation_effect)}',
    '{escape_sql(prevalence)}',
    '{escape_sql(therapies)}',
    '{escape_sql(research_links)}'
)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    normal_function = EXCLUDED.normal_function,
    mutation_effect = EXCLUDED.mutation_effect,
    prevalence = EXCLUDED.prevalence,
    research_links = EXCLUDED.research_links;
"""
        return sql

def main():
    """Main execution function."""
    print("-- Lung Cancer Gene Data Population Script")
    print("-- Generated using cBioPortal and NCBI Gene APIs")
    print("-- Run this script to update gene information\n")
    
    fetcher = GeneDataFetcher()
    
    # Fetch data from cBioPortal
    print("-- Fetching data from cBioPortal...")
    lung_genes = fetcher.get_lung_cancer_genes_from_cbioportal()
    
    if not lung_genes:
        print("-- Warning: No data fetched from cBioPortal")
        print("-- Falling back to common lung cancer genes list")
        # Fallback to known common lung cancer genes
        lung_genes = [
            {"entrezGeneId": 1956, "hugoGeneSymbol": "EGFR"},
            {"entrezGeneId": 3845, "hugoGeneSymbol": "KRAS"},
            {"entrezGeneId": 7157, "hugoGeneSymbol": "TP53"},
            {"entrezGeneId": 238, "hugoGeneSymbol": "ALK"},
            {"entrezGeneId": 5979, "hugoGeneSymbol": "RET"},
        ]
    
    print(f"-- Found {len(lung_genes)} genes\n")
    
    # Process top 50 most significant genes
    for i, gene_data in enumerate(lung_genes[:50], 1):
        gene_symbol = gene_data.get('hugoGeneSymbol') or gene_data.get('geneSymbol')
        
        if not gene_symbol:
            continue
        
        print(f"-- Processing {gene_symbol} ({i}/50)")
        
        # Fetch NCBI data
        ncbi_data = fetcher.get_ncbi_gene_info(gene_symbol)
        
        # Generate SQL
        sql = fetcher.generate_sql_insert(gene_symbol, gene_data, ncbi_data)
        print(sql)
    
    print("\n-- Script completed!")
    print("-- To apply these updates, run: python fetch_lung_cancer_gene_data.py > update_genes.sql")
    print("-- Then execute: psql -U your_user -d your_database -f update_genes.sql")

if __name__ == "__main__":
    main()
