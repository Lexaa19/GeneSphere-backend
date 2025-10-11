package com.gene.sphere.geneservice.repository;

import com.gene.sphere.geneservice.model.Gene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Gene} entities.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository} and declares
 * additional query methods derived from method names.
 * The entity ID type is {@link Integer}.
 * </p>
 */
@Repository
public interface GeneRepository extends JpaRepository<Gene,Integer> {

    /**
     * Finds all genes whose name matches the provided value exactly.
     * <p>
     * Case sensitivity depends on the underlying database collation/settings.
     * </p>
     *
     * @param name the exact gene name to match (e.g., "TP53")
     * @return a list of genes with the given name (possibly empty)
     */
    List<Gene> findByName(String name);

    /**
     * Finds a single gene whose name matches exactly, ignoring case.
     * This is safer than "containing" for unique lookups.
     *
     * @param name the exact gene name to match (case-insensitive)
     * @return an {@link Optional} containing the matching gene if found
     */
    Optional<Gene> findByNameIgnoreCase(String name);

    /**
     * Finds all genes whose name contains the given fragment, ignoring case.
     * <p>
     * This method returns a list to handle multiple matches properly.
     * Use this for search functionality where multiple results are expected.
     * </p>
     *
     * @param partOfName a substring to search within gene names (case-insensitive)
     * @return a list of genes containing the substring (possibly empty)
     */
    List<Gene> findAllByNameContainingIgnoreCase(String partOfName);

    /**
     * Finds the first gene whose name contains the given fragment, ignoring case.
     * <p>
     * This method is deprecated and may cause issues if multiple genes match.
     * Use findByNameIgnoreCase for exact matches or findAllByNameContainingIgnoreCase for searches.
     * </p>
     *
     * @deprecated Use findByNameIgnoreCase for exact matches
     * @param partOfName a substring to search within gene names (case-insensitive)
     * @return an {@link Optional} containing the first matching gene if found
     */
    @Deprecated
    Optional<Gene> findByNameContainingIgnoreCase(String partOfName);
}