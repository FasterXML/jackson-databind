package tools.jackson.databind.ext.javatime.ser;

import java.time.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to verify that explicit numeric {@link JsonFormat.Shape} determines
 * whether nanoseconds are written, overriding
 * {@link DateTimeFeature#WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS}.
 */
public class NumericShapeNanosecondsTest extends DateTimeTestBase
{
    static class IntBean {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public Duration duration = Duration.ofSeconds(3, 500_000_000);
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public LocalTime localTime = LocalTime.of(1, 2, 3, 500_000_000);
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public LocalDateTime localDateTime = LocalDateTime.of(2026, 9, 23, 1, 2, 3, 500_000_000);
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public OffsetTime offsetTime = OffsetTime.of(1, 2, 3, 500_000_000, ZoneOffset.UTC);
    }

    static class FloatBean {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public Duration duration = Duration.ofSeconds(3, 500_000_000);
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public LocalTime localTime = LocalTime.of(1, 2, 3, 500_000_000);
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public LocalDateTime localDateTime = LocalDateTime.of(2026, 9, 23, 1, 2, 3, 500_000_000);
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
        public OffsetTime offsetTime = OffsetTime.of(1, 2, 3, 500_000_000, ZoneOffset.UTC);
    }

    // [databind#6236]: `NUMBER_INT` means no nanoseconds, even if enabled globally
    @Test
    public void shapeIntOverridesNanosecondsEnabled() throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .enable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .build();
        assertEquals("""
                {"duration":3500,"localDateTime":[2026,9,23,1,2,3,500],\
                "localTime":[1,2,3,500],"offsetTime":[1,2,3,500,"Z"]}""",
                mapper.writeValueAsString(new IntBean()));
    }

    // [databind#6236]: `NUMBER_FLOAT` means nanoseconds, even if disabled globally
    @Test
    public void shapeFloatOverridesNanosecondsDisabled() throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .build();
        assertEquals("""
                {"duration":3.500000000,"localDateTime":[2026,9,23,1,2,3,500000000],\
                "localTime":[1,2,3,500000000],"offsetTime":[1,2,3,500000000,"Z"]}""",
                mapper.writeValueAsString(new FloatBean()));
    }
}
