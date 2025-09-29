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
     * Finds a single gene whose name contains the given fragment, ignoring case.
     * <p>
     * Note: Because the query can match multiple rows, declaring a single-result
     * return type means Spring Data will throw an
     * {@link org.springframework.dao.IncorrectResultSizeDataAccessException}
     * if more than one result is found. If you expect multiple matches, consider
     * using a {@code List<Gene>} return type instead (e.g.,
     * {@code List<Gene> findAllByNameContainingIgnoreCase(String partOfName)}).
     * </p>
     *
     * @param partOfName a substring to search within gene names (case-insensitive)
     * @return an {@link Optional} containing the matching gene if exactly one is found; {@link Optional#empty()} if none
     */
    Optional<Gene> findByNameContainingIgnoreCase(String partOfName);
}