package tools.jackson.databind.ext.javatime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link DateTimeFeature#ALWAYS_WRITE_SUBSECOND_DIGITS}.
 * <p>
 * Ported from the equivalent 2.x feature in
 * {@code jackson-modules-java8} (see
 * <a href="https://github.com/FasterXML/jackson-modules-java8/pull/386">modules-java8#386</a>,
 * fixing <a href="https://github.com/FasterXML/jackson-modules-java8/issues/76">modules-java8#76</a>).
 */
public class AlwaysWriteSubSecondDigitsTest extends DateTimeTestBase
{
    static class Wrapper {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public OffsetDateTime value;

        Wrapper(OffsetDateTime v) { value = v; }
    }

    private final ObjectMapper MAPPER = newMapperBuilder()
            .enable(DateTimeFeature.ALWAYS_WRITE_SUBSECOND_DIGITS)
            .build();

    private final ObjectMapper DEFAULT_MAPPER = newMapper();

    @Test
    public void testInstantZeroSubSecond() throws Exception
    {
        Instant value = Instant.parse("2017-09-14T04:28:48Z");
        // Default: sub-second field omitted entirely
        assertEquals(q("2017-09-14T04:28:48Z"), DEFAULT_MAPPER.writeValueAsString(value));
        // Enabled: zero-padded to millisecond precision
        assertEquals(q("2017-09-14T04:28:48.000Z"), MAPPER.writeValueAsString(value));
    }

    @Test
    public void testInstantHigherPrecisionNotTruncated() throws Exception
    {
        assertEquals(q("2017-09-14T04:28:48.100Z"),
                MAPPER.writeValueAsString(Instant.parse("2017-09-14T04:28:48.100Z")));
        assertEquals(q("2017-09-14T04:28:48.123456Z"),
                MAPPER.writeValueAsString(Instant.parse("2017-09-14T04:28:48.123456Z")));
        assertEquals(q("2017-09-14T04:28:48.123456789Z"),
                MAPPER.writeValueAsString(Instant.parse("2017-09-14T04:28:48.123456789Z")));
    }

    @Test
    public void testOffsetDateTime() throws Exception
    {
        OffsetDateTime value = OffsetDateTime.parse("2017-09-14T04:28:48+02:00");
        assertEquals(q("2017-09-14T04:28:48+02:00"), DEFAULT_MAPPER.writeValueAsString(value));
        assertEquals(q("2017-09-14T04:28:48.000+02:00"), MAPPER.writeValueAsString(value));

        // Note: the JDK ISO formatter renders 100 msec as ".1"; with the feature on,
        // width is stable at (at least) 3 digits
        OffsetDateTime millis = OffsetDateTime.parse("2017-09-14T04:28:48.100+02:00");
        assertEquals(q("2017-09-14T04:28:48.1+02:00"), DEFAULT_MAPPER.writeValueAsString(millis));
        assertEquals(q("2017-09-14T04:28:48.100+02:00"), MAPPER.writeValueAsString(millis));
    }

    @Test
    public void testZonedDateTime() throws Exception
    {
        ZonedDateTime value = ZonedDateTime.parse("2017-09-14T04:28:48+02:00[Europe/Budapest]");
        assertEquals(q("2017-09-14T04:28:48.000+02:00"), MAPPER.writeValueAsString(value));
    }

    @Test
    public void testZonedDateTimeWithZoneId() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .enable(DateTimeFeature.ALWAYS_WRITE_SUBSECOND_DIGITS)
                .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .build();
        ZonedDateTime value = ZonedDateTime.parse("2017-09-14T04:28:48+02:00[Europe/Budapest]");
        assertEquals(q("2017-09-14T04:28:48.000+02:00[Europe/Budapest]"),
                mapper.writeValueAsString(value));

        // Same path, feature disabled: zero sub-second is omitted, matching the JDK formatter
        ObjectMapper defaultZoneIdMapper = newMapperBuilder()
                .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .build();
        assertEquals(q("2017-09-14T04:28:48+02:00[Europe/Budapest]"),
                defaultZoneIdMapper.writeValueAsString(value));
    }

    @Test
    public void testLocalDateTime() throws Exception
    {
        LocalDateTime value = LocalDateTime.parse("2017-09-14T04:28:48");
        assertEquals(q("2017-09-14T04:28:48"), DEFAULT_MAPPER.writeValueAsString(value));
        assertEquals(q("2017-09-14T04:28:48.000"), MAPPER.writeValueAsString(value));

        // Seconds keep being written even when zero (as with the JDK ISO formatter)
        LocalDateTime noSeconds = LocalDateTime.parse("2017-09-14T04:28");
        assertEquals(q("2017-09-14T04:28:00"), DEFAULT_MAPPER.writeValueAsString(noSeconds));
        assertEquals(q("2017-09-14T04:28:00.000"), MAPPER.writeValueAsString(noSeconds));
    }

    // Feature must not leak into numeric timestamp serialization
    @Test
    public void testTimestampsUnaffected() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .enable(DateTimeFeature.ALWAYS_WRITE_SUBSECOND_DIGITS)
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        assertEquals("1505363328.000000000",
                mapper.writeValueAsString(Instant.parse("2017-09-14T04:28:48Z")));
    }

    // ... nor override an explicit `@JsonFormat` pattern
    @Test
    public void testExplicitPatternWins() throws Exception
    {
        assertEquals(a2q("{'value':'2017-09-14T04:28:48'}"),
                MAPPER.writeValueAsString(new Wrapper(OffsetDateTime.parse("2017-09-14T04:28:48Z"))));
    }

    // ... nor a default formatter passed to the serializer by the caller
    @Test
    public void testCallerProvidedDefaultFormatterWins() throws Exception
    {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy_MM_dd'X'HH:mm:ss");
        ObjectMapper mapper = newMapperBuilder()
                .addModule(new SimpleModule()
                        .addSerializer(new ZonedDateTimeSerializer(df)))
                .enable(DateTimeFeature.ALWAYS_WRITE_SUBSECOND_DIGITS)
                .build();
        assertEquals(q("2017_09_14X04:28:48"), mapper.writeValueAsString(
                ZonedDateTime.parse("2017-09-14T04:28:48+02:00[Europe/Budapest]")));
    }

    // ... including on the separate "write with Zone Id" path, where feature must
    // stay inert (even though that path does not use the caller's format either)
    @Test
    public void testCallerProvidedDefaultFormatterWithZoneId() throws Exception
    {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy_MM_dd'X'HH:mm:ss");
        ZonedDateTime value = ZonedDateTime.parse("2017-09-14T04:28:48+02:00[Europe/Budapest]");
        ObjectMapper mapper = newMapperBuilder()
                .addModule(new SimpleModule()
                        .addSerializer(new ZonedDateTimeSerializer(df)))
                .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .enable(DateTimeFeature.ALWAYS_WRITE_SUBSECOND_DIGITS)
                .build();
        ObjectMapper defaultMapper = newMapperBuilder()
                .addModule(new SimpleModule()
                        .addSerializer(new ZonedDateTimeSerializer(df)))
                .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .build();
        assertEquals(defaultMapper.writeValueAsString(value),
                mapper.writeValueAsString(value));
        assertEquals(q("2017-09-14T04:28:48+02:00[Europe/Budapest]"),
                mapper.writeValueAsString(value));
    }

    // Map keys are written by separate key serializers, not affected by the feature
    @Test
    public void testMapKeysUnaffected() throws Exception
    {
        assertEquals(a2q("{'2017-09-14T04:28:48Z':1}"),
                MAPPER.writeValueAsString(
                        Collections.singletonMap(Instant.parse("2017-09-14T04:28:48Z"), 1)));
        assertEquals(a2q("{'2017-09-14T04:28:48':1}"),
                MAPPER.writeValueAsString(
                        Collections.singletonMap(LocalDateTime.parse("2017-09-14T04:28:48"), 1)));
    }

    // Extremes of `Instant` range fall outside `LocalDate` range and cannot be
    // written using Date/Time fields: must retain default handling, not fail
    @Test
    public void testInstantExtremes() throws Exception
    {
        assertEquals(q("-1000000000-01-01T00:00:00Z"),
                MAPPER.writeValueAsString(Instant.MIN));
        assertEquals(DEFAULT_MAPPER.writeValueAsString(Instant.MIN),
                MAPPER.writeValueAsString(Instant.MIN));
        assertEquals(q("+1000000000-12-31T23:59:59.999999999Z"),
                MAPPER.writeValueAsString(Instant.MAX));
        assertEquals(DEFAULT_MAPPER.writeValueAsString(Instant.MAX),
                MAPPER.writeValueAsString(Instant.MAX));

        // But values just inside `LocalDate` range are still padded as usual
        Instant maxLocal = LocalDate.MAX.atStartOfDay().toInstant(ZoneOffset.UTC);
        assertEquals(q("+999999999-12-31T00:00:00.000Z"),
                MAPPER.writeValueAsString(maxLocal));
        Instant minLocal = LocalDate.MIN.atStartOfDay().toInstant(ZoneOffset.UTC);
        assertEquals(q("-999999999-01-01T00:00:00.000Z"),
                MAPPER.writeValueAsString(minLocal));
    }

    // Values written with the feature on must still be readable
    @Test
    public void testRoundTrip() throws Exception
    {
        for (String raw : new String[] {
                "2017-09-14T04:28:48Z", "2017-09-14T04:28:48.123456789Z",
                "1970-01-01T00:00:00Z", "+10000-09-14T04:28:48Z", "-0100-09-14T04:28:48Z" }) {
            Instant value = Instant.parse(raw);
            String json = MAPPER.writeValueAsString(value);
            assertEquals(value, MAPPER.readValue(json, Instant.class),
                    "Round-trip failed for " + raw + " (serialized as " + json + ")");
        }
    }
}
