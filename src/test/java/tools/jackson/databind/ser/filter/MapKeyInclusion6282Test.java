package tools.jackson.databind.ser.filter;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.IgnorePropertiesUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapKeyInclusion6282Test extends DatabindTestUtil
{
    enum Key {
        A,
        B
    }

    enum ToStringKey {
        A,
        B;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    enum RenamedKey {
        @JsonProperty("alpha")
        A,
        @JsonProperty("beta")
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

    static class IgnoreToStringEnumKeys {
        @JsonIgnoreProperties("b")
        public Map<ToStringKey, String> values = new LinkedHashMap<>();
    }

    static class IncludeRenamedEnumKeys {
        @JsonIncludeProperties("alpha")
        public Map<RenamedKey, String> values = new LinkedHashMap<>();
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

    @Test
    void ignorePropertiesMatchesEnumToStringOutput() throws Exception
    {
        IgnoreToStringEnumKeys input = new IgnoreToStringEnumKeys();
        input.values.put(ToStringKey.A, "a");
        input.values.put(ToStringKey.B, "b");

        assertEquals("{\"values\":{\"a\":\"a\"}}", MAPPER.writeValueAsString(input));
    }

    @Test
    void includePropertiesMatchesRenamedEnumKey() throws Exception
    {
        IncludeRenamedEnumKeys input = new IncludeRenamedEnumKeys();
        input.values.put(RenamedKey.A, "a");
        input.values.put(RenamedKey.B, "b");

        assertEquals("{\"values\":{\"alpha\":\"a\"}}", MAPPER.writeValueAsString(input));
    }

    @Test
    void checkerDoesNotConvertNullToLiteralNull()
    {
        assertFalse(IgnorePropertiesUtil.Checker.construct(new HashSet<>(Set.of("null")), null)
                .shouldIgnore(null));
        assertTrue(IgnorePropertiesUtil.Checker.construct(new HashSet<>(), new HashSet<>(Set.of("null")))
                .shouldIgnore(null));
    }
}
