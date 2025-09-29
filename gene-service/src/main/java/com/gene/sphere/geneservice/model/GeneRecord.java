package com.gene.sphere.geneservice.model;

/**
 * Immutable DTO returned in API responses (never expose JPA Entity directly).
 */
public record GeneRecord(
        String name,
        String description,
        String normalFunction,
        String mutationEffect,
        String prevalence,
        String therapies,
        String researchLinks
) {
}