package tools.jackson.databind.ext.javatime.key;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class OffsetTimeAsKeyTest extends DateTimeTestBase
{
    private static final TypeReference<Map<OffsetTime, String>> TYPE_REF = new TypeReference<Map<OffsetTime, String>>() {
    };
    private static final OffsetTime TIME_0 = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
    private static final String TIME_0_STRING = "00:00Z";
    private static final OffsetTime TIME_1 = OffsetTime.of(3, 14, 15, 920 * 1000 * 1000, ZoneOffset.UTC);
    private static final String TIME_1_STRING = "03:14:15.920Z";
    private static final OffsetTime TIME_2 = OffsetTime.of(3, 14, 15, 920 * 1000 * 1000, ZoneOffset.ofHours(6));
    private static final String TIME_2_STRING = "03:14:15.920+06:00";

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testSerialization0() throws Exception {
        assertEquals(mapAsString(TIME_0_STRING, "test"),
                MAPPER.writeValueAsString(asMap(TIME_0, "test")));
    }

    @Test
    public void testSerialization1() throws Exception {
        assertEquals(mapAsString(TIME_1_STRING, "test"),
                MAPPER.writeValueAsString(asMap(TIME_1, "test")),
                "Value is incorrect");
    }

    @Test
    public void testSerialization2() throws Exception {
        assertEquals(mapAsString(TIME_2_STRING, "test"),
                MAPPER.writeValueAsString(asMap(TIME_2, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization0() throws Exception {
        assertEquals(asMap(TIME_0, "test"),
                READER.readValue(mapAsString(TIME_0_STRING, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization1() throws Exception {
        assertEquals(asMap(TIME_1, "test"),
                READER.readValue(mapAsString(TIME_1_STRING, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization2() throws Exception {
        assertEquals(asMap(TIME_2, "test"),
                READER.readValue(mapAsString(TIME_2_STRING, "test")),
                "Value is incorrect");
    }
}
