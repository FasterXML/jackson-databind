package tools.jackson.databind.ext.javatime.ser;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestZonedDateTimeSerializationWithCustomFormatter
{
    @MethodSource("customFormatters")
    @ParameterizedTest
    public void testSerialization(DateTimeFormatter formatter) throws Exception {
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        assertTrue(serializeWith(zonedDateTime, formatter).contains(zonedDateTime.format(formatter.withZone(ZoneOffset.UTC))));
    }

    private String serializeWith(ZonedDateTime zonedDateTime, DateTimeFormatter f) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule().addSerializer(
                        new ZonedDateTimeSerializer(f)))
                .defaultTimeZone(TimeZone.getTimeZone("UTC"))
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return mapper.writeValueAsString(zonedDateTime);
    }

    public static Stream<DateTimeFormatter> customFormatters() {
        return Stream.of(
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        );
    }
}
