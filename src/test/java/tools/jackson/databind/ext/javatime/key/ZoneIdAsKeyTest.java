package tools.jackson.databind.ext.javatime.key;

import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZoneIdAsKeyTest extends DateTimeTestBase
{
    private static final ZoneId ZONE_0 = ZoneId.of("UTC");
    private static final String ZONE_0_STRING = "UTC";
    private static final ZoneId ZONE_1 = ZoneId.of("+06:00");
    private static final String ZONE_1_STRING = "+06:00";
    private static final ZoneId ZONE_2 = ZoneId.of("Europe/London");
    private static final String ZONE_2_STRING = "Europe/London";

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(new TypeReference<Map<ZoneId, String>>() { });

    @Test
    public void testSerialization0() throws Exception {
        assertEquals(mapAsString(ZONE_0_STRING, "test"),
                MAPPER.writeValueAsString(asMap(ZONE_0, "test")));
    }

    @Test
    public void testSerialization1() throws Exception {
        assertEquals(mapAsString(ZONE_1_STRING, "test"),
                MAPPER.writeValueAsString(asMap(ZONE_1, "test")));
    }

    @Test
    public void testSerialization2() throws Exception {
        assertEquals(mapAsString(ZONE_2_STRING, "test"),
                MAPPER.writeValueAsString(asMap(ZONE_2, "test")));
    }

    @Test
    public void testDeserialization0() throws Exception {
        assertEquals(asMap(ZONE_0, "test"),
                READER.readValue(mapAsString(ZONE_0_STRING, "test")));
    }

    @Test
    public void testDeserialization1() throws Exception {
        assertEquals(asMap(ZONE_1, "test"),
                READER.readValue(mapAsString(ZONE_1_STRING, "test")));
    }

    @Test
    public void testDeserialization2() throws Exception {
        assertEquals(asMap(ZONE_2, "test"),
                READER.readValue(mapAsString(ZONE_2_STRING, "test")));
    }
}
