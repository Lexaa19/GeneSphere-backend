package com.gene.sphere.geneservice.cache;

import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.GeneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private GeneService geneService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        // lenient() = "It's okay if this stub isn't used in every test - don't throw an exception"
        // If not all the methods use this mock, Mockito will complain and throw some stubbing error
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new RedisCacheService(redisTemplate, geneService, null);
    }

    @Test
    void testCacheHit_ShouldReturnCachedGene() {
        var geneName = "TP53";
        var expectedGene = new GeneRecord(
                "TP53",
                "Guardian of the genome",
                "Cell cycle regulation",
                "Oncogenic mutations",
                "50% of cancers",
                "MDM2 inhibitors",
                "https://p53.com"
        );
        var cacheKey = "gene:TP53";

        when(valueOperations.get(cacheKey)).thenReturn(expectedGene);

        var result = cacheService.getGeneByName(geneName);

        assertAll("Cache hit and found the result",
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(expectedGene, result.get()),
                // Don't call the service as the gene is found in cache
                () -> verify(geneService, never()).getGeneByName(geneName)
        );
    }

    @Test
    void testCacheMiss_ShouldFetchFromServiceAndCache() {
        var geneName = "TP53";
        var expectedGene = new GeneRecord(
                "TP53",
                "Guardian of the genome",
                "Cell cycle regulation",
                "Oncogenic mutations",
                "50% of cancers",
                "MDM2 inhibitors",
                "https://p53.com"
        );

        var cachedKey = "gene:TP53";

        when(valueOperations.get(cachedKey)).thenReturn(null);
        when(geneService.getGeneByName(geneName)).thenReturn(Optional.of(expectedGene));

        var result = cacheService.getGeneByName(geneName);

        assertAll("Cache miss and no result found",
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(expectedGene, result.get()),
                () -> verify(geneService).getGeneByName(geneName),
                () -> verify(valueOperations).set(eq(cachedKey), eq(expectedGene), any()) // Should cache the result
        );
    }

    @Test
    void testClearCache_ShouldDeleteAllGeneKeys() {
        var keysToDelete = Set.of("gene:BRCA1", "gene:TP53", "gene:EGFR");

        when(redisTemplate.keys("gene:*")).thenReturn(keysToDelete);

        cacheService.clearCache();

        verify(redisTemplate).keys("gene:*"); // Did it search?
        verify(redisTemplate).delete(keysToDelete); // Did it delete?
    }

    @Test
    void testEvictGene_ShouldDeleteSpecificKey() {
        var geneName = "BRCA1";
        var expectedKey = "gene:BRCA1";

        cacheService.evictGene(geneName);

        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    void shouldReturnEmpty_whenGeneNotFoundInCacheOrDatabase() {
        var geneName = "UNKNOWN_GENE";
        var cacheKey = "gene:UNKNOWN_GENE";

        // Redis cache miss
        when(valueOperations.get(cacheKey)).thenReturn(null);

        // Database miss (return Optional.empty())
        when(geneService.getGeneByName(geneName)).thenReturn(Optional.empty());

        // Verify: result is empty, service WAS called, nothing cached
        var result = cacheService.getGeneByName(geneName);

        // Check what the user gets
        assertAll("Gene not found in db and Redis",
                () -> assertTrue(result.isEmpty()),
                () -> assertFalse(result.isPresent())
        );

        // Check what the system did
        verify(geneService).getGeneByName(geneName);
        verify(valueOperations, never()).set(any(), any(), any()); // Should not cache empty
    }

    @Test
    void shouldReturnEmpty_whenGeneNameIsNull() {
        var result = cacheService.getGeneByName(null);

        assertTrue(result.isEmpty());

        // Should not call Redis
        verify(valueOperations, never()).get(any());

        // Should not call the db
        verify(geneService, never()).getGeneByName(any());
    }

    @Test
    void shouldReturnEmpty_whenGeneNameIsBlankOrEmpty() {
        var invalidInputs = new String[]{
                "",
                " ",
                "    ",
                "\t",         // Add tab only
                "\n",         // Add newline only
                "\t\n",
                "  \t\n  "    // Add mixed whitespace
        };
        // Use Java 17 features like switch expressions
        for (var input : invalidInputs) {
            var result = cacheService.getGeneByName(input);
            var description = switch (input) {
                case "" -> "empty string";
                case " " -> "single space";
                case "    " -> "multiple spaces";
                case "\t" -> "tab character";
                case "\n" -> "newline character";
                case "\t\n" -> "tab + newline";
                case "  \t\n  " -> "mixed whitespace";
                default -> "unknown whitespace input";
            };
            assertTrue(result.isEmpty(), "Should return empty for: " + description);
        }
        verify(valueOperations, never()).get(any());
        verify(geneService, never()).getGeneByName(any());

    }
}
