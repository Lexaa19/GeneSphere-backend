package com.gene.sphere.mutationservice.factory;

import com.gene.sphere.mutationservice.model.Mutation;
import com.gene.sphere.mutationservice.model.MutationRecord;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
        return Optional.ofNullable(mutation)
                .map(m -> new MutationRecord(
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
     * Converts a MutationRecord DTO to a Mutation entity for persistence.
     *
     * @param mutationDto the DTO from API request
     * @return entity ready for database
     */
    public Mutation fromDto(MutationRecord mutationDto) {
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
