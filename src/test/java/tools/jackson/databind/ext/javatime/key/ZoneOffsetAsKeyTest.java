package tools.jackson.databind.ext.javatime.key;

import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZoneOffsetAsKeyTest extends DateTimeTestBase
{
    private static final TypeReference<Map<ZoneOffset, String>> TYPE_REF = new TypeReference<Map<ZoneOffset, String>>() {
    };
    private static final ZoneOffset OFFSET_0 = ZoneOffset.UTC;
    private static final String OFFSET_0_STRING = "Z";
    private static final ZoneOffset OFFSET_1 = ZoneOffset.ofHours(6);
    private static final String OFFSET_1_STRING = "+06:00";

    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testSerialization0() throws Exception {
        String value = MAPPER.writeValueAsString(asMap(OFFSET_0, "test"));
        assertEquals(mapAsString(OFFSET_0_STRING, "test"), value);
    }

    @Test
    public void testSerialization1() throws Exception {
        String value = MAPPER.writeValueAsString(asMap(OFFSET_1, "test"));
        assertEquals(mapAsString(OFFSET_1_STRING, "test"), value);
    }

    @Test
    public void testDeserialization0() throws Exception {
        Map<ZoneOffset, String> value = MAPPER.readValue(mapAsString(OFFSET_0_STRING, "test"), TYPE_REF);
        assertEquals(asMap(OFFSET_0, "test"), value);
    }

    @Test
    public void testDeserialization1() throws Exception {
        Map<ZoneOffset, String> value = MAPPER.readValue(mapAsString(OFFSET_1_STRING, "test"), TYPE_REF);
        assertEquals(asMap(OFFSET_1, "test"), value);
    }
}
