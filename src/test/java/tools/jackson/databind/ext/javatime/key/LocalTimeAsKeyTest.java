package tools.jackson.databind.ext.javatime.key;

import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class LocalTimeAsKeyTest extends DateTimeTestBase
{
    private static final LocalTime TIME_0 = LocalTime.ofSecondOfDay(0);
    /*
     * Seconds are omitted if possible
     */
    private static final String TIME_0_STRING = "00:00";
    private static final LocalTime TIME = LocalTime.of(3, 14, 15, 920 * 1000 * 1000);
    private static final String TIME_STRING = "03:14:15.920";

    private static final TypeReference<Map<LocalTime, String>> TYPE_REF = new TypeReference<Map<LocalTime, String>>() {
    };
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testSerialization0() throws Exception {
        assertEquals(mapAsString(TIME_0_STRING, "test"),
                MAPPER.writeValueAsString(asMap(TIME_0, "test")));
    }

    @Test
    public void testSerialization1() throws Exception {
        assertEquals(mapAsString(TIME_STRING, "test"),
                MAPPER.writeValueAsString(asMap(TIME, "test")));
    }

    @Test
    public void testDeserialization0() throws Exception {
        assertEquals(asMap(TIME_0, "test"), READER.readValue(mapAsString(TIME_0_STRING, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization1() throws Exception {
        assertEquals(asMap(TIME, "test"), READER.readValue(mapAsString(TIME_STRING, "test")),
                "Value is incorrect");
    }
}
