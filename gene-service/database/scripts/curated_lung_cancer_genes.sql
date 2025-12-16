-- Curated Lung Cancer Gene Data
-- Data sourced from: cBioPortal, COSMIC, OncoKB, Clinical Literature
-- Last updated: December 2025
-- 
-- This script updates existing genes with comprehensive clinical information
-- for lung cancer. Use this to enrich genes extracted from your mutations table.

-- Update strategy: ON CONFLICT DO UPDATE to preserve existing genes but enrich data

-- Top frequently mutated genes in lung cancer with complete information

INSERT INTO genes (name, description, normal_function, mutation_effect, prevalence, therapies, research_links) VALUES

-- Driver genes with targeted therapies
('EGFR', 'Epidermal Growth Factor Receptor', 
 'Cell surface receptor for epidermal growth factor family, regulates cell proliferation', 
 'Overexpression or mutation leads to uncontrolled cell growth', 
 '10-30% of lung adenocarcinomas, especially in non-smokers and East Asians', 
 'Erlotinib, Gefitinib, Osimertinib, Afatinib', 
 'https://www.ncbi.nlm.nih.gov/gene/1956'),

('KRAS', 'Kirsten Rat Sarcoma Viral Oncogene', 
 'GTPase involved in cellular signaling pathways controlling cell division', 
 'Oncogenic mutations promote continuous growth signaling', 
 '25-30% of lung adenocarcinomas, strongly associated with smoking', 
 'Sotorasib (KRAS G12C inhibitor), AMG 510', 
 'https://www.ncbi.nlm.nih.gov/gene/3845'),

('ALK', 'Anaplastic Lymphoma Kinase', 
 'Receptor tyrosine kinase involved in nervous system development', 
 'Chromosomal rearrangements create oncogenic fusion proteins', 
 '3-7% of lung adenocarcinomas, more common in younger patients', 
 'Crizotinib, Alectinib, Ceritinib, Brigatinib', 
 'https://www.ncbi.nlm.nih.gov/gene/238'),

('TP53', 'Tumor Protein p53', 
 'Tumor suppressor, DNA damage response, cell cycle control', 
 'Loss of function removes cell cycle checkpoints and apoptosis', 
 '50-70% of lung cancers, higher in smoking-related cancers', 
 'No direct therapies; research into p53 reactivation ongoing', 
 'https://www.ncbi.nlm.nih.gov/gene/7157'),

('ROS1', 'ROS Proto-Oncogene 1', 
 'Receptor tyrosine kinase involved in cellular differentiation', 
 'Gene fusions create constitutively active kinase', 
 '1-2% of lung adenocarcinomas', 
 'Crizotinib, Entrectinib, Ceritinib', 
 'https://www.ncbi.nlm.nih.gov/gene/6098'),

('BRAF', 'B-Raf Proto-Oncogene', 
 'Serine/threonine kinase in the MAPK/ERK signaling pathway', 
 'Mutations lead to constitutive activation of growth signaling', 
 '1-3% of lung adenocarcinomas', 
 'Dabrafenib + Trametinib combination', 
 'https://www.ncbi.nlm.nih.gov/gene/673'),

('MET', 'MET Proto-Oncogene', 
 'Receptor tyrosine kinase for hepatocyte growth factor', 
 'Amplification or mutations promote invasion and metastasis', 
 '1-5% amplification, 3% exon 14 skipping mutations', 
 'Capmatinib, Tepotinib (for MET exon 14 skipping)', 
 'https://www.ncbi.nlm.nih.gov/gene/4233'),

('RET', 'Rearranged during Transfection', 
 'Receptor tyrosine kinase important for neural development', 
 'Gene fusions create oncogenic driver', 
 '1-2% of lung adenocarcinomas', 
 'Selpercatinib, Pralsetinib', 
 'https://www.ncbi.nlm.nih.gov/gene/5979'),

('NTRK1', 'Neurotrophic Receptor Tyrosine Kinase 1', 
 'Receptor for nerve growth factor, involved in neural development', 
 'Gene fusions create constitutively active kinase', 
 '<1% of lung cancers', 
 'Larotrectinib, Entrectinib', 
 'https://www.ncbi.nlm.nih.gov/gene/4914'),

('NTRK2', 'Neurotrophic Receptor Tyrosine Kinase 2', 
 'Receptor for BDNF, involved in neuronal survival and differentiation', 
 'Gene fusions lead to constitutive kinase activation', 
 '<1% of lung cancers', 
 'Larotrectinib, Entrectinib', 
 'https://www.ncbi.nlm.nih.gov/gene/4915'),

('NTRK3', 'Neurotrophic Receptor Tyrosine Kinase 3', 
 'Receptor for neurotrophin-3, regulates neuron development', 
 'Gene fusions cause uncontrolled kinase signaling', 
 '<1% of lung cancers', 
 'Larotrectinib, Entrectinib', 
 'https://www.ncbi.nlm.nih.gov/gene/4916'),

-- Tumor suppressors
('PIK3CA', 'Phosphatidylinositol-4,5-Bisphosphate 3-Kinase Catalytic Subunit Alpha', 
 'Key enzyme in PI3K/AKT/mTOR pathway regulating cell survival', 
 'Mutations lead to increased cell survival and proliferation', 
 '2-4% of lung adenocarcinomas', 
 'PI3K inhibitors in clinical trials', 
 'https://www.ncbi.nlm.nih.gov/gene/5290'),

('STK11', 'Serine/Threonine Kinase 11 (LKB1)', 
 'Tumor suppressor regulating cell metabolism and growth', 
 'Loss of function leads to metabolic reprogramming and tumor growth', 
 '15-30% of lung adenocarcinomas, often co-occurs with KRAS', 
 'No direct therapies; metabolic targeting under investigation', 
 'https://www.ncbi.nlm.nih.gov/gene/6794'),

('KEAP1', 'Kelch Like ECH Associated Protein 1', 
 'Regulates NRF2-mediated oxidative stress response', 
 'Loss of function leads to increased antioxidant response and drug resistance', 
 '10-20% of lung adenocarcinomas, often with KRAS/STK11', 
 'No direct therapies; combination strategies under study', 
 'https://www.ncbi.nlm.nih.gov/gene/9817'),

('PTEN', 'Phosphatase and Tensin Homolog', 
 'Tumor suppressor, negative regulator of PI3K/AKT pathway', 
 'Loss of function leads to increased cell survival and proliferation', 
 '4-8% of lung cancers', 
 'PI3K/AKT/mTOR inhibitors', 
 'https://www.ncbi.nlm.nih.gov/gene/5728'),

('RB1', 'RB Transcriptional Corepressor 1', 
 'Tumor suppressor, regulates cell cycle progression', 
 'Loss of function removes G1/S checkpoint control', 
 '5-10% of lung cancers, more common in small cell lung cancer', 
 'CDK4/6 inhibitors (in trials)', 
 'https://www.ncbi.nlm.nih.gov/gene/5925'),

('CDKN2A', 'Cyclin Dependent Kinase Inhibitor 2A (p16)', 
 'Tumor suppressor, inhibits CDK4/6 to regulate cell cycle', 
 'Loss of function allows uncontrolled cell cycle progression', 
 '15-20% of lung adenocarcinomas', 
 'CDK4/6 inhibitors under investigation', 
 'https://www.ncbi.nlm.nih.gov/gene/1029'),

('NF1', 'Neurofibromin 1', 
 'Tumor suppressor, negative regulator of RAS signaling', 
 'Loss of function leads to increased RAS pathway activity', 
 '5-15% of lung adenocarcinomas', 
 'MEK inhibitors, combination therapies', 
 'https://www.ncbi.nlm.nih.gov/gene/4763'),

-- Growth factor receptors
('ERBB2', 'Erb-B2 Receptor Tyrosine Kinase 2 (HER2)', 
 'Growth factor receptor involved in cell proliferation', 
 'Amplification/overexpression drives tumor growth', 
 '2-5% of lung adenocarcinomas', 
 'Trastuzumab, Ado-trastuzumab emtansine', 
 'https://www.ncbi.nlm.nih.gov/gene/2064'),

('FGFR1', 'Fibroblast Growth Factor Receptor 1', 
 'Receptor tyrosine kinase regulating cell proliferation and differentiation', 
 'Amplification leads to enhanced growth signaling', 
 '15-20% of squamous cell lung carcinomas', 
 'FGFR inhibitors in clinical trials', 
 'https://www.ncbi.nlm.nih.gov/gene/2260'),

('FGFR2', 'Fibroblast Growth Factor Receptor 2', 
 'Receptor for FGF ligands, regulates cell growth and differentiation', 
 'Mutations and fusions lead to constitutive activation', 
 '1-3% of lung cancers', 
 'FGFR inhibitors (Erdafitinib, others)', 
 'https://www.ncbi.nlm.nih.gov/gene/2263'),

('FGFR3', 'Fibroblast Growth Factor Receptor 3', 
 'Receptor tyrosine kinase involved in cell growth regulation', 
 'Mutations lead to increased kinase activity', 
 '1-3% of lung cancers', 
 'FGFR inhibitors', 
 'https://www.ncbi.nlm.nih.gov/gene/2261'),

-- DNA damage response and repair
('ATM', 'ATM Serine/Threonine Kinase', 
 'DNA damage checkpoint kinase, coordinates DNA repair', 
 'Loss of function impairs DNA damage response', 
 '5-10% of lung cancers', 
 'PARP inhibitors, platinum chemotherapy', 
 'https://www.ncbi.nlm.nih.gov/gene/472'),

('BRCA1', 'BRCA1 DNA Repair Associated', 
 'DNA repair through homologous recombination', 
 'Loss of function leads to genomic instability', 
 '1-2% of lung cancers', 
 'PARP inhibitors, platinum chemotherapy', 
 'https://www.ncbi.nlm.nih.gov/gene/672'),

('BRCA2', 'BRCA2 DNA Repair Associated', 
 'DNA repair through homologous recombination', 
 'Loss of function impairs DNA repair leading to mutations', 
 '1-2% of lung cancers', 
 'PARP inhibitors, platinum chemotherapy', 
 'https://www.ncbi.nlm.nih.gov/gene/675'),

-- Other important genes
('MDM2', 'MDM2 Proto-Oncogene', 
 'E3 ubiquitin ligase that regulates p53', 
 'Amplification inhibits p53 tumor suppressor function', 
 '5-10% of lung cancers', 
 'MDM2 inhibitors in clinical trials', 
 'https://www.ncbi.nlm.nih.gov/gene/4193'),

('MYC', 'MYC Proto-Oncogene', 
 'Transcription factor regulating cell proliferation and metabolism', 
 'Amplification drives excessive cell growth', 
 '8-10% amplification in lung cancers', 
 'No direct inhibitors; indirect targeting strategies', 
 'https://www.ncbi.nlm.nih.gov/gene/4609'),

('APC', 'Adenomatous Polyposis Coli', 
 'Tumor suppressor in WNT signaling pathway', 
 'Loss of function leads to WNT pathway activation', 
 '5-10% of lung cancers', 
 'WNT pathway inhibitors under investigation', 
 'https://www.ncbi.nlm.nih.gov/gene/324'),

('SMAD4', 'SMAD Family Member 4', 
 'Tumor suppressor in TGF-beta signaling', 
 'Loss of function disrupts growth inhibitory signals', 
 '3-5% of lung cancers', 
 'No direct therapies', 
 'https://www.ncbi.nlm.nih.gov/gene/4089'),

('NOTCH1', 'Notch Receptor 1', 
 'Cell surface receptor regulating cell fate decisions', 
 'Mutations can be oncogenic or tumor suppressive depending on context', 
 '5-10% of lung cancers', 
 'Gamma-secretase inhibitors (context dependent)', 
 'https://www.ncbi.nlm.nih.gov/gene/4851')

ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    normal_function = EXCLUDED.normal_function,
    mutation_effect = EXCLUDED.mutation_effect,
    prevalence = EXCLUDED.prevalence,
    therapies = EXCLUDED.therapies,
    research_links = EXCLUDED.research_links;

-- Verify the updates
SELECT 
    COUNT(*) FILTER (WHERE description != 'Gene associated with cancer mutations') as enriched_genes,
    COUNT(*) FILTER (WHERE description = 'Gene associated with cancer mutations') as basic_genes,
    COUNT(*) as total_genes
FROM genes;

-- Show sample of enriched genes
SELECT name, description, prevalence, therapies
FROM genes
WHERE description != 'Gene associated with cancer mutations'
ORDER BY name
LIMIT 10;
