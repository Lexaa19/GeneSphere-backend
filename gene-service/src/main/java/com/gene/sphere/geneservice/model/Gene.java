package com.gene.sphere.geneservice.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class Gene {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String normalFunction;
    private String mutationEffect;
    // shows how common is a gene mutation in lung cancer
    private String prevalance;
    private String therapies;
    private String researchLinks;

}
