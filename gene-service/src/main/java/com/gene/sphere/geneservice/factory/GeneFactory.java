package com.gene.sphere.geneservice.factory;

import com.gene.sphere.geneservice.model.Gene;
import com.gene.sphere.geneservice.model.GeneRecord;
import org.springframework.stereotype.Component;

/**
 * Factory (Mapper) component responsible for converting between
 * the internal JPA {@link Gene} entity
 * and the external API-facing {@link GeneRecord} DTO.
 *
 * <p>This enforces separation of concerns:
 * <ul>
 *   <li><strong>Gene (Entity)</strong>: represents the database row with persistence annotations, 
 *       internal-only structure.</li>
 *   <li><strong>GeneRecord (DTO)</strong>: immutable data transfer object safely returned through
 *       REST APIs to clients.</li>
 * </ul></p>
 *
 * <p>Having a dedicated factory class supports the <em>Factory / Mapper design pattern</em>,
 * simplifying the {@code GeneService} by moving mapping logic into a reusable component.</p>
 */
@Component
public class GeneFactory {

    /**
     * Converts a JPA {@link Gene} entity
     * into an immutable {@link GeneRecord} DTO
     * for safe exposure in API responses.
     *
     * <p>Example response JSON for TP53:</p>
     * <pre>{@code
     * {
     *   "name": "TP53",
     *   "description": "Tumor suppressor gene, guardian of genome",
     *   "normalFunction": "Controls cell cycle, DNA repair",
     *   "mutationEffect": "Loss causes uncontrolled proliferation",
     *   "prevalence": "50%",
     *   "therapies": "None approved, trials ongoing",
     *   "researchLinks": "https://pubmed.ncbi.nlm.nih.gov/..."
     * }
     * }</pre>
     *
     * @param gene the {@link Gene} entity retrieved from the database
     * @return a {@link GeneRecord} DTO suitable for returning to clients
     */
    public GeneRecord toDto(Gene gene) {
        return new GeneRecord(
                gene.getName(),
                gene.getDescription(),
                gene.getNormalFunction(),
                gene.getMutationEffect(),
                gene.getPrevalence(),
                gene.getTherapies(),
                gene.getResearchLinks()
        );
    }

    /**
     * Converts an API-facing {@link GeneRecord} DTO
     * into a JPA {@link Gene} entity
     * so it can be persisted to the database.
     *
     * <p>Example request JSON:</p>
     * <pre>{@code
     * POST /genes
     * Content-Type: application/json
     *
     * {
     *   "name": "KRAS",
     *   "description": "Oncogene frequently mutated in LUAD",
     *   "normalFunction": "GTPase, controls cell growth signaling",
     *   "mutationEffect": "G12 mutations lock KRAS in active state",
     *   "prevalence": "30%",
     *   "therapies": "KRAS G12C inhibitors (sotorasib, adagrasib)",
     *   "researchLinks": "https://pubmed.ncbi.nlm.nih.gov/..."
     * }
     * }</pre>
     *
     * @param dto the {@link GeneRecord} DTO from client request
     * @return a {@link Gene} entity ready for persistence
     */
    public Gene fromDto(GeneRecord dto) {
        Gene gene = new Gene();
        gene.setName(dto.name());
        gene.setDescription(dto.description());
        gene.setNormalFunction(dto.normalFunction());
        gene.setMutationEffect(dto.mutationEffect());
        gene.setPrevalence(dto.prevalence());
        gene.setTherapies(dto.therapies());
        gene.setResearchLinks(dto.researchLinks());
        return gene;
    }
}