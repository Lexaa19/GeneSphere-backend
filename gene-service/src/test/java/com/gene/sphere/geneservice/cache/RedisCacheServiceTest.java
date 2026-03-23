package com.gene.sphere.geneservice.cache;

import com.gene.sphere.geneservice.model.Gene;
import com.gene.sphere.geneservice.model.GeneRecord;
import com.gene.sphere.geneservice.service.GeneService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;
    @Mock
    GeneService geneService;
    @Mock
    RedissonClient redissonClient;
    @Mock
    MeterRegistry meterRegistry;
    @Mock
    ValueOperations<String, Object> valueOperations; // For redisTemplate.opsForValue()
    @Mock
    Counter counter;
    @Mock
    RLock lock;  // For redissonClient.getLock()
    private RedisCacheService cacheService;

    /**
     * Creates a test GeneRecord with the specified name and default values.
     */
    private GeneRecord createTestGene(String name) {
        return new GeneRecord(
                name,
                "Test description for " + name,
                "Test normal function",
                "Test mutation effect",
                "Test prevalence",
                "Test therapies",
                "https://test.example.com/gene/" + name
        );
    }

    /**
     * Creates a fully customizable test GeneRecord.
     */
    private GeneRecord createTestGene(
            String name,
            String description,
            String normalFunction) {
        return new GeneRecord(
                name,
                description,
                normalFunction,
                "Default mutation effect",
                "Default prevalence",
                "Default therapies",
                "https://test.example.com/gene/" + name
        );
    }

    @BeforeEach
    void setUp() {
        // Setup redisTemplate to return valueOperations for all calls
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RedisCacheService.CacheConfig config = new RedisCacheService.CacheConfig(
                Duration.ofDays(5),
                "gene:",
                true
        );

        cacheService = new RedisCacheService(
                redisTemplate,
                geneService,
                redissonClient,
                config
        );

        // Use Reflection to set fields that would normally be injected by Spring
        ReflectionTestUtils.setField(cacheService, "meterRegistry", meterRegistry);
        ReflectionTestUtils.setField(cacheService, "lockWaitTime", Duration.ofSeconds(3));
        ReflectionTestUtils.setField(cacheService, "lockLeaseTime", Duration.ofSeconds(10));
        ReflectionTestUtils.setField(cacheService, "cacheTtl", Duration.ofDays(5));
    }

    @Test
    void shouldReturnEmptyWhenGeneNameIsNull() {
        // Arrange - ask meterRegistry for a fake counter
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act - search the cache with null value
        Optional<GeneRecord> result = cacheService.getGeneByName(null);

        // Assert - make sure no gene is returned
        assertThat(result).isEmpty();
        // Check that the code asked for a counter named "cache.requests.invalid"
        verify(meterRegistry).counter("cache.requests.invalid");
        verify(counter).increment();
        verify(geneService, never()).getGeneByName(anyString());
    }

    @Test
    void shouldReturnEmptyWhenGeneNameIsBlank() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        Optional<GeneRecord> result = cacheService.getGeneByName(" ");

        assertTrue(result.isEmpty());
        verify(meterRegistry).counter("cache.requests.invalid");
        verify(counter).increment();
        verify(geneService, never()).getGeneByName(anyString());
    }

    @Test
    void shouldReturnHitFromCacheWhenGeneIsFound() {
        GeneRecord fakeGene = createTestGene("TP53");
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // stub the cache to return the gene
        when(valueOperations.get("gene:TP53")).thenReturn(fakeGene);
        Optional<GeneRecord> cachedGene = cacheService.getGeneByName("tp53");

        assertThat(cachedGene).isPresent();
        assertEquals(cachedGene.get().name(), "TP53");

        verify(valueOperations).get("gene:TP53");  // ✅ CORRECT!
        verify(meterRegistry).counter("cache.hits");
        verify(counter).increment();
        verify(geneService, never()).getGeneByName(anyString());
    }

    @Test
    void shouldEvictGeneFromCache() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        cacheService.evictGene("TP53");
        verify(redisTemplate).delete("gene:TP53");
        verify(meterRegistry).counter("cache.evictions");
        verify(counter).increment();
    }

    @Test
    void shouldNotEvictWhenGeneNameIsNull() {
        cacheService.evictGene(null);
        verify(redisTemplate, never()).delete(anyString());
        verify(counter, never()).increment();
    }

    @Test
    void shouldNormalizeGeneNameToUppercase() {
        GeneRecord fakeGene = createTestGene("TP53");
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // stub the cache to return the gene
        when(valueOperations.get("gene:TP53")).thenReturn(fakeGene);
        Optional<GeneRecord> firstCachedGene = cacheService.getGeneByName("tp53");
        Optional<GeneRecord> secondCachedGene = cacheService.getGeneByName("tP53");
        Optional<GeneRecord> thirdCachedGene = cacheService.getGeneByName("Tp53");

        assertThat(firstCachedGene).isPresent();
        assertThat(secondCachedGene).isPresent();
        assertThat(thirdCachedGene).isPresent();

        verify(valueOperations, times(3)).get("gene:TP53");
    }

    @Test
    void shouldLoadFromDatabaseWhenCacheMisses() throws Exception {
        // Arrange
        GeneRecord fakeGene = createTestGene("EGFR");
        // Mock counter with varargs - use proper varargs matching
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        when(valueOperations.get("gene:EGFR")).thenReturn(null); // Cache miss (both checks)
        when(geneService.getGeneByName("EGFR")).thenReturn(Optional.of(fakeGene));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

        // Act
        Optional<GeneRecord> result = cacheService.getGeneByName("EGFR");

        // Assert
        assertThat(result).isPresent();
        assertEquals("EGFR", result.get().name());
        verify(valueOperations, times(2)).get("gene:EGFR"); // Checked twice: initial check + double-check
        verify(geneService).getGeneByName("EGFR"); // Loaded from DB
        verify(valueOperations).set(eq("gene:EGFR"), eq(fakeGene), any(Duration.class)); // Cached result
        verify(meterRegistry).counter("cache.misses");
        verify(lock).unlock(); // Lock released
    }

    @Test
    void shouldReturnEmptyWhenGeneNotFoundInCacheOrDatabase() throws Exception {
        // Arrange - Gene doesn't exist anywhere
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        when(valueOperations.get("gene:REJR")).thenReturn(null); // NOT in cache
        when(geneService.getGeneByName("REJR")).thenReturn(Optional.empty()); // NOT in database
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

        // Act - Try to get non-existent gene via CACHE SERVICE (not geneService directly!)
        Optional<GeneRecord> result = cacheService.getGeneByName("REJR");

        // Assert - Should return empty
        assertThat(result).isEmpty();
        verify(valueOperations, times(2)).get("gene:REJR"); // Checked cache twice (initial + double-check)
        verify(geneService).getGeneByName("REJR"); // Tried to load from DB
        verify(valueOperations, never()).set(anyString(), any(), any()); // Did NOT cache (nothing to cache!)
        verify(lock).unlock(); // Lock was released
    }

    @Test
        // Two threads try to get the same gene at the exact same time
    void shouldHandleLockContentionGracefully() throws Exception {
        // ARRANGE: Mock the world where lock is busy
        // 1. Setup counters
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        // 2. Create test gene
        GeneRecord fakeGene = createTestGene("EGFR");
        // 3. Cache returns null
        when(valueOperations.get("gene:EGFR")).thenReturn(null);
        // 4. Lock returns FALSE
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        // 5. Database returns gene
        when(geneService.getGeneByName("EGFR")).thenReturn(Optional.of(fakeGene));

        // ACT: Ask for the gene
        Optional<GeneRecord> result = cacheService.getGeneByName("EGFR");

        // ASSERT: Verify fallback worked
        assertThat(result).isPresent();
        verify(lock, never()).unlock(); // Never unlocked (never got it)
    }

    @Test
    void shouldReturnCachedValueFromDoubleCheckAfterLockAcquired() throws Exception {
        // ARRANGE
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        GeneRecord fakeGene = createTestGene("KRAS");

        // KEY: Cache returns DIFFERENT values on 1st vs 2nd call!
        when(valueOperations.get("gene:KRAS"))
                .thenReturn(null)      // ← 1st check: MISS
                .thenReturn(fakeGene); // ← 2nd check (double-check): HIT!

        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true); // Got lock

        // ACT
        Optional<GeneRecord> result = cacheService.getGeneByName("KRAS");

        // ASSERT
        assertThat(result).isPresent();
        assertEquals("KRAS", result.get().name());
        
        // Verify cache was checked TWICE (initial + double-check)
        verify(valueOperations, times(2)).get("gene:KRAS");
        
        // Verify NO database call (found in double-check!)
        verify(geneService, never()).getGeneByName(anyString());
        
        // Verify NO cache write (already cached!)
        verify(valueOperations, never()).set(anyString(), any(), any());
        
        // Verify counter incremented
        verify(meterRegistry).counter("cache.double_check.hits");
        
        // Verify lock was released
        verify(lock).unlock();
    }
}