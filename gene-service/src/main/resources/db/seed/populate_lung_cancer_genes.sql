-- Lung Cancer Gene Data Population Script
-- Insert key lung cancer genes with real clinical data

INSERT INTO genes (name, description, normal_function, mutation_effect, prevalence, therapies, research_links) VALUES
('EGFR', 'Epidermal Growth Factor Receptor', 'Cell surface receptor for epidermal growth factor family, regulates cell proliferation', 'Overexpression or mutation leads to uncontrolled cell growth', '10-30% of lung adenocarcinomas, especially in non-smokers and East Asians', 'Erlotinib, Gefitinib, Osimertinib, Afatinib', 'https://www.ncbi.nlm.nih.gov/gene/1956'),

('KRAS', 'Kirsten Rat Sarcoma Viral Oncogene', 'GTPase involved in cellular signaling pathways controlling cell division', 'Oncogenic mutations promote continuous growth signaling', '25-30% of lung adenocarcinomas, strongly associated with smoking', 'Sotorasib (KRAS G12C inhibitor), AMG 510', 'https://www.ncbi.nlm.nih.gov/gene/3845'),

('ALK', 'Anaplastic Lymphoma Kinase', 'Receptor tyrosine kinase involved in nervous system development', 'Chromosomal rearrangements create oncogenic fusion proteins', '3-7% of lung adenocarcinomas, more common in younger patients', 'Crizotinib, Alectinib, Ceritinib, Brigatinib', 'https://www.ncbi.nlm.nih.gov/gene/238'),

('TP53', 'Tumor Protein p53', 'Tumor suppressor, DNA damage response, cell cycle control', 'Loss of function removes cell cycle checkpoints and apoptosis', '50-70% of lung cancers, higher in smoking-related cancers', 'No direct therapies; research into p53 reactivation ongoing', 'https://www.ncbi.nlm.nih.gov/gene/7157'),

('ROS1', 'ROS Proto-Oncogene 1', 'Receptor tyrosine kinase involved in cellular differentiation', 'Gene fusions create constitutively active kinase', '1-2% of lung adenocarcinomas', 'Crizotinib, Entrectinib, Ceritinib', 'https://www.ncbi.nlm.nih.gov/gene/6098'),

('BRAF', 'B-Raf Proto-Oncogene', 'Serine/threonine kinase in the MAPK/ERK signaling pathway', 'Mutations lead to constitutive activation of growth signaling', '1-3% of lung adenocarcinomas', 'Dabrafenib + Trametinib combination', 'https://www.ncbi.nlm.nih.gov/gene/673'),

('MET', 'MET Proto-Oncogene', 'Receptor tyrosine kinase for hepatocyte growth factor', 'Amplification or mutations promote invasion and metastasis', '1-5% amplification, 3% exon 14 skipping mutations', 'Capmatinib, Tepotinib (for MET exon 14 skipping)', 'https://www.ncbi.nlm.nih.gov/gene/4233'),

('RET', 'Rearranged during Transfection', 'Receptor tyrosine kinase important for neural development', 'Gene fusions create oncogenic driver', '1-2% of lung adenocarcinomas', 'Selpercatinib, Pralsetinib', 'https://www.ncbi.nlm.nih.gov/gene/5979'),

('NTRK1', 'Neurotrophic Receptor Tyrosine Kinase 1', 'Receptor for nerve growth factor, involved in neural development', 'Gene fusions create constitutively active kinase', '<1% of lung cancers', 'Larotrectinib, Entrectinib', 'https://www.ncbi.nlm.nih.gov/gene/4914'),

('PIK3CA', 'Phosphatidylinositol-4,5-Bisphosphate 3-Kinase Catalytic Subunit Alpha', 'Key enzyme in PI3K/AKT/mTOR pathway regulating cell survival', 'Mutations lead to increased cell survival and proliferation', '2-4% of lung adenocarcinomas', 'PI3K inhibitors in clinical trials', 'https://www.ncbi.nlm.nih.gov/gene/5290'),

('STK11', 'Serine/Threonine Kinase 11 (LKB1)', 'Tumor suppressor regulating cell metabolism and growth', 'Loss of function leads to metabolic reprogramming and tumor growth', '15-30% of lung adenocarcinomas, often co-occurs with KRAS', 'No direct therapies; metabolic targeting under investigation', 'https://www.ncbi.nlm.nih.gov/gene/6794'),

('KEAP1', 'Kelch Like ECH Associated Protein 1', 'Regulates NRF2-mediated oxidative stress response', 'Loss of function leads to increased antioxidant response and drug resistance', '10-20% of lung adenocarcinomas, often with KRAS/STK11', 'No direct therapies; combination strategies under study', 'https://www.ncbi.nlm.nih.gov/gene/9817'),

('ERBB2', 'Erb-B2 Receptor Tyrosine Kinase 2 (HER2)', 'Growth factor receptor involved in cell proliferation', 'Amplification/overexpression drives tumor growth', '2-5% of lung adenocarcinomas', 'Trastuzumab, Ado-trastuzumab emtansine', 'https://www.ncbi.nlm.nih.gov/gene/2064'),

('FGFR1', 'Fibroblast Growth Factor Receptor 1', 'Receptor tyrosine kinase regulating cell proliferation and differentiation', 'Amplification leads to enhanced growth signaling', '15-20% of squamous cell lung carcinomas', 'FGFR inhibitors in clinical trials', 'https://www.ncbi.nlm.nih.gov/gene/2260'),

('NF1', 'Neurofibromin 1', 'Tumor suppressor, negative regulator of RAS signaling', 'Loss of function leads to increased RAS pathway activity', '5-15% of lung adenocarcinomas', 'MEK inhibitors, combination therapies', 'https://www.ncbi.nlm.nih.gov/gene/4763');

-- Verify the data was inserted
SELECT COUNT(*) as total_genes_inserted FROM genes;
SELECT name, description FROM genes ORDER BY name;
