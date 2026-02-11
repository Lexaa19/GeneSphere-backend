package com.gene.sphere.geneservice.cache;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class CacheStatusTest {

    @Test
    void constructor_ShouldCreateCacheStatusWithProvidedValues() {
        long fixedTimeStamp = 1234567890L;
        CacheStatus status = new CacheStatus(100L, 80L, 20L, fixedTimeStamp, "AVAILABLE");

        String result = status.toString();
        assertTrue(result.contains("total=100"));
        assertTrue(result.contains("timestamp=1234567890"));
    }

    @Test
    void of_ShouldSetTimestampAutomatically() {
        long before = System.currentTimeMillis();
        CacheStatus status = CacheStatus.of(100L, 80L, 20L, "AVAILABLE");
        long after = System.currentTimeMillis();
        long ts = status.timestamp();
        assertTrue(ts >= before && ts <= after);
    }

    @Test
    void empty_ShouldReturnUnavailableWithZeroCounts() {
        long before = System.currentTimeMillis();
        CacheStatus status = CacheStatus.empty();
        long after = System.currentTimeMillis();
        long ts = status.timestamp();

        assertTrue(ts >= before && ts <= after);
        assertEquals(status.totalKeys(), 0L);
        assertEquals(status.status(), "UNAVAILABLE");
        assertEquals(status.geneKeys(), 0L);

        String result = status.toString();
        assertTrue(result.contains("status=UNAVAILABLE"));
        assertTrue(result.contains("total=0"));
        assertTrue(result.contains("genes=0"));
        assertTrue(result.contains("mutations=0"));
    }

    @Test
    void isCacheAvailable_shouldReturnTrue() {
        CacheStatus status = CacheStatus.of(100L, 80L, 20L, "AVAILABLE");
        assertTrue(status.isAvailable());
    }

    @Test
    void isCacheAvailable_shouldReturnFalse() {
        CacheStatus status = CacheStatus.of(100L, 80L, 20L, "UNAVAILABLE");
        assertFalse(status.isAvailable());
    }

    @Test
    void isCacheAvailable_shouldReturnFalse_NullState() {
        CacheStatus status = CacheStatus.of(100L, 80L, 20L, null);
        assertFalse(status.isAvailable());
    }

    @Test
    void isCacheAvailable_shouldReturnFalse_UnknownState() {
        CacheStatus status = CacheStatus.of(100L, 80L, 20L, "UNKNOWN");
        assertFalse(status.isAvailable());
    }

    @Test
    void areDtosEqual_shouldReturnTrue() {
        long fixedTimestamp = 1234567890L;
        CacheStatus status = new CacheStatus(100L, 80L, 20L, fixedTimestamp, "UNKNOWN");
        CacheStatus secondStatus = new CacheStatus(100L, 80L, 20L, fixedTimestamp, "UNKNOWN");
        assertEquals(status, secondStatus);
    }

    @Test
    void recordsWithDifferentValues_ShouldNotBeEqual() {
        CacheStatus status = CacheStatus.of(100L, 80L, 20L, "AVAILABLE");
        CacheStatus secondStatus = CacheStatus.of(100L, 180L, 120L, "AVAILABLE");
        assertNotEquals(status, secondStatus);
    }

    @Test
    void areDtosHashesAreEqual_shouldReturnTrue() {
        // if a fixed time stamp is not added, the hash will fail as it takes different times (even milliseconds apart count)
        long fixedTimestamp = 1234567890L;
        CacheStatus status = new CacheStatus(100L, 80L, 20L, fixedTimestamp, "UNKNOWN");
        CacheStatus secondStatus = new CacheStatus(100L, 80L, 20L, fixedTimestamp, "UNKNOWN");
        assertEquals(status.hashCode(), secondStatus.hashCode());
    }

    @Test
    void toString_ShouldContainAllFields_WhenStatusIsAvailable() {
        // Arrange - use constructor with fixed values for predictable testing
        long fixedTimestamp = 1234567890L;
        CacheStatus status = new CacheStatus(100L, 80L, 20L, fixedTimestamp, "AVAILABLE");

        // Act
        String result = status.toString();

        // Assert - verify all fields are present in the string
        assertTrue(result.contains("total=100"));
        assertTrue(result.contains("genes=80"));
        assertTrue(result.contains("mutations=20"));
        assertTrue(result.contains("status=AVAILABLE"));
        assertTrue(result.contains("timestamp=1234567890"));
    }
}