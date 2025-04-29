package tools.jackson.databind.ext.javatime.ser;

import java.time.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class WriteNanosecondsTest extends DateTimeTestBase
{
    public static final ZoneId UTC = ZoneId.of("UTC");

    // 05-Feb-2025, tatu: Use Jackson 2.x defaults wrt as-timestamps
    //   serialization
    private final static ObjectMapper MAPPER = mapperBuilder()
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public static class DummyClass<T> {
        @JsonFormat(with = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        private final T nanoseconds;

        @JsonFormat(without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        private final T notNanoseconds;

        DummyClass(T t) {
            this.nanoseconds = t;
            this.notNanoseconds = t;
        }
    }

    @Test
    public void testSerializeDurationWithAndWithoutNanoseconds() throws Exception {
        DummyClass<Duration> value = new DummyClass<>(Duration.ZERO);

        String json = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .writeValueAsString(value);

        assertThat(json).contains("\"nanoseconds\":0.0");
        assertThat(json).contains("\"notNanoseconds\":0");
    }

    @Test
    public void testSerializeInstantWithAndWithoutNanoseconds() throws Exception {
        DummyClass<Instant> input = new DummyClass<>(Instant.EPOCH);

        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("\"nanoseconds\":0.0"));
        assertTrue(json.contains("\"notNanoseconds\":0"));
    }

    @Test
    public void testSerializeLocalDateTimeWithAndWithoutNanoseconds() throws Exception {
        DummyClass<LocalDateTime> input = new DummyClass<>(
                // Nanos will only be written if it's non-zero
                LocalDateTime.of(1970, 1, 1, 0, 0, 0, 1)
        );

        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("\"nanoseconds\":[1970,1,1,0,0,0,1]"));
        assertTrue(json.contains("\"notNanoseconds\":[1970,1,1,0,0,0,0]"));
    }

    @Test
    public void testSerializeLocalTimeWithAndWithoutNanoseconds() throws Exception {
        DummyClass<LocalTime> input = new DummyClass<>(
                // Nanos will only be written if it's non-zero
                LocalTime.of(0, 0, 0, 1)
        );

        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("\"nanoseconds\":[0,0,0,1]"));
        assertTrue(json.contains("\"notNanoseconds\":[0,0,0,0]"));
    }

    @Test
    public void testSerializeOffsetDateTimeWithAndWithoutNanoseconds() throws Exception {
        DummyClass<OffsetDateTime> input = new DummyClass<>(OffsetDateTime.ofInstant(Instant.EPOCH, UTC));

        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("\"nanoseconds\":0.0"));
        assertTrue(json.contains("\"notNanoseconds\":0"));
    }

    @Test
    public void testSerializeOffsetTimeWithAndWithoutNanoseconds() throws Exception {
        DummyClass<OffsetTime> input = new DummyClass<>(
                // Nanos will only be written if it's non-zero
                OffsetTime.of(0,0,0, 1 , ZoneOffset.UTC)
        );

        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("\"nanoseconds\":[0,0,0,1,\"Z\"]"));
        assertTrue(json.contains("\"notNanoseconds\":[0,0,0,0,\"Z\"]"));
    }

    @Test
    public void testSerializeZonedDateTimeWithAndWithoutNanoseconds() throws Exception {
        DummyClass<ZonedDateTime> input = new DummyClass<>(ZonedDateTime.ofInstant(Instant.EPOCH, UTC));

        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("\"nanoseconds\":0.0"));
        assertTrue(json.contains("\"notNanoseconds\":0"));
    }
}
