package com.gene.sphere.geneservice.cache;

import org.junit.jupiter.api.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class ClearResultTest {

    @Test
    void success_ShouldCreateSuccessfulResultWithDeletions() {
        ClearResult success = ClearResult.success(150L, "gene*");
        assertTrue(success.isSuccessful());
        assertThat(success.deletedCount(), equalTo(150L));
        assertThat(success.pattern(), equalTo("gene*"));
        assertThat(success.message(), equalTo("Successfully cleared 150 cache entries matching pattern 'gene*'"));
        assertNotNull(success.timestamp());
    }

    @Test
    void success_ShouldCreateSuccessfulResultWithZeroDeletions() {
        ClearResult success = ClearResult.success(0L, "gene*");
        assertTrue(success.isSuccessful());
        assertThat(success.deletedCount(), equalTo(0L));
        assertThat(success.message(), equalTo("No cache entries found matching pattern 'gene*'"));
    }

    @Test
    void failure_ShouldCreateFailedResult() {
        ClearResult clearResult = ClearResult.failure("gene:*", "Connection timeout");
        assertFalse(clearResult.isSuccessful());
        assertThat(clearResult.deletedCount(), equalTo(0L));
        assertTrue(clearResult.message().contains("Failed to clear") || clearResult.message().contains("Connection timeout"));
        assertThat(clearResult.pattern(), equalTo("gene:*"));
    }

    @Test
    void partialSuccess_shouldPartiallyClearCache() {
        ClearResult clearResult = ClearResult.partialSuccess(10L, "gene:*", "Network error");
        assertTrue(clearResult.isSuccessful());
        assertThat(clearResult.deletedCount(), equalTo(10L));
        assertTrue(clearResult.message().contains("Partially cleared 10"));
    }

    @Test
    void constructor_ShouldThrowExceptionForNullPattern() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ClearResult(true, 10L, "TEST", 213123213L, null);
        });
        assertTrue(exception.getMessage().contains("Pattern cannot be null or blank"));
    }

    @Test
    void constructor_ShouldThrowExceptionForNegativeDeletedCount() {
        long deletedCount = -10L;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ClearResult(true, deletedCount, "TEST", 213123213L, "gene:*");
        });
        assertTrue(exception.getMessage().contains("Deleted count cannot be negative: " + deletedCount));
    }

    @Test
    void constructor_ShouldThrowExceptionForNullMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ClearResult(true, 10L, null, 213123213L, "gene*");
        });
        assertTrue(exception.getMessage().contains("Message cannot be null or blank"));
    }

    @Test
    void constructor_ShouldThrowExceptionForNegativeTimeStamp() {
        long timestamp = -1L;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ClearResult(true, 10L, "TEST", timestamp, "gene*");
        });
        assertTrue(exception.getMessage().contains("Timestamp must be positive: " + timestamp));
    }

    @Test
    void constructor_ShouldThrowExceptionForBlankMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ClearResult(true, 10L, "  ", 213123213L, "gene*");
        });
        assertTrue(exception.getMessage().contains("Message cannot be null or blank"));
    }

    @Test
    void constructor_ShouldThrowExceptionForBlankPattern() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ClearResult(true, 10L, "TEST", 213123213L, "");
        });
        assertTrue(exception.getMessage().contains("Pattern cannot be null or blank"));
    }

    @Test
    void constructor_ShouldThrowExceptionForZeroTimestamp() {
        long timestamp = 0L;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ClearResult(true, 10L, "TEST", timestamp, "gene:*");
        });
        assertTrue(exception.getMessage().contains("Timestamp must be positive: " + timestamp));
    }

    @Test
    void hasDeletedEntries_ShouldReturnTrueWhenCountGreaterThanZero() {
        long deletedCount = 10L;
        ClearResult result = new ClearResult(true, deletedCount, "TEST", 213123213L, "gene:*");
        assertTrue(result.hasDeletedEntries());
    }

    @Test
    void hasDeletedEntries_ShouldReturnFalseWhenCountIsZero() {
        long deletedCount = 0L;
        ClearResult result = new ClearResult(true, deletedCount, "TEST", 213123213L, "gene:*");
        assertFalse(result.hasDeletedEntries());
    }

    @Test
    void toString_ShouldContainAllFields() {
        long fixedTimestamp = 1234567890L;
        ClearResult result = new ClearResult(true, 150L, "Test message", fixedTimestamp, "gene:*");
        String toStringResult = result.toString();

        assertTrue(toStringResult.contains("success=true"));
        assertTrue(toStringResult.contains("deleted=150"));
        assertTrue(toStringResult.contains("pattern='gene:*'"));
        assertTrue(toStringResult.contains("timestamp=1234567890"));
        assertTrue(toStringResult.contains("message='Test message'"));
    }
}