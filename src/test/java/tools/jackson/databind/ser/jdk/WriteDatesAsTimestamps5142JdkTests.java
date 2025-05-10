package tools.jackson.databind.ser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5142] JSTEP-5 Tests to verify current behavior and discuss further action DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS
public class WriteDatesAsTimestamps5142JdkTests
    extends DateTimeTestBase
{
    private static final ObjectMapper WITH_TIMESTAMP_MAPPER = mapperBuilder()
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private static final ObjectMapper WITHOUT_TIMESTAMP_MAPPER = mapperBuilder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    public void testWriteDatesAsTimeStamps()
        throws Exception
    {
        // java.util.Date
        _testTimestamp(
                new Date(1234567890123L),
                Date.class,
                // Expected... [2009,1,13,23,31,30,123],
                "1234567890123",
                "\"2009-02-13T23:31:30.123Z\""
        );

        // java.util.Calendar
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1234567890123L);
        _testTimestamp(
                cal,
                Calendar.class,
                // Expected... [2009,1,13,23,31,30,123],
                "1234567890123",
                "\"2009-02-13T23:31:30.123Z\""
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
