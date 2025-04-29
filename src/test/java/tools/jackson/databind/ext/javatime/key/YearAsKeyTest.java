package tools.jackson.databind.ext.javatime.key;

import java.time.Year;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class YearAsKeyTest extends DateTimeTestBase
{
    private static final TypeReference<Map<Year, String>> TYPE_REF = new TypeReference<Map<Year, String>>() {
    };
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testKeySerialization() throws Exception {
        assertEquals(mapAsString("3141", "test"),
                MAPPER.writeValueAsString(asMap(Year.of(3141), "test")),
                "Value is incorrect");
    }

    @Test
    public void testKeyDeserialization() throws Exception {
        assertEquals(asMap(Year.of(3141), "test"), READER.readValue(mapAsString("3141", "test")),
                "Value is incorrect");
        // Test both padded, unpadded
        assertEquals(asMap(Year.of(476), "test"), READER.readValue(mapAsString("0476", "test")),
                "Value is incorrect");
        assertEquals(asMap(Year.of(476), "test"), READER.readValue(mapAsString("476", "test")),
                "Value is incorrect");
    }

    @Test
    public void deserializeYearKey_notANumber() throws Exception {
        assertThrows(InvalidFormatException.class, () -> {
            READER.readValue(mapAsString("10000BC", "test"));
        });
    }

    @Test
    public void deserializeYearKey_notAYear() throws Exception {
        assertThrows(InvalidFormatException.class, () -> {
            READER.readValue(mapAsString(Integer.toString(Year.MAX_VALUE+1), "test"));
        });
    }

    @Test
    public void serializeAndDeserializeYearKeyUnpadded() throws Exception {
        // fix for issue #51 verify we can deserialize an unpadded year e.g. "1"
        Map<Year, Float> testMap = Collections.singletonMap(Year.of(1), 1F);
        String serialized = MAPPER.writeValueAsString(testMap);
        TypeReference<Map<Year, Float>> yearFloatTypeReference = new TypeReference<Map<Year, Float>>() {};
        Map<Year, Float> deserialized = MAPPER.readValue(serialized, yearFloatTypeReference);
        assertEquals(testMap, deserialized);

        // actually, check padded as well just to make sure
        Map<Year, Float> deserialized2 = MAPPER.readValue(a2q("{'0001':1.0}"),
                yearFloatTypeReference);
        assertEquals(testMap, deserialized2);
    }
}
