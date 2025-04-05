package tools.jackson.databind.ext.javatime.key;

import java.time.Period;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class PeriodAsKeyTest extends DateTimeTestBase
{
    private static final TypeReference<Map<Period, String>> TYPE_REF = new TypeReference<Map<Period, String>>() {
    };
    private static final Period PERIOD_0 = Period.of(0, 0, 0);
    private static final String PERIOD_0_STRING = "P0D";
    private static final Period PERIOD = Period.of(3, 1, 4);
    private static final String PERIOD_STRING = "P3Y1M4D";

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testSerialization0() throws Exception {
        assertEquals(mapAsString(PERIOD_0_STRING, "test"),
                MAPPER.writeValueAsString(asMap(PERIOD_0, "test")),
                "Value is incorrect");
    }

    @Test
    public void testSerialization1() throws Exception {
        assertEquals(mapAsString(PERIOD_STRING, "test"),
                MAPPER.writeValueAsString(asMap(PERIOD, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization0() throws Exception {
        assertEquals(asMap(PERIOD_0, "test"),
                READER.readValue(mapAsString(PERIOD_0_STRING, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization1() throws Exception {
        assertEquals(asMap(PERIOD, "test"),
                READER.readValue(mapAsString(PERIOD_STRING, "test")),
                "Value is incorrect");
    }
}
