package tools.jackson.databind.ext.javatime.key;

import java.time.MonthDay;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class MonthDayAsKeyTest extends DateTimeTestBase
{
    private static final MonthDay MONTH_DAY = MonthDay.of(3, 14);
    private static final String MONTH_DAY_STRING = "--03-14";

    private static final TypeReference<Map<MonthDay, String>> TYPE_REF = new TypeReference<Map<MonthDay, String>>() {
    };
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testSerialization() throws Exception {
        assertEquals(mapAsString(MONTH_DAY_STRING, "test"), MAPPER.writeValueAsString(asMap(MONTH_DAY, "test")),
                "Value is incorrect");
    }

    @Test
    public void testDeserialization() throws Exception {
        assertEquals(asMap(MONTH_DAY, "test"), READER.readValue(mapAsString(MONTH_DAY_STRING, "test")),
                "Value is incorrect");
    }
}
