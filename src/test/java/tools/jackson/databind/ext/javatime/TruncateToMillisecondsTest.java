package tools.jackson.databind.ext.javatime;

import java.time.*;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DateTimeFeature#TRUNCATE_TO_MSECS_ON_READ} and
 * {@link DateTimeFeature#TRUNCATE_TO_MSECS_ON_WRITE} features.
 */
public class TruncateToMillisecondsTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_TRUNCATE_WRITE = jsonMapperBuilder()
        .enable(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)
        .build();

    private final ObjectMapper MAPPER_TRUNCATE_READ = jsonMapperBuilder()
        .enable(DateTimeFeature.TRUNCATE_TO_MSECS_ON_READ)
        .build();

    private final ObjectMapper MAPPER_TRUNCATE_BOTH = jsonMapperBuilder()
        .enable(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)
        .enable(DateTimeFeature.TRUNCATE_TO_MSECS_ON_READ)
        .build();

    private final ObjectMapper MAPPER_WRITE_AS_TIMESTAMPS = jsonMapperBuilder()
        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    private final ObjectMapper MAPPER_TRUNCATE_WRITE_AS_TIMESTAMPS = jsonMapperBuilder()
        .enable(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)
        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    // Serialization tests

    @Test
    public void testInstantSerializationTruncation() throws Exception {
        Instant instant = Instant.parse("2023-01-15T10:30:45.123456789Z");

        // Without truncation - preserves full nanoseconds
        String json1 = MAPPER.writeValueAsString(instant);
        assertTrue(json1.contains("123456789") || json1.contains("45.123456789"),
            "Expected nanoseconds in: " + json1);

        // With truncation - only milliseconds (zeros out extra nanoseconds)
        String json2 = MAPPER_TRUNCATE_WRITE.writeValueAsString(instant);
        assertTrue(json2.contains("123") && !json2.contains("456789"),
            "Expected truncated value in: " + json2);
    }

    @Test
    public void testLocalDateTimeSerializationTruncation() throws Exception {
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 15, 10, 30, 45, 123456789);

        // Without truncation
        String json1 = MAPPER.writeValueAsString(ldt);
        assertTrue(json1.contains("123456789") || json1.contains("45.123456789"),
            "Expected nanoseconds in: " + json1);

        // With truncation
        String json2 = MAPPER_TRUNCATE_WRITE.writeValueAsString(ldt);
        assertFalse(json2.contains("456789"), "Expected truncated value in: " + json2);
    }

    @Test
    public void testLocalTimeSerializationTruncation() throws Exception {
        LocalTime time = LocalTime.of(10, 30, 45, 123456789);

        // Without truncation
        String json1 = MAPPER.writeValueAsString(time);
        assertTrue(json1.contains("123456789") || json1.contains("45.123456789"),
            "Expected nanoseconds in: " + json1);

        // With truncation
        String json2 = MAPPER_TRUNCATE_WRITE.writeValueAsString(time);
        assertFalse(json2.contains("456789"), "Expected truncated value in: " + json2);
    }

    @Test
    public void testDurationSerializationTruncation() throws Exception {
        Duration duration = Duration.ofSeconds(123, 456789012);

        // Without truncation
        String json1 = MAPPER.writeValueAsString(duration);
        assertTrue(json1.contains("456789012") || json1.contains(".456789012"),
            "Expected nanoseconds in: " + json1);

        // With truncation
        String json2 = MAPPER_TRUNCATE_WRITE.writeValueAsString(duration);
        assertTrue(json2.contains("456") || json2.contains(".456"),
            "Expected milliseconds in: " + json2);
        assertFalse(json2.contains("789012"), "Expected truncated value in: " + json2);
    }

    // Deserialization tests

    @Test
    public void testInstantDeserializationTruncation() throws Exception {
        String json = "\"2023-01-15T10:30:45.123456789Z\"";

        // Without truncation - preserves full nanoseconds
        Instant instant1 = MAPPER.readValue(json, Instant.class);
        assertEquals(123456789, instant1.getNano());

        // With truncation - only milliseconds
        Instant instant2 = MAPPER_TRUNCATE_READ.readValue(json, Instant.class);
        assertEquals(123000000, instant2.getNano());
    }

    @Test
    public void testLocalDateTimeDeserializationTruncation() throws Exception {
        String json = "\"2023-01-15T10:30:45.123456789\"";

        // Without truncation
        LocalDateTime ldt1 = MAPPER.readValue(json, LocalDateTime.class);
        assertEquals(123456789, ldt1.getNano());

        // With truncation
        LocalDateTime ldt2 = MAPPER_TRUNCATE_READ.readValue(json, LocalDateTime.class);
        assertEquals(123000000, ldt2.getNano());
    }

    @Test
    public void testLocalTimeDeserializationTruncation() throws Exception {
        String json = "\"10:30:45.123456789\"";

        // Without truncation
        LocalTime time1 = MAPPER.readValue(json, LocalTime.class);
        assertEquals(123456789, time1.getNano());

        // With truncation
        LocalTime time2 = MAPPER_TRUNCATE_READ.readValue(json, LocalTime.class);
        assertEquals(123000000, time2.getNano());
    }

    @Test
    public void testDurationDeserializationTruncation() throws Exception {
        String json = "\"PT123.456789012S\"";

        // Without truncation
        Duration duration1 = MAPPER.readValue(json, Duration.class);
        assertEquals(456789012, duration1.getNano());

        // With truncation
        Duration duration2 = MAPPER_TRUNCATE_READ.readValue(json, Duration.class);
        assertEquals(456000000, duration2.getNano());
    }

    // Round-trip tests

    @Test
    public void testRoundTripWithBothFeaturesEnabled() throws Exception {
        Instant original = Instant.parse("2023-01-15T10:30:45.123456789Z");

        // Serialize with truncation
        String json = MAPPER_TRUNCATE_BOTH.writeValueAsString(original);

        // Deserialize with truncation
        Instant result = MAPPER_TRUNCATE_BOTH.readValue(json, Instant.class);

        // Result should have milliseconds only
        assertEquals(123000000, result.getNano());
        assertEquals(original.getEpochSecond(), result.getEpochSecond());
    }

    @Test
    public void testRoundTripSerializeTruncateDeserializeWithout() throws Exception {
        LocalDateTime original = LocalDateTime.of(2023, 1, 15, 10, 30, 45, 123456789);

        // Serialize with truncation
        String json = MAPPER_TRUNCATE_WRITE.writeValueAsString(original);

        // Deserialize without truncation (value is already truncated)
        LocalDateTime result = MAPPER.readValue(json, LocalDateTime.class);

        // Result should still have milliseconds only (from serialization truncation)
        assertEquals(123000000, result.getNano());
    }

    // Test with numeric timestamp format

    @Test
    public void testInstantNumericTimestampTruncation() throws Exception {
        Instant instant = Instant.parse("2023-01-15T10:30:45.123456789Z");

        // Serialize as numeric timestamp with truncation
        String json = MAPPER_TRUNCATE_WRITE_AS_TIMESTAMPS.writeValueAsString(instant);

        // The numeric value should not contain nanoseconds beyond milliseconds
        Instant result = MAPPER.readValue(json, Instant.class);
        assertTrue(result.getNano() % 1_000_000 == 0,
            "Nanoseconds should be multiple of 1,000,000: " + result.getNano());
    }

    // Test values already at millisecond precision

    @Test
    public void testAlreadyTruncatedValueRemains() throws Exception {
        // Value already at millisecond precision
        Instant instant = Instant.parse("2023-01-15T10:30:45.123Z");
        assertEquals(123000000, instant.getNano());

        // Truncation should not change it
        String json = MAPPER_TRUNCATE_WRITE.writeValueAsString(instant);
        Instant result = MAPPER_TRUNCATE_READ.readValue(json, Instant.class);

        assertEquals(123000000, result.getNano());
        assertEquals(instant, result);
    }

    // Test edge cases

    @Test
    public void testZeroNanosecondsRemains() throws Exception {
        Instant instant = Instant.parse("2023-01-15T10:30:45Z");
        assertEquals(0, instant.getNano());

        // Truncation should not change it
        String json = MAPPER_TRUNCATE_WRITE.writeValueAsString(instant);
        Instant result = MAPPER_TRUNCATE_READ.readValue(json, Instant.class);

        assertEquals(0, result.getNano());
        assertEquals(instant, result);
    }

    @Test
    public void testNegativeDurationTruncation() throws Exception {
        // Create a negative duration: -5 seconds
        Duration duration = Duration.ofSeconds(-5);

        // Serialize with truncation (should not change since it has no fractional part)
        String json = MAPPER_TRUNCATE_WRITE.writeValueAsString(duration);

        // Deserialize with truncation
        Duration result = MAPPER_TRUNCATE_READ.readValue(json, Duration.class);

        // Should preserve the negative duration
        assertEquals(-5, result.getSeconds());
        assertEquals(0, result.getNano());

        // Test with negative duration that has fractional seconds
        Duration duration2 = Duration.ofMillis(-5123).plusNanos(456789);
        String json2 = MAPPER_TRUNCATE_WRITE.writeValueAsString(duration2);
        Duration result2 = MAPPER_TRUNCATE_READ.readValue(json2, Duration.class);

        // Should truncate to milliseconds
        assertTrue(result2.toNanos() % 1_000_000 == 0,
            "Expected millisecond precision for negative duration");
    }

    // Test independence from READ/WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS

    @Test
    public void testTruncationIndependentOfNanosecondFeature() throws Exception {
        ObjectMapper mapperNanosOff = jsonMapperBuilder()
            .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .enable(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

        Instant instant = Instant.parse("2023-01-15T10:30:45.123456789Z");

        // Even with nanoseconds disabled, truncation should still occur
        String json = mapperNanosOff.writeValueAsString(instant);
        Instant result = MAPPER.readValue(json, Instant.class);

        // Result should have only millisecond precision
        assertTrue(result.getNano() % 1_000_000 == 0,
            "Expected millisecond precision: " + result.getNano());
    }

    // Test with OffsetDateTime and ZonedDateTime

    @Test
    public void testOffsetDateTimeTruncation() throws Exception {
        OffsetDateTime odt = OffsetDateTime.parse("2023-01-15T10:30:45.123456789+01:00");

        // Serialize with truncation
        String json = MAPPER_TRUNCATE_WRITE.writeValueAsString(odt);

        // Deserialize with truncation
        OffsetDateTime result = MAPPER_TRUNCATE_READ.readValue(json, OffsetDateTime.class);

        assertEquals(123000000, result.getNano());
    }

    @Test
    public void testZonedDateTimeTruncation() throws Exception {
        ZonedDateTime zdt = ZonedDateTime.parse("2023-01-15T10:30:45.123456789+01:00[Europe/Paris]");

        // Serialize with truncation
        String json = MAPPER_TRUNCATE_WRITE.writeValueAsString(zdt);

        // Deserialize with truncation
        ZonedDateTime result = MAPPER_TRUNCATE_READ.readValue(json, ZonedDateTime.class);

        assertEquals(123000000, result.getNano());
    }
}
