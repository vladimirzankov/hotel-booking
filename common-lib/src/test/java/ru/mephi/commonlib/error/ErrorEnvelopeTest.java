package ru.mephi.commonlib.error;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorEnvelopeTest {

    @Test
    void of_createsEnvelope_withAllFields() {
        Map<String, Object> details = Map.of("field", "value");

        ErrorEnvelope envelope = ErrorEnvelope.of("ERR_CODE", "Error message", "req-123", details);

        assertEquals("ERR_CODE", envelope.code());
        assertEquals("Error message", envelope.message());
        assertEquals("req-123", envelope.requestId());
        assertNotNull(envelope.timestamp());
        assertEquals(details, envelope.details());
    }

    @Test
    void of_setsTimestamp_toCurrentTime() {
        Instant before = Instant.now().minusMillis(100);

        ErrorEnvelope envelope = ErrorEnvelope.of("CODE", "msg", "req", Map.of());

        Instant after = Instant.now().plusMillis(100);

        assertTrue(envelope.timestamp().isAfter(before));
        assertTrue(envelope.timestamp().isBefore(after));
    }

    @Test
    void of_handlesNullDetails_asEmptyMap() {
        ErrorEnvelope envelope = ErrorEnvelope.of("CODE", "msg", "req", null);

        assertNotNull(envelope.details());
        assertTrue(envelope.details().isEmpty());
    }

    @Test
    void of_handlesEmptyDetails() {
        ErrorEnvelope envelope = ErrorEnvelope.of("CODE", "msg", "req", Map.of());

        assertTrue(envelope.details().isEmpty());
    }

    @Test
    void of_handlesNullRequestId() {
        ErrorEnvelope envelope = ErrorEnvelope.of("CODE", "msg", null, Map.of());

        assertNull(envelope.requestId());
    }

    @Test
    void of_handlesNullCode() {
        ErrorEnvelope envelope = ErrorEnvelope.of(null, "msg", "req", Map.of());

        assertNull(envelope.code());
    }

    @Test
    void of_handlesNullMessage() {
        ErrorEnvelope envelope = ErrorEnvelope.of("CODE", null, "req", Map.of());

        assertNull(envelope.message());
    }

    @Test
    void of_preservesDetailsContent() {
        Map<String, Object> details = Map.of(
                "field1", "value1",
                "field2", 123,
                "field3", true
        );

        ErrorEnvelope envelope = ErrorEnvelope.of("CODE", "msg", "req", details);

        assertEquals("value1", envelope.details().get("field1"));
        assertEquals(123, envelope.details().get("field2"));
        assertEquals(true, envelope.details().get("field3"));
    }

    @Test
    void constructor_createsEnvelope_directly() {
        Instant timestamp = Instant.now();
        Map<String, Object> details = Map.of("key", "value");

        ErrorEnvelope envelope = new ErrorEnvelope(
                "DIRECT_CODE",
                "Direct message",
                "req-direct",
                timestamp,
                details
        );

        assertEquals("DIRECT_CODE", envelope.code());
        assertEquals("Direct message", envelope.message());
        assertEquals("req-direct", envelope.requestId());
        assertEquals(timestamp, envelope.timestamp());
        assertEquals(details, envelope.details());
    }

    @Test
    void record_hasCorrectEquality() {
        Instant timestamp = Instant.now();
        Map<String, Object> details = Map.of("k", "v");

        ErrorEnvelope e1 = new ErrorEnvelope("CODE", "msg", "req", timestamp, details);
        ErrorEnvelope e2 = new ErrorEnvelope("CODE", "msg", "req", timestamp, details);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void record_differentTimestamp_notEqual() {
        ErrorEnvelope e1 = new ErrorEnvelope("CODE", "msg", "req", Instant.now(), Map.of());
        ErrorEnvelope e2 = new ErrorEnvelope("CODE", "msg", "req", Instant.now().plusSeconds(1), Map.of());

        assertNotEquals(e1, e2);
    }

    @Test
    void toString_containsAllFields() {
        ErrorEnvelope envelope = ErrorEnvelope.of("TEST_CODE", "Test message", "req-456", Map.of());

        String str = envelope.toString();

        assertTrue(str.contains("TEST_CODE"));
        assertTrue(str.contains("Test message"));
        assertTrue(str.contains("req-456"));
    }
}
