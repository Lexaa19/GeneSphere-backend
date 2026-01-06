package com.gene.sphere.mutationservice.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MutationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }


    @Test
    void testJsonSerialization() throws Exception {
        var mutation = getMutation();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(mutation);
        Mutation deserialized = mapper.readValue(json, Mutation.class);
        assertEquals(mutation, deserialized);
    }

    private static Mutation getMutation() {
        var mutation = new Mutation();
        mutation.setId(123);
        mutation.setGeneName("EGFR");
        mutation.setChromosome("7");
        mutation.setPosition(55191822L);
        mutation.setReferenceAllele("G");
        mutation.setAlternateAllele("T");
        mutation.setMutationType("SNV");
        mutation.setPatientId("TCGA-05-4244-01");
        mutation.setSampleId("TCGA-05-4244-01");
        mutation.setProteinChange("p.L858R");
        mutation.setCancerType("Lung Adenocarcinoma (TCGA)");
        mutation.setClinicalSignificance("Pathogenic");
        mutation.setAlleleFrequency(new BigDecimal("0.6523"));
        return mutation;
    }

    @Test
    void testGettersAndSetters() {
        var mutation = getMutation();

        assertEquals(123, mutation.getId());
        assertEquals("EGFR", mutation.getGeneName());
        assertEquals("7", mutation.getChromosome());
        assertEquals(55191822L, mutation.getPosition());
        assertEquals("G", mutation.getReferenceAllele());
        assertEquals("T", mutation.getAlternateAllele());
        assertEquals("SNV", mutation.getMutationType());
        assertEquals("TCGA-05-4244-01", mutation.getPatientId());
        assertEquals("TCGA-05-4244-01", mutation.getSampleId());
        assertEquals("p.L858R", mutation.getProteinChange());
        assertEquals("Lung Adenocarcinoma (TCGA)", mutation.getCancerType());
        assertEquals("Pathogenic", mutation.getClinicalSignificance());
        assertEquals(new BigDecimal("0.6523"), mutation.getAlleleFrequency());
    }

    @Test
    void testEqualsAndHashCode() {
        // Test the hashCode to make sure the entity works fine in hash-based collections
        var firstMutation = getMutation();
        var secondMutation = getMutation();
        assertEquals(firstMutation, secondMutation);
        assertEquals(firstMutation.hashCode(), secondMutation.hashCode());
        secondMutation.setChromosome("8");
        assertNotEquals(firstMutation, secondMutation);
    }

    @Test
    void testNullHandling() {
        var mutation = new Mutation();
        mutation.setGeneName(null);

        assertNull(mutation.getGeneName());

        var secondMutation = new Mutation();
        secondMutation.setId(123);
        secondMutation.setGeneName(null);
        secondMutation.setChromosome(null);

        var thirdMutation = new Mutation();
        thirdMutation.setId(123);
        thirdMutation.setGeneName(null);
        thirdMutation.setChromosome(null);

        assertEquals(secondMutation, thirdMutation);
        assertNotEquals(mutation, null);
        assertEquals(mutation, mutation);
    }

    @Test
    void validate_shouldReturnNoViolations_whenMutationIsValid() {
        var mutation = getMutation();
        Set<ConstraintViolation<Mutation>> violations = validator.validate(mutation);
        assertThat(violations).hasSize(0);
    }

    @Test
    void testKoValidation(){
        var koMutation = new Mutation();
        koMutation.setGeneName(null);
        koMutation.setChromosome(null);

        Set<ConstraintViolation<Mutation>> violation = validator.validate(koMutation);
        assertFalse(violation.isEmpty());
        assertThat(violation).hasSize(2);
        assertThat(violation).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                       "Gene name cannot be null",
                        "Chromosome cannot be null"
                );
    }

    @Test
    void testMinimalMutation() {
        var mutation = new Mutation();
        mutation.setId(1);
        // All other fields are null by default
        assertEquals(1, mutation.getId());
        assertNull(mutation.getGeneName());
        assertNull(mutation.getChromosome());
        assertNull(mutation.getPosition());
        assertNull(mutation.getReferenceAllele());
        assertNull(mutation.getAlternateAllele());
        assertNull(mutation.getMutationType());
        assertNull(mutation.getPatientId());
        assertNull(mutation.getSampleId());
        assertNull(mutation.getProteinChange());
        assertNull(mutation.getCancerType());
        assertNull(mutation.getClinicalSignificance());
        assertNull(mutation.getAlleleFrequency());
    }

    @Test
    void testToStringContainsKeyFields() {
        var mutation = getMutation();
        String str = mutation.toString();
        assertTrue(str.contains("EGFR"));
        assertTrue(str.contains("123"));
    }
}
