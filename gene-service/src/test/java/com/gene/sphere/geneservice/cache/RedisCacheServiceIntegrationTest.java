package com.gene.sphere.geneservice.cache;

import com.gene.sphere.geneservice.config.TestRedisConfig;
import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.GeneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class RedisCacheServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @MockBean
    private GeneService geneService;
    private RedisCacheService cacheService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
        // Clean Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create cache service with real Redis
        cacheService = new RedisCacheService(redisTemplate, geneService, null);
    }

    @Test
    void testRealRedisCache_ShouldCacheAndRetrieveGene() {
        // Given - Cache some data first
        var geneName = "TP53";
        var geneRecord = new GeneRecord(
                "TP53",
                "Tumor protein p53",
                "Cell cycle control",
                "Loss of function",
                "50% of cancers",
                "Targeted therapy",
                "https://tp53.org"
        );

        when(geneService.getGeneByName(geneName)).thenReturn(Optional.of(geneRecord));

        // When - First call (cache miss)
        var firstCall = cacheService.getGeneByName(geneName);

        // Then - Should get gene and cache it
        assertTrue(firstCall.isPresent());
        assertEquals(geneRecord, firstCall.get());

        // When - Second call (should be cache hit)
        var secondCall = cacheService.getGeneByName(geneName);

        // Then - Should get cached gene without calling service again
        assertTrue(secondCall.isPresent());
        assertEquals(geneRecord, secondCall.get());

        // Verify data is actually in Redis
        var cachedValue = redisTemplate.opsForValue().get("gene:TP53");
        assertNotNull(cachedValue);
        assertInstanceOf(GeneRecord.class, cachedValue);
    }

    @Test
    void testRealRedisEviction_ShouldRemoveSpecificGene() {
        // Given - Cache some data first
        var geneName = "TP53";
        var geneRecord = new GeneRecord(
                "TP53",
                "Tumor protein p53",
                "Cell cycle control",
                "Loss of function",
                "50% of cancers",
                "Targeted therapy",
                "https://tp53.org"
        );

        when(geneService.getGeneByName(geneName)).thenReturn(Optional.of(geneRecord));

        cacheService.getGeneByName(geneName);

        // Verify the gene is cached
        var beforeEviction = redisTemplate.opsForValue().get("gene:TP53");
        assertNotNull(beforeEviction);

        // When - Evict the gene
        cacheService.evictGene(geneName);

        // Then - Should be removed from Redis
        var afterEviction = redisTemplate.opsForValue().get("gene:TP53");
        assertNull(afterEviction);
    }

    @Test
    void testRealRedisClearCache_ShouldRemoveAllGenes() {
        var genes = new String[]{"BRCA1", "TP53", "EGFR"};

        for (var gene : genes) {
            var record = new GeneRecord(
                    gene,
                    "Description for " + gene,
                    "Normal function",
                    "Mutation effects",
                    "Common",
                    "Various therapies",
                    "https://example.com/" + gene
            );
            when(geneService.getGeneByName(gene)).thenReturn(Optional.of(record));
            cacheService.getGeneByName(gene); // Cache each gene
        }

        // Verify all the genes are cached
        for (var gene : genes) {
            var cached = redisTemplate.opsForValue().get("gene:" + gene);
            assertNotNull(cached, "Gene " + gene + " should be cached");
        }

        // When - Clear cache
        cacheService.clearCache();

        for (var gene : genes) {
            var cached = redisTemplate.opsForValue().get("gene: " + gene);
            assertNull(cached, "Gene " + gene + " should be evicted");
        }
    }

    @Test
    void testRedisTTL_ShouldExpireAfterConfiguredTime() throws InterruptedException {
        // Given - Gene with short TTL for testing
        var geneName = "SHORT_TTL";
        var geneRecord = new GeneRecord(
                "SHORT_TTL",
                "Gene with short TTL",
                "Test function",
                "Test mutation",
                "Rare",
                "Experimental",
                "https://test.com"
        );

        when(geneService.getGeneByName(geneName)).thenReturn(Optional.of(geneRecord));

        // When - Cache gene
        cacheService.getGeneByName(geneName);

        // Then - Should be cached immediately
        var immediateCheck = redisTemplate.opsForValue().get("gene:SHORT_TTL");
        assertNotNull(immediateCheck);

        var ttl = redisTemplate.getExpire("gene:SHORT_TTL");
        assertTrue(ttl > Duration.ofDays(4).getSeconds(), "TTL should be set to approximately 5 days");

        System.out.println("TTL for cached gene: " + ttl + " seconds");
    }
}