package com.gene.sphere.mutationservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MutationDtoTest {

    private static MutationDto getMutationRecord() {
        // ARRANGE: Prepare test data
        var geneName = "TP53";
        var chromosome = "17";
        var position = 7579472L;
        var referenceAllele = "C";
        var alternateAllele = "T";
        var mutationType = "SNV";
        var patientId = "PATIENT_001";
        var sampleId = "TCGA-05-4244-01";
        var proteinChange = "p.R273H";
        var cancerType = "Lung Adenocarcinoma";
        var clinicalSignificance = "Pathogenic";
        BigDecimal alleleFrequency = new BigDecimal("0.45");


        // ACT - create the record
        MutationDto record = new MutationDto(
                geneName,
                chromosome,
                position,
                referenceAllele,
                alternateAllele,
                mutationType,
                patientId,
                sampleId,
                proteinChange,
                cancerType,
                clinicalSignificance,
                alleleFrequency
        );
        return record;
    }

    @Test
    void shouldCreateMutationRecord_withAllFields() {

        MutationDto record = getMutationRecord();

        assertNotNull(record);
        assertEquals("TP53", record.geneName());
        assertEquals("17", record.chromosome());
        assertEquals(7579472L, record.position());
        assertEquals("C", record.referenceAllele());
        assertEquals("T", record.alternateAllele());
        assertEquals("SNV", record.mutationType());
        assertEquals("PATIENT_001", record.patientId());
        assertEquals("TCGA-05-4244-01", record.sampleId());
        assertEquals("p.R273H", record.proteinChange());
        assertEquals("Lung Adenocarcinoma", record.cancerType());
        assertEquals("Pathogenic", record.clinicalSignificance());
        assertEquals(0, new BigDecimal("0.45").compareTo(record.alleleFrequency()));
    }

    @Test
    void shouldBeEqual_whenRecordsHaveSameData() {
        // ARRANGE: Create two identical records
        MutationDto record1 = new MutationDto(
                "TP53", "17", 7579472L, "C", "T", "SNV",
                "P001", "TCGA-05-4244-01", "p.R273H",
                "Lung Adenocarcinoma", "Pathogenic",
                new BigDecimal("0.45")
        );


        MutationDto record2 = new MutationDto(
                "TP53", "17", 7579472L, "C", "T", "SNV",
                "P001", "TCGA-05-4244-01", "p.R273H",
                "Lung Adenocarcinoma", "Pathogenic",
                new BigDecimal("0.45")
        );

        assertEquals(record1, record2);
        assertEquals(record1.hashCode(), record2.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenRecordsHaveDifferentData() {

        MutationDto record1 = new MutationDto(
                "TP53", "17", 7579472L, "C", "T", "SNV",
                "P001", "TCGA-05-4244-01", "p.R273H",
                "Lung Adenocarcinoma", "Pathogenic",
                new BigDecimal("0.45")
        );


        MutationDto record2 = new MutationDto(
                "TP53", "18", 7579472L, "C", "T", "SNV",
                "P011", "TCGA-05-4244-11", "p.R273H",
                "Lung Adenocarcinoma", "Pathogenic",
                new BigDecimal("0.45")
        );

        assertNotEquals(record1, record2);
    }

    @Test
    void shouldAllowNullOptionalFields() {
        var record = new MutationDto(
                "TP53", "17", 7579472L, "C", "T", "SNV",
                "P001", "TCGA-05-4244-01",
                null,  // proteinChange can be null
                "Lung Adenocarcinoma",
                null,  // clinicalSignificance can be null
                new BigDecimal("0.45")
        );
        assertNull(record.proteinChange());
        assertNull(record.clinicalSignificance());
    }

    @Test
    void shouldThrowException_whenMandatoryGeneNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        null,  // geneName is mandatory
                        "17", 7579472L, "C", "T", "SNV",
                        "P001", "TCGA-05-4244-01", "p.R273H",
                        "Lung Adenocarcinoma", "Pathogenic",
                        new BigDecimal("0.45")
                )

        );
    }

    @Test
    void shouldThrowException_whenGeneNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        "",
                        "17", 7579472L, "C", "T", "SNV",
                        "P001", "TCGA-05-4244-01", "p.R273H",
                        "Lung Adenocarcinoma", "Pathogenic",
                        new BigDecimal("0.45")
                )
        );
    }

    @Test
    void shouldThrowException_whenMutationTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        "TP53",
                        "17", 7579472L, "C", "T", null,
                        "P001", "TCGA-05-4244-01", "p.R273H",
                        "Lung Adenocarcinoma", "Pathogenic",
                        new BigDecimal("0.45")
                )
        );
    }

    @Test
    void shouldThrowException_whenMutationTypeIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        "TP53",
                        "17", 7579472L, "C", "T", "",
                        "P001", "TCGA-05-4244-01", "p.R273H",
                        "Lung Adenocarcinoma", "Pathogenic",
                        new BigDecimal("0.45")
                )
        );
    }


    @Test
    void shouldThrowException_whenPatientIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        "TP53",
                        "17", 7579472L, "C", "T", "SNV",
                        null, "TCGA-05-4244-01", "p.R273H",
                        "Lung Adenocarcinoma", "Pathogenic",
                        new BigDecimal("0.45")
                )
        );
    }

    @Test
    void shouldThrowException_whenPatientIdIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        "TP53",
                        "17", 7579472L, "C", "T", "SNV",
                        "", "TCGA-05-4244-01", "p.R273H",
                        "Lung Adenocarcinoma", "Pathogenic",
                        new BigDecimal("0.45")
                )
        );
    }

    @Test
    void shouldThrowException_whenCancerTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        "TP53",
                        "17", 7579472L, "C", "T", "SNV",
                        "111", "TCGA-05-4244-01", "p.R273H",
                        null, "Pathogenic",
                        new BigDecimal("0.45")
                )
        );
    }

    @Test
    void shouldThrowException_whenCancerTypeIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new MutationDto(
                        "TP53",
                        "17", 7579472L, "C", "T", "SNV",
                        "1111", "TCGA-05-4244-01", "p.R273H",
                        "", "Pathogenic",
                        new BigDecimal("0.45")
                )
        );
    }

    @Test
    void shouldGenerateReadableString_whenCallingToString() {
        var record = getMutationRecord();
        var result = record.toString();

        assertNotNull(result);
        assertTrue(result.contains("TP53"), "toString should contain gene name");
        assertTrue(result.contains("geneName"), "toString should contain field names");
        assertTrue(result.contains("17"), "toString should contain chromosome");
    }

    @Test
    void shouldAcceptLargePositionNumber() {
        var record = new MutationDto(
                "TP53", "17",
                999999999L,  // Very large position
                "C", "T", "SNV",
                "P001", "TCGA-05-4244-01", "p.R273H",
                "Lung Adenocarcinoma", "Pathogenic",
                new BigDecimal("0.45")
        );

        assertEquals(999999999L, record.position());
    }
}