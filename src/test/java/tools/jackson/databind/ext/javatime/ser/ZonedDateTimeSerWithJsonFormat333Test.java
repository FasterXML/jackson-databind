package tools.jackson.databind.ext.javatime.ser;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [module-java8#333]: ZonedDateTime serialization with @JsonFormat pattern never uses
//  while WRITE_DATES_WITH_ZONE_ID enabled #333
public class ZonedDateTimeSerWithJsonFormat333Test
    extends DateTimeTestBase
{
    public static class ContainerWithPattern333 {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss z")
        public ZonedDateTime value;
    }

    public static class ContainerWithoutPattern333 {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public ZonedDateTime value;
    }

    private final ObjectMapper MAPPER = mapperBuilder()
            .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
            .build();

    @Test
    public void testJsonFormatOverridesSerialization() throws Exception
    {
        // ISO-8601 string for ZonedDateTime
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2024-11-15T18:27:06.921054+01:00[Europe/Berlin]");
        ContainerWithPattern333 input = new ContainerWithPattern333();
        input.value = zonedDateTime;

        assertEquals(a2q("{'value':'2024-11-15 18:27:06 CET'}"),
                MAPPER.writeValueAsString(input));
    }
}
