package tools.jackson.databind.ext.javatime.key;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDateAsKeyTest extends DateTimeTestBase
{
    private static final LocalDate DATE = LocalDate.of(2015, 3, 14);
    private static final String DATE_STRING = "2015-03-14";

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(new TypeReference<Map<LocalDate, String>>() { });

    @Test
    public void testSerialization() throws Exception {
        assertEquals(mapAsString(DATE_STRING, "test"),
                MAPPER.writeValueAsString(asMap(DATE, "test")));
    }

    @Test
    public void testDeserialization() throws Exception {
        assertEquals(asMap(DATE, "test"), READER.readValue(mapAsString(DATE_STRING, "test")));
    }
}
