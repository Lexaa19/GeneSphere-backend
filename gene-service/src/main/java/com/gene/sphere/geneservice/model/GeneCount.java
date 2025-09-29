package com.gene.sphere.geneservice.model;

/**
 * Simple DTO for returning mutation frequency
 * e.g. { "gene": "TP53", "count": 561 }
 */
public record GeneCount(String gene, long count) {
}
