package com.gene.sphere.mutationservice.factory;

import com.gene.sphere.mutationservice.model.Mutation;
import com.gene.sphere.mutationservice.model.MutationDto;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Factory for converting between Mutation entity and MutationDto (API request/response DTO).
 *
 * <p>
 * MutationDto is used for both API requests (incoming data) and responses (outgoing data).
 * This factory provides bidirectional conversion between the database entity and the immutable DTO.
 */
@Component
public class MutationFactory {

    /**
     * Converts a Mutation entity to a MutationDto for API responses.
     *
     * @param mutation the entity from the database
     * @return immutable DTO for API (used in both requests and responses)
     */
    public MutationDto toDto(Mutation mutation) {
        return Optional.ofNullable(mutation)
                .map(m -> new MutationDto(
                        m.getGeneName(),
                        m.getChromosome(),
                        m.getPosition(),
                        m.getReferenceAllele(),
                        m.getAlternateAllele(),
                        m.getMutationType(),
                        m.getPatientId(),
                        m.getSampleId(),
                        m.getProteinChange(),
                        m.getCancerType(),
                        m.getClinicalSignificance(),
                        m.getAlleleFrequency()
                ))
                .orElse(null);
    }


    /**
     * Converts a MutationDto (from API request or response) to a Mutation entity for persistence.
     *
     * @param mutationDto the DTO from API request or response
     * @return entity ready for database
     */
    public Mutation fromDto(MutationDto mutationDto) {
        return Optional.ofNullable(mutationDto)
                .map(dto -> {
                    Mutation mutation = new Mutation();
                    mutation.setGeneName(dto.geneName());
                    mutation.setChromosome(dto.chromosome());
                    mutation.setPosition(dto.position());
                    mutation.setReferenceAllele(dto.referenceAllele());
                    mutation.setAlternateAllele(dto.alternateAllele());
                    mutation.setMutationType(dto.mutationType());
                    mutation.setPatientId(dto.patientId());
                    mutation.setSampleId(dto.sampleId());
                    mutation.setProteinChange(dto.proteinChange());
                    mutation.setCancerType(dto.cancerType());
                    mutation.setClinicalSignificance(dto.clinicalSignificance());
                    mutation.setAlleleFrequency(dto.alleleFrequency());
                    return mutation;
                })
                .orElse(null);
    }

}
