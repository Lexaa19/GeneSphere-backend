package com.gene.sphere.geneservice.cache;

import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.GeneService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class RedisCacheServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisCacheService cacheService;

    @MockBean
    private GeneService geneService;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private RLock rLock;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.redis.timeout", () -> "5000ms");
        registry.add("spring.data.redis.timeout", () -> "5000ms");
        registry.add("spring.redis.lettuce.shutdown-timeout", () -> "200ms");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Ignore connection errors in setup
        }
        reset(geneService, redissonClient, rLock);

        // Mock Redisson lock behavior - always allow lock acquisition
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        doNothing().when(rLock).unlock();
    }

    @Test
    void testRealRedisCache_ShouldCacheAndRetrieveGene() {
        String geneName = "TP53";
        GeneRecord geneRecord = new GeneRecord(
                "TP53",
                "Tumor protein p53",
                "Cell cycle control",
                "Loss of function",
                "50% of cancers",
                "Targeted therapy",
                "https://tp53.org"
        );

        when(geneService.getGeneByName(geneName)).thenReturn(Optional.of(geneRecord));

        var firstCall = cacheService.getGeneByName(geneName);
        assertTrue(firstCall.isPresent());
        assertEquals(geneRecord, firstCall.get());

        var secondCall = cacheService.getGeneByName(geneName);
        assertTrue(secondCall.isPresent());
        assertEquals(geneRecord, secondCall.get());

        verify(geneService, times(1)).getGeneByName(geneName);

        Object cachedValue = redisTemplate.opsForValue().get("gene:TP53");
        assertNotNull(cachedValue);
        assertInstanceOf(GeneRecord.class, cachedValue);
    }

    @Test
    void testRealRedisEviction_ShouldRemoveSpecificGene() {
        String geneName = "TP53";
        GeneRecord geneRecord = new GeneRecord(
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

        Object beforeEviction = redisTemplate.opsForValue().get("gene:TP53");
        assertNotNull(beforeEviction);

        cacheService.evictGene(geneName);

        Object afterEviction = redisTemplate.opsForValue().get("gene:TP53");
        assertNull(afterEviction);

        var callAfterEviction = cacheService.getGeneByName(geneName);
        assertTrue(callAfterEviction.isPresent());
        verify(geneService, times(2)).getGeneByName(geneName);
    }

    @Test
    void testRealRedisClearCache_ShouldRemoveAllGenes() {
        String[] genes = {"BRCA1", "TP53", "EGFR"};

        for (String gene : genes) {
            GeneRecord record = new GeneRecord(
                    gene,
                    "Description for " + gene,
                    "Normal function",
                    "Mutation effects",
                    "Common",
                    "Various therapies",
                    "https://example.com/" + gene
            );
            when(geneService.getGeneByName(gene)).thenReturn(Optional.of(record));
            cacheService.getGeneByName(gene);
        }

        for (String gene : genes) {
            assertNotNull(redisTemplate.opsForValue().get("gene:" + gene));
        }

        cacheService.clearCache();

        for (String gene : genes) {
            assertNull(redisTemplate.opsForValue().get("gene:" + gene));
        }

        for (String gene : genes) {
            cacheService.getGeneByName(gene);
        }
        verify(geneService, times(2)).getGeneByName("BRCA1");
        verify(geneService, times(2)).getGeneByName("TP53");
        verify(geneService, times(2)).getGeneByName("EGFR");
    }

    @Test
    void testRedisTTL_ShouldExpireAfterConfiguredTime() {
        String geneName = "SHORT_TTL";
        GeneRecord geneRecord = new GeneRecord(
                "SHORT_TTL",
                "Gene with short TTL",
                "Test function",
                "Test mutation",
                "Rare",
                "Experimental",
                "https://test.com"
        );

        when(geneService.getGeneByName(geneName)).thenReturn(Optional.of(geneRecord));

        cacheService.getGeneByName(geneName);

        Object immediateCheck = redisTemplate.opsForValue().get("gene:SHORT_TTL");
        assertNotNull(immediateCheck);

        Long ttl = redisTemplate.getExpire("gene:SHORT_TTL");
        assertNotNull(ttl);
        assertTrue(ttl > Duration.ofDays(4).getSeconds(), "TTL should be ~5 days");
    }

    @Test
    void testCacheMiss_ShouldReturnEmptyWhenGeneNotFound() {
        String geneName = "NONEXISTENT";
        when(geneService.getGeneByName(geneName)).thenReturn(Optional.empty());

        var result = cacheService.getGeneByName(geneName);
        assertFalse(result.isPresent());

        verify(geneService, times(1)).getGeneByName(geneName);

        var secondResult = cacheService.getGeneByName(geneName);
        assertFalse(secondResult.isPresent());
        verify(geneService, times(2)).getGeneByName(geneName);
    }
}