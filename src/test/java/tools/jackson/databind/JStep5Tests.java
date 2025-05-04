package tools.jackson.databind;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.cfg.DateTimeFeature;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JStep5Tests extends tools.jackson.databind.ext.javatime.DateTimeTestBase
{
    static class Wrapper<T> {
        @JsonFormat(pattern="yyyy/MM/dd'T'HH-mm-ss", shape=JsonFormat.Shape.STRING)
        public T value;
        public Wrapper() { }
        public Wrapper(T v) { value = v; }
    }

    private static ObjectMapper withTimestampMapper() {
        return mapperBuilder()
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    private static ObjectMapper withoutTimestampMapper() {
        return mapperBuilder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Test
    public void testWriteDatesAsTimeStmaps() throws Exception {
        // java.time.OffsetDateTime
        _testTimestamp(
                LocalDateTime.of(2025, 5, 4, 18, 1, 0),
                LocalDateTime.class,
                "[2025,5,4,18,1]", // WRONG? Why not [2025,5,4,18,1,0]?
                "\"2025-05-04T18:01:00\""
        );
        // java.time.ZonedDateTime
        _testTimestamp(
                LocalDateTime.of(2025, 5, 4, 18, 1, 2),
                LocalDateTime.class,
                "[2025,5,4,18,1,2]",
                "\"2025-05-04T18:01:02\""
        );
        // java.time.LocalDate
        _testTimestamp(
                LocalDate.of(2025, 5, 4),
                LocalDate.class,
                "[2025,5,4]",
                "\"2025-05-04\""
        );
        // java.time.LocalTime
        _testTimestamp(
                LocalTime.of(18, 1, 2),
                LocalTime.class,
                "[18,1,2]",
                "\"18:01:02\""
        );
        // java.time.Instant
        _testTimestamp(
                Instant.ofEpochMilli(1234567890123L),
                Instant.class,
                "[1234567890123]",
                "\"2009-02-13T23:31:30.123Z\""
        );
        // java.time.ZoneId
        _testTimestamp(
                ZoneId.of("UTC"),
                ZoneId.class,
                "\"UTC\"",
                "\"UTC\""
        );
        // java.time.ZoneOffset
        _testTimestamp(
                ZoneOffset.ofHours(2),
                ZoneOffset.class,
                "\"+02:00\"",
                "\"+02:00\""
        );
        // java.time.Duration
        _testTimestamp(
                Duration.ofHours(2),
                Duration.class,
                "[7200000]",
                "\"PT2H\""
        );
        // java.time.Period
        _testTimestamp(
                Period.of(2025, 5, 4),
                Period.class,
                "[2025,5,4]",
                "\"P2025Y5M4D\""
        );
        // java.time.Year
        _testTimestamp(
                Year.of(2025),
                Year.class,
                "[2025]",
                "\"2025\""
        );
        // java.time.YearMonth
        _testTimestamp(
                YearMonth.of(2025, 5),
                YearMonth.class,
                "[2025,5]",
                "\"2025-05\""
        );
        // java.time.MonthDay
        _testTimestamp(
                MonthDay.of(5, 4),
                MonthDay.class,
                "[5,4]",
                "\"--05-04\""
        );
        // java.time.OffsetTime
        _testTimestamp(
                OffsetTime.of(18, 1, 2, 0, ZoneOffset.UTC),
                OffsetTime.class,
                "[18,1,2,0]",
                "\"18:01:02Z\""
        );
        // java.time.OffsetDateTime
        _testTimestamp(
                OffsetDateTime.of(2025, 5, 4, 18, 1, 2, 0, ZoneOffset.UTC),
                OffsetDateTime.class,
                "[2025,5,4,18,1,2,0]",
                "\"2025-05-04T18:01:02Z\""
        );
        // java.time.ZonedDateTime
        _testTimestamp(
                ZonedDateTime.of(2025, 5, 4, 18, 1, 2, 0, ZoneOffset.UTC),
                ZonedDateTime.class,
                "[2025,5,4,18,1,2,0]",
                "\"2025-05-04T18:01:02Z\""
        );
        // java.time.LocalDateTime
        _testTimestamp(
                LocalDateTime.of(2025, 5, 4, 18, 1, 2),
                LocalDateTime.class,
                "[2025,5,4,18,1,2]",
                "\"2025-05-04T18:01:02\""
        );
    }

    private <T> void  _testTimestamp(T value, Class<?> clazz, String withString, String withoutString) {
        assertEquals(withString,
                withTimestampMapper()
                        .writerFor(clazz)
                        .writeValueAsString(value),
                String.format("withTimestampMapper : Expected %s, got %s", withString, value)
        );
        assertEquals(withoutString,
                withoutTimestampMapper()
                        .writerFor(clazz)
                        .writeValueAsString(value),
                String.format("withoutTimestampMapper : Expected %s, got %s", withoutString, value)
        );
    }

}
