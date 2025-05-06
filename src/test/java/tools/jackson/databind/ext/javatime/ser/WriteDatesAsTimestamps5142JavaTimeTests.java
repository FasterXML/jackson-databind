package tools.jackson.databind.ext.javatime.ser;

import java.time.*;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5142] JSTEP-5 Tests to verify current behavior and discuss further action DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS
public class WriteDatesAsTimestamps5142JavaTimeTests
    extends DateTimeTestBase
{
    private static final ObjectMapper WITH_TIMESTAMP_MAPPER = mapperBuilder()
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private static final ObjectMapper WITHOUT_TIMESTAMP_MAPPER = mapperBuilder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    public void testWriteDatesAsTimeStamps() throws Exception {
        // java.time.OffsetDateTime
        _testTimestamp(
                LocalDateTime.of(2025, 5, 4, 18, 1, 0),
                LocalDateTime.class,
                // Expected "[2025,5,4,18,1,0]",
                "[2025,5,4,18,1]", // Acutal
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
                // Expected "1234567890123",
                "1234567890.123000000",
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
                // Expected "7200000",
                "\"PT2H\"", // Actual
                "\"PT2H\""
        );
        // java.time.Period
        _testTimestamp(
                Period.of(2025, 5, 4),
                Period.class,
                // Expected "[2025,5,4]",
                "\"P2025Y5M4D\"", // Actual
                "\"P2025Y5M4D\""
        );
        // java.time.Year
        _testTimestamp(
                Year.of(2025),
                Year.class,
                "2025", // Actual
                "2025"
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
                "\"--05-04\"",
                "\"--05-04\""
        );
        // java.time.OffsetTime
        _testTimestamp(
                OffsetTime.of(18, 1, 2, 0, ZoneOffset.UTC),
                OffsetTime.class,
                "[18,1,2,\"Z\"]",
                "\"18:01:02Z\""
        );
        // java.time.OffsetDateTime
        _testTimestamp(
                OffsetDateTime.of(2025, 5, 4, 18, 1, 2, 0, ZoneOffset.UTC),
                OffsetDateTime.class,
                // Expected... "[2025,5,4,18,1,2,0]",
                "1746381662.000000000",
                "\"2025-05-04T18:01:02Z\""
        );
        // java.time.ZonedDateTime
        _testTimestamp(
                ZonedDateTime.of(2025, 5, 4, 18, 1, 2, 0, ZoneOffset.UTC),
                ZonedDateTime.class,
                // Expected... "[2025,5,4,18,1,2,0]",
                "1746381662.000000000",
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

    private static <T> void _testTimestamp(T value, Class<?> clazz, String withString, String withoutString) {
        assertEquals(
                withString,
                WITH_TIMESTAMP_MAPPER.writerFor(clazz).writeValueAsString(value),
                String.format("withTimestampMapper : Expected %s, got %s", withString, value)
        );
        assertEquals(
                withoutString,
                WITHOUT_TIMESTAMP_MAPPER.writerFor(clazz).writeValueAsString(value),
                String.format("withoutTimestampMapper : Expected %s, got %s", withoutString, value)
        );
    }

}
