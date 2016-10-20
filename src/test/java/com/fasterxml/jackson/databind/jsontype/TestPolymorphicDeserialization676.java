package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.util.*;

/**
 * Reproduction of [https://github.com/FasterXML/jackson-databind/issues/676]
 * <p/>
 * Deserialization of class with generic collection inside
 * depends on how is was deserialized first time.
 */
public class TestPolymorphicDeserialization676 extends BaseMapTest
{
    private static final int TIMESTAMP = 123456;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MapContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.PROPERTY,
                property = "@class")
        public Map<String, Object> map;

        public MapContainer() { }

        public MapContainer(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof MapContainer)) return false;
            return map.equals(((MapContainer) o).map);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[MapContainer:");
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                sb.append(" '").append(entry.getKey()).append("' : ");
                Object value = entry.getValue();
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append("(").append(value.getClass().getName()).append(") ");
                    sb.append(String.valueOf(value));
                }
            }
            return sb.append(']').toString();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PolymorphicValueWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.PROPERTY,
                property = "@class")
        public Object value;
    }

    private final MapContainer originMap;

    public TestPolymorphicDeserialization676() {
        Map<String, Object> localMap = new LinkedHashMap<String, Object>();
        localMap.put("DateValue", new Date(TIMESTAMP));
        originMap = new MapContainer(localMap);
    }

    /**
     * If the class was first deserialized as polymorphic field,
     * deserialization will fail at complex type.
     */
    public void testDeSerFail() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        MapContainer deserMapBad = createDeSerMapContainer(originMap, mapper);
        assertEquals(originMap, deserMapBad);
        assertEquals(originMap,
                mapper.readValue(mapper.writeValueAsString(originMap), MapContainer.class));
    }

    public void testDeSerCorrect() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("1", 1);
        // commenting out the following statement will fail the test
        assertEquals(new MapContainer(map),
                mapper.readValue(mapper.writeValueAsString(new MapContainer(map)),
                        MapContainer.class));

        MapContainer deserMapGood = createDeSerMapContainer(originMap, mapper);

        assertEquals(originMap, deserMapGood);
        assertEquals(new Date(TIMESTAMP), deserMapGood.map.get("DateValue"));

        assertEquals(originMap, mapper.readValue(mapper.writeValueAsString(originMap), MapContainer.class));
    }

    private MapContainer createDeSerMapContainer(MapContainer src, ObjectMapper mapper) throws IOException {
        PolymorphicValueWrapper result = new PolymorphicValueWrapper();
        result.value = src;
        String json = mapper.writeValueAsString(result);
        assertEquals("{\"value\":{\"@class\":"
                + "\""+getClass().getName()+"$MapContainer\","
                + "\"map\":{\"DateValue\":[\"java.util.Date\",123456]}}}",
                json);
        PolymorphicValueWrapper deserializedResult = mapper.readValue(json, PolymorphicValueWrapper.class);
        return (MapContainer) deserializedResult.value;
    }
}
