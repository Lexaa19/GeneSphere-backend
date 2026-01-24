package com.gene.sphere.mutationservice.factory;

import com.gene.sphere.mutationservice.model.Mutation;
import com.gene.sphere.mutationservice.model.MutationDto;
import org.springframework.stereotype.Component;

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
     * @return immutable DTO for API (used in both requests and responses), or null if input is null
     */
    public MutationDto toDto(Mutation mutation) {
        if (mutation == null) {
            return null;
        }
        
        return new MutationDto(
                mutation.getGeneName(),
                mutation.getChromosome(),
                mutation.getPosition(),
                mutation.getReferenceAllele(),
                mutation.getAlternateAllele(),
                mutation.getMutationType(),
                mutation.getPatientId(),
                mutation.getSampleId(),
                mutation.getProteinChange(),
                mutation.getCancerType(),
                mutation.getClinicalSignificance(),
                mutation.getAlleleFrequency()
        );
    }


    /**
     * Converts a MutationDto (from API request or response) to a Mutation entity for persistence.
     *
     * @param mutationDto the DTO from API request or response
     * @return entity ready for database, or null if input is null
     */
    public Mutation fromDto(MutationDto mutationDto) {
        if (mutationDto == null) {
            return null;
        }
        
        Mutation mutation = new Mutation();
        mutation.setGeneName(mutationDto.geneName());
        mutation.setChromosome(mutationDto.chromosome());
        mutation.setPosition(mutationDto.position());
        mutation.setReferenceAllele(mutationDto.referenceAllele());
        mutation.setAlternateAllele(mutationDto.alternateAllele());
        mutation.setMutationType(mutationDto.mutationType());
        mutation.setPatientId(mutationDto.patientId());
        mutation.setSampleId(mutationDto.sampleId());
        mutation.setProteinChange(mutationDto.proteinChange());
        mutation.setCancerType(mutationDto.cancerType());
        mutation.setClinicalSignificance(mutationDto.clinicalSignificance());
        mutation.setAlleleFrequency(mutationDto.alleleFrequency());
        return mutation;
    }

}
