package tools.jackson.databind.ext.javatime.misc;

import java.time.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to verify that explicit numeric {@link JsonFormat.Shape} determines
 * whether timestamps are written and read as nanoseconds, overriding
 * {@link DateTimeFeature#WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS} and
 * {@link DateTimeFeature#READ_DATE_TIMESTAMPS_AS_NANOSECONDS}; and that
 * values round-trip regardless of global settings.
 */
public class NumericShapeNanosecondsTest extends DateTimeTestBase
{
    static final Duration DURATION = Duration.ofSeconds(3, 500_000_000);
    static final Instant INSTANT = Instant.ofEpochSecond(3, 500_000_000);
    static final LocalTime LOCAL_TIME = LocalTime.of(1, 2, 3, 500_000_000);
    static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2026, 9, 23, 1, 2, 3, 500_000_000);
    static final OffsetTime OFFSET_TIME = OffsetTime.of(1, 2, 3, 500_000_000, ZoneOffset.UTC);

    static class IntBean {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public Duration duration = DURATION;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public Instant instant = INSTANT;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public LocalTime localTime = LOCAL_TIME;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public LocalDateTime localDateTime = LOCAL_DATE_TIME;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public OffsetTime offsetTime = OFFSET_TIME;
    }

    static class FloatBean {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public Duration duration = DURATION;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public Instant instant = INSTANT;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public LocalTime localTime = LOCAL_TIME;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public LocalDateTime localDateTime = LOCAL_DATE_TIME;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public OffsetTime offsetTime = OFFSET_TIME;
    }

    // [databind#6236]: `NUMBER_INT` means "not as nanoseconds" regardless of global settings
    @ParameterizedTest(name = "globalNanos={0}")
    @ValueSource(booleans = { false, true })
    public void shapeIntRoundTrip(boolean globalNanos) throws Exception
    {
        ObjectMapper mapper = _mapper(globalNanos);
        String json = mapper.writeValueAsString(new IntBean());
        assertEquals("""
                {"duration":3500,"instant":3500,"localDateTime":[2026,9,23,1,2,3,500],\
                "localTime":[1,2,3,500],"offsetTime":[1,2,3,500,"Z"]}""", json);

        IntBean result = mapper.readValue(json, IntBean.class);
        assertEquals(DURATION, result.duration);
        assertEquals(INSTANT, result.instant);
        assertEquals(LOCAL_TIME, result.localTime);
        assertEquals(LOCAL_DATE_TIME, result.localDateTime);
        assertEquals(OFFSET_TIME, result.offsetTime);
    }

    // [databind#6236]: `NUMBER_FLOAT` means "as nanoseconds" regardless of global settings
    @ParameterizedTest(name = "globalNanos={0}")
    @ValueSource(booleans = { false, true })
    public void shapeFloatRoundTrip(boolean globalNanos) throws Exception
    {
        ObjectMapper mapper = _mapper(globalNanos);
        String json = mapper.writeValueAsString(new FloatBean());
        assertEquals("""
                {"duration":3.500000000,"instant":3.500000000,\
                "localDateTime":[2026,9,23,1,2,3,500000000],\
                "localTime":[1,2,3,500000000],"offsetTime":[1,2,3,500000000,"Z"]}""", json);

        FloatBean result = mapper.readValue(json, FloatBean.class);
        assertEquals(DURATION, result.duration);
        assertEquals(INSTANT, result.instant);
        assertEquals(LOCAL_TIME, result.localTime);
        assertEquals(LOCAL_DATE_TIME, result.localDateTime);
        assertEquals(OFFSET_TIME, result.offsetTime);
    }

    private ObjectMapper _mapper(boolean globalNanos) {
        return mapperBuilder()
                .configure(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, globalNanos)
                .configure(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, globalNanos)
                .build();
    }
}
