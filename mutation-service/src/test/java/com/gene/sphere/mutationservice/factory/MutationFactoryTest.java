package com.gene.sphere.mutationservice.factory;

import com.gene.sphere.mutationservice.model.Mutation;
import com.gene.sphere.mutationservice.model.MutationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MutationFactoryTest {
    private MutationFactory mutationFactory;

    @BeforeEach
    void setUp() {
        mutationFactory = new MutationFactory();
    }

    @Test
    void toDto_shouldMapAllFields_whenGivenValidMutation() {
        // ARRANGE: Create a Mutation entity with all fields
        Mutation mutation = new Mutation();
        mutation.setGeneName("TP53");
        mutation.setChromosome("17");
        mutation.setPosition(7579472L);
        mutation.setReferenceAllele("C");
        mutation.setAlternateAllele("T");
        mutation.setMutationType("SNV");
        mutation.setPatientId("PATIENT_001");
        mutation.setSampleId("TCGA-05-4244-01");
        mutation.setProteinChange("p.R273H");
        mutation.setCancerType("Lung Adenocarcinoma");
        mutation.setClinicalSignificance("Pathogenic");
        mutation.setAlleleFrequency(new BigDecimal("0.45"));

        // ACT: Convert to DTO
        MutationDto dto = mutationFactory.toDto(mutation);

        // ASSERT: Check ALL 12 fields are mapped correctly
        assertNotNull(dto, "DTO should not be null");
        assertEquals("TP53", dto.geneName(), "Gene name should match");
        assertEquals("17", dto.chromosome(), "Chromosome should match");
        assertEquals(7579472L, dto.position(), "Position should match");
        assertEquals("C", dto.referenceAllele(), "Reference allele should match");
        assertEquals("T", dto.alternateAllele(), "Alternate allele should match");
        assertEquals("SNV", dto.mutationType(), "Mutation type should match");
        assertEquals("PATIENT_001", dto.patientId(), "Patient ID should match");
        assertEquals("TCGA-05-4244-01", dto.sampleId(), "Sample ID should match");
        assertEquals("p.R273H", dto.proteinChange(), "Protein change should match");
        assertEquals("Lung Adenocarcinoma", dto.cancerType(), "Cancer type should match");
        assertEquals("Pathogenic", dto.clinicalSignificance(), "Clinical significance should match");
        assertEquals(0, new BigDecimal("0.45").compareTo(dto.alleleFrequency()),
                "Allele frequency should match");
    }

    @Test
    void fromDto_shouldMapAllFields_whenGivenValidDto() {
        // ARRANGE: Create DTO with all fields
        MutationDto dto = new MutationDto(
                "TP53", "17", 7579472L, "C", "T", "SNV",
                "PATIENT_001", "TCGA-05-4244-01",
                "p.R273H",
                "Lung Adenocarcinoma",
                "Pathogenic",
                new BigDecimal("0.45")
        );

        // ACT: Convert to entity
        Mutation mutation = mutationFactory.fromDto(dto);

        // ASSERT: Check ALL 12 fields are mapped correctly
        assertNotNull(mutation, "Entity should not be null");
        assertEquals("TP53", mutation.getGeneName(), "Gene name should match");
        assertEquals("17", mutation.getChromosome(), "Chromosome should match");
        assertEquals(7579472L, mutation.getPosition(), "Position should match");
        assertEquals("C", mutation.getReferenceAllele(), "Reference allele should match");
        assertEquals("T", mutation.getAlternateAllele(), "Alternate allele should match");
        assertEquals("SNV", mutation.getMutationType(), "Mutation type should match");
        assertEquals("PATIENT_001", mutation.getPatientId(), "Patient ID should match");
        assertEquals("TCGA-05-4244-01", mutation.getSampleId(), "Sample ID should match");
        assertEquals("p.R273H", mutation.getProteinChange(), "Protein change should match");
        assertEquals("Lung Adenocarcinoma", mutation.getCancerType(), "Cancer type should match");
        assertEquals("Pathogenic", mutation.getClinicalSignificance(), "Clinical significance should match");
        assertEquals(0, new BigDecimal("0.45").compareTo(mutation.getAlleleFrequency()),
                "Allele frequency should match");
    }

    @Test
    void toDto_shouldHandleNullOptionalFields_whenGivenPartialMutation() {
        // ARRANGE: Create mutation with nulls for optional fields
        Mutation mutation = new Mutation();
        mutation.setGeneName("TP53");
        mutation.setChromosome("17");
        mutation.setPosition(7579472L);
        mutation.setReferenceAllele("C");
        mutation.setAlternateAllele("T");
        mutation.setMutationType("SNV");
        mutation.setPatientId("PATIENT_001");
        mutation.setSampleId("TCGA-05-4244-01");
        mutation.setProteinChange(null);  // Optional - can be null
        mutation.setCancerType("Lung Adenocarcinoma");
        mutation.setClinicalSignificance(null);  // Optional - can be null
        mutation.setAlleleFrequency(new BigDecimal("0.45"));

        // ACT: Convert entity to DTO
        MutationDto dto = mutationFactory.toDto(mutation);

        // ASSERT: Null fields should remain null in DTO
        assertNotNull(dto, "DTO should not be null");
        assertNull(dto.proteinChange(), "Protein change should be null");
        assertNull(dto.clinicalSignificance(), "Clinical significance should be null");

        // But required fields should still be present
        assertEquals("TP53", dto.geneName(), "Gene name should be present");
        assertEquals("Lung Adenocarcinoma", dto.cancerType(), "Cancer type should be present");
    }


    @Test
    void fromDto_shouldHandleNullOptionalFields_whenGivenPartialDto() {
        // ARRANGE: Create DTO with null optional fields
        MutationDto dto = new MutationDto(
                "TP53", "17", 7579472L, "C", "T", "SNV",
                "PATIENT_001", "TCGA-05-4244-01",
                null,  // proteinChange can be null
                "Lung Adenocarcinoma",
                null,  // clinicalSignificance can be null
                new BigDecimal("0.45")
        );

        // ACT: Convert DTO to entity
        Mutation mutation = mutationFactory.fromDto(dto);

        // ASSERT
        assertNotNull(mutation);
        assertNull(mutation.getProteinChange(), "Protein change should be null");
        assertNull(mutation.getClinicalSignificance(), "Clinical significance should be null");
        assertEquals("TP53", mutation.getGeneName(), "Gene name should be present");
        assertEquals("Lung Adenocarcinoma", mutation.getCancerType(), "Cancer type should be present");
    }


    @Test
    void roundTrip_shouldPreserveAllData() {
        // ARRANGE
        Mutation original = new Mutation();
        original.setGeneName("TP53");
        original.setChromosome("17");
        original.setPosition(7579472L);
        original.setReferenceAllele("C");
        original.setAlternateAllele("T");
        original.setMutationType("SNV");
        original.setPatientId("PATIENT_001");
        original.setSampleId("TCGA-05-4244-01");
        original.setProteinChange("p.R273H");
        original.setCancerType("Lung Adenocarcinoma");
        original.setClinicalSignificance("Pathogenic");
        original.setAlleleFrequency(new BigDecimal("0.45"));

        // ACT: Entity → DTO → Entity (round trip)
        MutationDto dto = mutationFactory.toDto(original);
        Mutation result = mutationFactory.fromDto(dto);

        // ASSERT
        assertNotNull(result, "Result entity should not be null");
        assertEquals(original.getGeneName(), result.getGeneName(), "Gene name should be preserved");
        assertEquals(original.getChromosome(), result.getChromosome(), "Chromosome should be preserved");
        assertEquals(original.getPosition(), result.getPosition(), "Position should be preserved");
        assertEquals(original.getReferenceAllele(), result.getReferenceAllele(), "Reference allele should be preserved");
        assertEquals(original.getAlternateAllele(), result.getAlternateAllele(), "Alternate allele should be preserved");
        assertEquals(original.getMutationType(), result.getMutationType(), "Mutation type should be preserved");
        assertEquals(original.getPatientId(), result.getPatientId(), "Patient ID should be preserved");
        assertEquals(original.getSampleId(), result.getSampleId(), "Sample ID should be preserved");
        assertEquals(original.getProteinChange(), result.getProteinChange(), "Protein change should be preserved");
        assertEquals(original.getCancerType(), result.getCancerType(), "Cancer type should be preserved");
        assertEquals(original.getClinicalSignificance(), result.getClinicalSignificance(), "Clinical significance should be preserved");
        assertEquals(0, original.getAlleleFrequency().compareTo(result.getAlleleFrequency()), "Allele frequency should be preserved");
    }

    @Test
    void toDto_shouldReturnNull_whenGivenNullEntity() {
        MutationDto toDto = mutationFactory.toDto(null);
        assertNull(toDto, "Should return null when given null entity");
    }

    @Test
    void fromDto_shouldReturnNull_whenGivenNullDto() {
        Mutation fromDto = mutationFactory.fromDto(null);
        assertNull(fromDto, "Should return null when given null DTO");
    }

    @Test
    void shouldHandleVeryLargePosition() {
        MutationDto record = new MutationDto(
                "TP53", "17", Long.MAX_VALUE, "C", "T", "SNV",
                "PATIENT_001", "TCGA-05-4244-01",
                "p.R273H",
                "Lung Adenocarcinoma",
                "Pathogenic",
                new BigDecimal("0.45")
        );

        Mutation fromDto = mutationFactory.fromDto(record);

        assertNotNull(fromDto, "Entity should not be null");
        assertEquals(Long.MAX_VALUE, fromDto.getPosition(), "Position should handle Long.MAX_VALUE");
    }

    @Test
    void shouldHandleZeroAndMaxAlleleFrequency() {
        MutationDto maxRecord = new MutationDto(
                "TP53", "17", Long.MAX_VALUE, "C", "T", "SNV",
                "PATIENT_001", "TCGA-05-4244-01",
                "p.R273H",
                "Lung Adenocarcinoma",
                "Pathogenic",
                new BigDecimal("1.0")
        );
        Mutation maxFromDto = mutationFactory.fromDto(maxRecord);
        assertNotNull(maxFromDto);
        assertEquals(0, maxFromDto.getAlleleFrequency().compareTo(maxRecord.alleleFrequency()), "Allele frequency should be preserved");

        MutationDto minRecord = new MutationDto(
                "TP53", "17", Long.MAX_VALUE, "C", "T", "SNV",
                "PATIENT_001", "TCGA-05-4244-01",
                "p.R273H",
                "Lung Adenocarcinoma",
                "Pathogenic",
                new BigDecimal("0.0")
        );

        Mutation minFromDto = mutationFactory.fromDto(minRecord);
        assertNotNull(minFromDto);
        assertEquals(0, minFromDto.getAlleleFrequency().compareTo(minRecord.alleleFrequency()), "Allele frequency should be preserved");
    }

    @Test
    void toDto_shouldNotExposeEntityId() {
        Mutation mutation = new Mutation();
        mutation.setId(123);
        mutation.setGeneName("TP53");
        mutation.setMutationType("SNV");
        mutation.setPatientId("PATIENT_001");
        mutation.setCancerType("Lung Adenocarcinoma");
        // Populate required fields so MutationDto canonical constructor does not throw
        mutation.setChromosome("17");
        mutation.setPosition(123456L);
        mutation.setReferenceAllele("C");
        mutation.setAlternateAllele("T");

        MutationDto toDto = mutationFactory.toDto(mutation);
        assertNotNull(toDto);
        assertThrows(NoSuchMethodException.class, () -> {
            MutationDto.class.getDeclaredMethod("id");
        });
    }

}