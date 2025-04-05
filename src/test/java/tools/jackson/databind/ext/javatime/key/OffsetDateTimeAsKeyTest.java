package tools.jackson.databind.ext.javatime.key;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class OffsetDateTimeAsKeyTest extends DateTimeTestBase
{
    private static final TypeReference<Map<OffsetDateTime, String>> TYPE_REF = new TypeReference<Map<OffsetDateTime, String>>() {
    };
    private static final OffsetDateTime DATE_TIME_0 = OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC);
    private static final String DATE_TIME_0_STRING = "1970-01-01T00:00Z";
    private static final OffsetDateTime DATE_TIME_1 = OffsetDateTime.of(2015, 3, 14, 9, 26, 53, 590 * 1000 * 1000, ZoneOffset.UTC);
    private static final String DATE_TIME_1_STRING = "2015-03-14T09:26:53.590Z";
    private static final OffsetDateTime DATE_TIME_2 = OffsetDateTime.of(2015, 3, 14, 9, 26, 53, 590 * 1000 * 1000, ZoneOffset.ofHours(6));
    private static final String DATE_TIME_2_STRING = "2015-03-14T09:26:53.590+06:00";

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testSerialization0() throws Exception {
        String value = MAPPER.writeValueAsString(asMap(DATE_TIME_0, "test"));
        assertEquals(mapAsString(DATE_TIME_0_STRING, "test"), value,
                "Value is incorrect");
    }

    @Test
    public void testSerialization1() throws Exception {
        assertEquals(mapAsString(DATE_TIME_1_STRING, "test"),
                MAPPER.writeValueAsString(asMap(DATE_TIME_1, "test")),
                "Value is incorrect");
    }

    @Test
    public void testSerialization2() throws Exception {
        assertEquals(mapAsString(DATE_TIME_2_STRING, "test"),
                MAPPER.writeValueAsString(asMap(DATE_TIME_2, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization0() throws Exception {
        assertEquals(asMap(DATE_TIME_0, "test"), READER.readValue(mapAsString(DATE_TIME_0_STRING, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization1() throws Exception {
        assertEquals(asMap(DATE_TIME_1, "test"), READER.readValue(mapAsString(DATE_TIME_1_STRING, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization2() throws Exception {
        assertEquals(asMap(DATE_TIME_2, "test"),
                READER.readValue(mapAsString(DATE_TIME_2_STRING, "test")),
                "Value is incorrect");
    }
}
