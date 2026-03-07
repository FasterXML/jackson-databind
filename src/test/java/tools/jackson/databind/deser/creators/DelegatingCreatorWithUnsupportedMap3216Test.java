package tools.jackson.databind.deser.creators;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// [databind#3216] Delegating @JsonCreator should not fail due to
// inability to resolve deserializers for unused bean properties
public class DelegatingCreatorWithUnsupportedMap3216Test
{
    static class Container3216 {
        public List<String[]> keys;
        public List<String> values;

        public Container3216() { }

        public Container3216(List<String[]> keys, List<String> values) {
            this.keys = keys;
            this.values = values;
        }
    }

    static class Data3216 {
        // Map with unsupported key type (String[]) -- would fail if
        // Jackson tries to resolve a Map key deserializer for it
        @JsonIgnore
        public Map<String[], String> map = new HashMap<>();

        public Data3216() { }

        @JsonValue
        public Container3216 getContainer() {
            List<String[]> keys = new ArrayList<>();
            List<String> values = new ArrayList<>();
            map.forEach((key, value) -> {
                keys.add(key);
                values.add(value);
            });
            return new Container3216(keys, values);
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Data3216 create(Container3216 container) {
            Data3216 data = new Data3216();
            for (int index = 0; index < container.keys.size(); index++) {
                data.map.put(container.keys.get(index), container.values.get(index));
            }
            return data;
        }
    }

    // Variant without @JsonIgnore: should also work with delegating creator
    // since bean properties are not used during delegation deserialization
    static class DataNoIgnore3216 {
        public Map<String[], String> map = new HashMap<>();

        public DataNoIgnore3216() { }

        @JsonValue
        public Container3216 getContainer() {
            List<String[]> keys = new ArrayList<>();
            List<String> values = new ArrayList<>();
            map.forEach((key, value) -> {
                keys.add(key);
                values.add(value);
            });
            return new Container3216(keys, values);
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static DataNoIgnore3216 create(Container3216 container) {
            DataNoIgnore3216 data = new DataNoIgnore3216();
            for (int index = 0; index < container.keys.size(); index++) {
                data.map.put(container.keys.get(index), container.values.get(index));
            }
            return data;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Baseline test: delegating creator with @JsonIgnore on the problematic field works
    @Test
    public void testDelegatingCreatorWithJsonIgnore() throws Exception
    {
        Data3216 data = new Data3216();
        data.map.put(new String[]{"a", "b"}, "c");

        String json = MAPPER.writeValueAsString(data);
        Data3216 result = MAPPER.readValue(json, Data3216.class);
        assertNotNull(result);
        assertEquals(1, result.map.size());
    }

    // [databind#3216]: should work even without @JsonIgnore since
    // delegating creator means properties are never used
    @Test
    public void testDelegatingCreatorWithoutJsonIgnore() throws Exception
    {
        DataNoIgnore3216 data = new DataNoIgnore3216();
        data.map.put(new String[]{"a", "b"}, "c");

        String json = MAPPER.writeValueAsString(data);
        DataNoIgnore3216 result = MAPPER.readValue(json, DataNoIgnore3216.class);
        assertNotNull(result);
        assertEquals(1, result.map.size());
    }
}
