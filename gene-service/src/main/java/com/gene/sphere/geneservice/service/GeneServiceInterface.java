package com.gene.sphere.geneservice.service;

import com.gene.sphere.geneservice.model.GeneRecord;
import java.util.List;
import java.util.Optional;

public interface GeneServiceInterface {
    Optional<GeneRecord> getGeneByName(String name);
    List<GeneRecord> getAllGenes();
    GeneRecord createGene(GeneRecord dto);
    GeneRecord updateGene(Integer id, GeneRecord dto);
    void deleteGene(Integer id);
}