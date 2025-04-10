package tools.jackson.databind.ext.javatime.ser;

import java.time.Duration;
import java.time.temporal.TemporalAmount;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class DurationSerTest extends DateTimeTestBase
{
    private final ObjectWriter WRITER = newMapper().writer();

    // [datetime#224]
    static class MyDto224 {
        @JsonFormat(pattern = "MINUTES"
              // Work-around from issue:
//              , without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        @JsonProperty("mins")
        final Duration duration;

        public MyDto224(Duration d) { duration = d; }

        public Duration getDuration() { return duration; }
    }

    // [datetime#282]
    static class Bean282 {
        @JsonFormat(pattern = "SECONDS")
        public Duration duration;

        public Bean282(Duration d) { duration = d; }
    }

    @Test
    public void testSerializationAsTimestampNanoseconds01() throws Exception
    {
        Duration duration = Duration.ofSeconds(60L, 0);
        String value = WRITER
                .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(duration);
        assertEquals("60"+NO_NANOSECS_SUFFIX, value);
    }

    @Test
    public void testSerializationAsTimestampNanoseconds02() throws Exception
    {
        Duration duration = Duration.ofSeconds(13498L, 8374);
        String value = WRITER
                .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(duration);
        assertEquals("13498.000008374", value);
    }

    // [modules-java8#165]
    @Test
    public void testSerializationAsTimestampNanoseconds03() throws Exception
    {
        ObjectWriter w = WRITER
                .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);

        // 20-Oct-2020, tatu: Very weird, but "use nanoseconds" actually results
        //   in unit being seconds, with fractions (with nanosec precision)
        String value = w.writeValueAsString(Duration.ofMillis(1L));
        assertEquals("0.001000000", value);

        value = w.writeValueAsString(Duration.ofMillis(-1L));
        assertEquals("-0.001000000", value);
    }
    
    @Test
    public void testSerializationAsTimestampMilliseconds01() throws Exception
    {
        final ObjectWriter w = WRITER
                .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        String value = w.writeValueAsString(Duration.ofSeconds(45L, 0));
        assertEquals("45000", value);

        // and with negative value too
        value = w.writeValueAsString(Duration.ofSeconds(-32L, 0));
        assertEquals("-32000", value);
    }

    @Test
    public void testSerializationAsTimestampMilliseconds02() throws Exception
    {
        String value = WRITER
                .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(Duration.ofSeconds(13498L, 8374));
        assertEquals("13498000", value);
    }

    @Test
    public void testSerializationAsTimestampMilliseconds03() throws Exception
    {
        Duration duration = Duration.ofSeconds(13498L, 837481723);
        String value = WRITER
                .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(duration);
        assertEquals("13498837", value);
    }

    @Test
    public void testSerializationAsString01() throws Exception
    {
        Duration duration = Duration.ofSeconds(60L, 0);
        String value = WRITER
                .without(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .writeValueAsString(duration);
        assertEquals(q(duration.toString()), value);
    }

    @Test
    public void testSerializationAsString02() throws Exception
    {
        Duration duration = Duration.ofSeconds(13498L, 8374);
        String value = WRITER
                .without(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .writeValueAsString(duration);
        assertEquals(q(duration.toString()), value);
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
                .enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .enable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .build();
        Duration duration = Duration.ofSeconds(13498L, 8374);
        String value = mapper.writeValueAsString(duration);

        assertEquals("[\"" + Duration.class.getName() + "\",13498.000008374]", value);
    }

    @Test
    public void testSerializationWithTypeInfo02() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
                .enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .build();
        Duration duration = Duration.ofSeconds(13498L, 837481723);
        String value = mapper.writeValueAsString(duration);

        assertEquals("[\"" + Duration.class.getName() + "\",13498837]", value);
    }

    @Test
    public void testSerializationWithTypeInfo03() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .build();
        Duration duration = Duration.ofSeconds(13498L, 8374);
        String value = mapper.writeValueAsString(duration);

        assertEquals("[\"" + Duration.class.getName() + "\",\"" + duration.toString() + "\"]", value);
    }

    /*
    /**********************************************************
    /* Tests for custom patterns (modules-java8#189)
    /**********************************************************
     */

    @Test
    public void shouldSerializeInNanos_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("NANOS");
        assertEquals("3600000000000", mapper.writeValueAsString(Duration.ofHours(1)));
    }

    @Test
    public void shouldSerializeInMicros_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("MICROS");
        assertEquals("1000", mapper.writeValueAsString(Duration.ofMillis(1)));
    }

    @Test
    public void shouldSerializeInMicrosDiscardingFractions_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("MICROS");
        assertEquals("1", mapper.writeValueAsString(Duration.ofNanos(1500)));
    }

    @Test
    public void shouldSerializeInMillis_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("MILLIS");
        assertEquals("1000", mapper.writeValueAsString(Duration.ofSeconds(1)));
    }

    @Test
    public void shouldSerializeInMillisDiscardingFractions_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("MILLIS");
        assertEquals("1", mapper.writeValueAsString(Duration.ofNanos(1500000)));
    }

    @Test
    public void shouldSerializeInSeconds_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("SECONDS");
        assertEquals("60", mapper.writeValueAsString(Duration.ofMinutes(1)));
    }

    @Test
    public void shouldSerializeInSecondsDiscardingFractions_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("SECONDS");
        assertEquals("1", mapper.writeValueAsString(Duration.ofMillis(1500)));
    }

    @Test
    public void shouldSerializeInMinutes_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("MINUTES");
        assertEquals("60", mapper.writeValueAsString(Duration.ofHours(1)));
    }

    @Test
    public void shouldSerializeInMinutesDiscardingFractions_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("MINUTES");
        assertEquals("1", mapper.writeValueAsString(Duration.ofSeconds(90)));
    }

    @Test
    public void shouldSerializeInHours_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("HOURS");
        assertEquals("24", mapper.writeValueAsString(Duration.ofDays(1)));
    }

    @Test
    public void shouldSerializeInHoursDiscardingFractions_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("HOURS");
        assertEquals("1", mapper.writeValueAsString(Duration.ofMinutes(90)));
    }

    @Test
    public void shouldSerializeInHalfDays_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("HALF_DAYS");
        assertEquals("2", mapper.writeValueAsString(Duration.ofDays(1)));
    }

    @Test
    public void shouldSerializeInHalfDaysDiscardingFractions_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("DAYS");
        assertEquals("1", mapper.writeValueAsString(Duration.ofHours(30)));
    }

    @Test
    public void shouldSerializeInDays_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("DAYS");
        assertEquals("1", mapper.writeValueAsString(Duration.ofDays(1)));
    }

    @Test
    public void shouldSerializeInDaysDiscardingFractions_whenSetAsPattern() throws Exception
    {
        ObjectMapper mapper = _mapperForPatternOverride("DAYS");
        assertEquals("1", mapper.writeValueAsString(Duration.ofHours(36)));
    }

    protected ObjectMapper _mapperForPatternOverride(String pattern) {
        ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(Duration.class,
                        cfg -> cfg.setFormat(JsonFormat.Value.forPattern(pattern)))
                .enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .build();
        return mapper;
    }

    // [datetime#224]
    @Test
    public void testDurationFormatOverrideMinutes() throws Exception
    {
        assertEquals(a2q("{'mins':120}"),
                WRITER
                    .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                    .writeValueAsString(new MyDto224(Duration.ofHours(2))));
    }

    // [datetime#282]
    @Test
    public void testDurationFormatOverrideSeconds() throws Exception
    {
        final Duration maxDuration = Duration.ofSeconds(Long.MIN_VALUE);
        assertEquals(a2q("{'duration':"+Long.MIN_VALUE+"}"),
                WRITER
                    .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                    .writeValueAsString(new Bean282(maxDuration)));
    }
}
