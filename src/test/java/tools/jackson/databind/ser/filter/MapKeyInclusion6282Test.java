package tools.jackson.databind.ser.filter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapKeyInclusion6282Test extends DatabindTestUtil
{
    enum Key {
        A,
        B
    }

    static class IgnoreEnumKeys {
        @JsonIgnoreProperties("B")
        public Map<Key, String> values = new LinkedHashMap<>();
    }

    static class IncludeEnumKeys {
        @JsonIncludeProperties("A")
        public Map<Key, String> values = new LinkedHashMap<>();
    }

    static class IgnoreIntegerKeys {
        @JsonIgnoreProperties("2")
        public Map<Integer, String> values = new LinkedHashMap<>();
    }

    static class IncludeIntegerKeys {
        @JsonIncludeProperties("1")
        public Map<Integer, String> values = new LinkedHashMap<>();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void ignorePropertiesMatchesSerializedEnumKeys() throws Exception
    {
        IgnoreEnumKeys input = new IgnoreEnumKeys();
        input.values.put(Key.A, "a");
        input.values.put(Key.B, "b");

        assertEquals("{\"values\":{\"A\":\"a\"}}", MAPPER.writeValueAsString(input));
    }

    @Test
    void includePropertiesMatchesSerializedEnumKeys() throws Exception
    {
        IncludeEnumKeys input = new IncludeEnumKeys();
        input.values.put(Key.A, "a");
        input.values.put(Key.B, "b");

        assertEquals("{\"values\":{\"A\":\"a\"}}", MAPPER.writeValueAsString(input));
    }

    @Test
    void ignorePropertiesMatchesSerializedIntegerKeys() throws Exception
    {
        IgnoreIntegerKeys input = new IgnoreIntegerKeys();
        input.values.put(1, "a");
        input.values.put(2, "b");

        assertEquals("{\"values\":{\"1\":\"a\"}}", MAPPER.writeValueAsString(input));
    }

    @Test
    void includePropertiesMatchesSerializedIntegerKeys() throws Exception
    {
        IncludeIntegerKeys input = new IncludeIntegerKeys();
        input.values.put(1, "a");
        input.values.put(2, "b");

        assertEquals("{\"values\":{\"1\":\"a\"}}", MAPPER.writeValueAsString(input));
    }
}
