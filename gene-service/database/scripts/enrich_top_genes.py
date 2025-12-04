#!/usr/bin/env python3
"""
Strategy to enrich top mutated genes with real data from cBioPortal and NCBI.
This script identifies the most frequently mutated genes in your database
and fetches detailed information for them.
"""

import psycopg2
import requests
import time
from typing import Dict, Optional

# Database configuration
DB_CONFIG = {
    'host': 'localhost',
    'database': 'gene_db',
    'user': 'gene_user',
    'password': 'gene_pass'
}

class GeneEnricher:
    def __init__(self):
        self.conn = psycopg2.connect(**DB_CONFIG)
        self.cursor = self.conn.cursor()
        
    def get_top_mutated_genes(self, limit=500):
        """Get genes ordered by mutation frequency in your database."""
        query = """
        SELECT g.name, COUNT(m.id) as mutation_count
        FROM genes g
        LEFT JOIN mutations m ON g.name = m.gene_symbol
        WHERE g.description LIKE '%Cancer-associated gene%'
        GROUP BY g.name
        HAVING COUNT(m.id) > 0
        ORDER BY mutation_count DESC
        LIMIT %s;
        """
        self.cursor.execute(query, (limit,))
        return self.cursor.fetchall()
    
    def fetch_ncbi_gene_info(self, gene_symbol: str) -> Optional[Dict]:
        """Fetch gene description from NCBI."""
        try:
            # Search for gene ID
            search_url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi"
            search_params = {
                "db": "gene",
                "term": f"{gene_symbol}[Gene Name] AND Homo sapiens[Organism]",
                "retmode": "json"
            }
            response = requests.get(search_url, params=search_params)
            search_data = response.json()
            
            if not search_data.get('esearchresult', {}).get('idlist'):
                return None
            
            gene_id = search_data['esearchresult']['idlist'][0]
            
            # Fetch gene summary
            summary_url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi"
            summary_params = {
                "db": "gene",
                "id": gene_id,
                "retmode": "json"
            }
            response = requests.get(summary_url, params=summary_params)
            summary_data = response.json()
            
            result = summary_data.get('result', {}).get(gene_id, {})
            return {
                "gene_id": gene_id,
                "description": result.get('description', ''),
                "summary": result.get('summary', '')[:500]  # Limit to 500 chars
            }
        except Exception as e:
            print(f"Error fetching NCBI data for {gene_symbol}: {e}")
            return None
    
    def update_gene(self, gene_name: str, mutation_count: int, ncbi_data: Optional[Dict]):
        """Update a single gene with enriched data."""
        if ncbi_data:
            description = ncbi_data.get('description', f'{gene_name} - Gene with cancer mutations')
            normal_function = ncbi_data.get('summary', 'Function determined by gene family')
            research_link = f"https://www.ncbi.nlm.nih.gov/gene/{ncbi_data.get('gene_id', '')}"
        else:
            description = f'{gene_name} - Frequently mutated in cancer'
            normal_function = 'Gene function varies; see research links'
            research_link = f"https://www.ncbi.nlm.nih.gov/gene/?term={gene_name}"
        
        update_query = """
        UPDATE genes 
        SET 
            description = %s,
            normal_function = %s,
            mutation_effect = %s,
            prevalence = %s,
            research_links = %s
        WHERE name = %s;
        """
        
        self.cursor.execute(update_query, (
            description,
            normal_function,
            f'Mutations documented in cancer studies',
            f'Found in {mutation_count} mutation records in database',
            research_link,
            gene_name
        ))
        self.conn.commit()
    
    def enrich_genes(self, limit=500):
        """Main enrichment process."""
        print(f"Fetching top {limit} mutated genes...")
        top_genes = self.get_top_mutated_genes(limit)
        
        print(f"Found {len(top_genes)} genes to enrich")
        
        for i, (gene_name, mutation_count) in enumerate(top_genes, 1):
            print(f"[{i}/{len(top_genes)}] Processing {gene_name} ({mutation_count} mutations)...")
            
            # Fetch NCBI data
            ncbi_data = self.fetch_ncbi_gene_info(gene_name)
            
            # Update database
            self.update_gene(gene_name, mutation_count, ncbi_data)
            
            # Rate limiting for NCBI (3 requests/second)
            time.sleep(0.35)
            
            if i % 50 == 0:
                print(f"Progress: {i}/{len(top_genes)} genes enriched")
        
        print(f"\n✅ Enrichment complete! {len(top_genes)} genes updated.")
    
    def close(self):
        """Close database connection."""
        self.cursor.close()
        self.conn.close()

if __name__ == "__main__":
    print("=== Gene Database Enrichment Tool ===\n")
    
    enricher = GeneEnricher()
    
    try:
        # Enrich top 500 genes
        enricher.enrich_genes(limit=500)
    finally:
        enricher.close()
    
    print("\nTo check results, run:")
    print("  SELECT COUNT(*) FROM genes WHERE description NOT LIKE '%Cancer-associated gene%';")
