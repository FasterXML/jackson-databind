package tools.jackson.databind.ser.filter;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
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

    public static class CountingKeySerializer extends ValueSerializer<Key> {
        static final AtomicInteger CALLS = new AtomicInteger();

        @Override
        public void serialize(Key value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName(value.name() + CALLS.incrementAndGet());
        }
    }

    static class IncludeStatefulKey {
        @JsonIncludeProperties("A1")
        @JsonSerialize(keyUsing = CountingKeySerializer.class)
        public Map<Key, String> values = new LinkedHashMap<>();
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

    @Test
    void filteringUsesTheSameSerializedKeyThatItWrites() throws Exception
    {
        CountingKeySerializer.CALLS.set(0);
        IncludeStatefulKey input = new IncludeStatefulKey();
        input.values.put(Key.A, "a");

        assertEquals("{\"values\":{\"A1\":\"a\"}}", MAPPER.writeValueAsString(input));
        assertEquals(1, CountingKeySerializer.CALLS.get());
    }
}
