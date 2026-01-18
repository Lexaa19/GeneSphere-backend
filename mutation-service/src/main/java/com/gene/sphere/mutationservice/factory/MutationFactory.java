package com.gene.sphere.mutationservice.factory;

import com.gene.sphere.mutationservice.model.Mutation;
import com.gene.sphere.mutationservice.model.MutationRecord;
import org.springframework.stereotype.Component;

/**
 * Factory for converting between Mutation entity and MutationRecord DTO.
 */
@Component
public class MutationFactory {

    /**
     * Converts a Mutation entity to a MutationRecord DTO for API responses.
     *
     * @param mutation the entity from database
     * @return immutable DTO for API
     */
    public MutationRecord toDto(Mutation mutation) {
        return new MutationRecord(
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
                mutation.getAlleleFrequency());
    }

    /**
     * Converts a MutationRecord DTO to a Mutation entity for persistence.
     *
     * @param mutationDto the DTO from API request
     * @return entity ready for database
     */
    public Mutation fromDto(MutationRecord mutationDto) {
        Mutation mutation = new Mutation();
        mutation.setGeneName(mutationDto.name());
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
