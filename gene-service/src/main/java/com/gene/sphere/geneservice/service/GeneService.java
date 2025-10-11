package com.gene.sphere.geneservice.service;

import com.gene.sphere.geneservice.factory.GeneFactory;
import com.gene.sphere.geneservice.model.Gene;
import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.repository.GeneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer encapsulating business logic for Gene operations.
 * - Uses Repository for persistence
 * - Uses Factory for mapping Entity <-> DTO
 */
@Service
public class GeneService implements GeneServiceInterface {
    private final GeneRepository geneRepository;
    private final GeneFactory factory;

    public GeneService(GeneRepository repo, GeneFactory factory) {
        this.geneRepository = repo;
        this.factory = factory;
    }

    /**
     * Get gene by name (case insensitive exact match).
     *
     * @param name gene symbol (exact match)
     * @return API DTO wrapped in Optional
     */
    public Optional<GeneRecord> getGeneByName(String name) {
        return geneRepository.findByNameIgnoreCase(name).map(factory::toDto);
    }

    /**
     * Get all genes from DB.
     */
    public List<GeneRecord> getAllGenes() {
        return geneRepository.findAll().stream().map(factory::toDto).toList();
    }

    /**
     * Create and save a new Gene entry.
     */
    public GeneRecord createGene(GeneRecord dto) {
        Gene g = factory.fromDto(dto);
        return factory.toDto(geneRepository.save(g));
    }

    /**
     * Update an existing gene (if present).
     * Throws NoSuchElementException if not found.
     */
    public GeneRecord updateGene(Integer id, GeneRecord dto) {
        Gene g = geneRepository.findById(id).orElseThrow();
        g.setDescription(dto.description());
        g.setNormalFunction(dto.normalFunction());
        g.setMutationEffect(dto.mutationEffect());
        g.setPrevalence(dto.prevalence());
        g.setTherapies(dto.therapies());
        g.setResearchLinks(dto.researchLinks());
        return factory.toDto(geneRepository.save(g));
    }

    /**
     * Delete a gene by ID.
     */
    public void deleteGene(Integer id) {
        geneRepository.deleteById(id);
    }

    /**
     * Search for genes containing the given substring (case insensitive).
     * Use this for search functionality where multiple results are expected.
     *
     * @param partOfName substring to search for in gene names
     * @return list of matching genes
     */
    public List<GeneRecord> searchGenesByName(String partOfName) {
        return geneRepository.findAllByNameContainingIgnoreCase(partOfName)
                .stream()
                .map(factory::toDto)
                .toList();
    }
}