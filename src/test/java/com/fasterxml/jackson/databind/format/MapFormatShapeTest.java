package com.fasterxml.jackson.databind.format;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

@SuppressWarnings("serial")
public class MapFormatShapeTest extends BaseMapTest
{
    @JsonPropertyOrder({ "extra" })
    static class Map476Base extends LinkedHashMap<String,Integer> {
        public int extra = 13;
    }

    @JsonFormat(shape=JsonFormat.Shape.OBJECT)
    static class Map476AsPOJO extends Map476Base { }

    @JsonPropertyOrder({ "a", "b", "c" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Bean476Container
    {
        public Map476AsPOJO a;
        public Map476Base b;
        @JsonFormat(shape=JsonFormat.Shape.OBJECT)
        public Map476Base c;

        public Bean476Container(int forA, int forB, int forC) {
            if (forA != 0) {
                a = new Map476AsPOJO();
                a.put("value", forA);
            }
            if (forB != 0) {
                b = new Map476Base();
                b.put("value", forB);
            }
            if (forC != 0) {
                c = new Map476Base();
                c.put("value", forC);
            }
        }
    }

    static class Bean476Override
    {
        @JsonFormat(shape=JsonFormat.Shape.NATURAL)
        public Map476AsPOJO stuff;

        public Bean476Override(int value) {
            stuff = new Map476AsPOJO();
            stuff.put("value", value);
        }
    }

    // from [databind#1540]
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonPropertyOrder({ "property", "map" })
    static class Map1540Implementation implements Map<Integer, Integer> {
        public int property;
        public Map<Integer, Integer> map = new LinkedHashMap<>();

        public Map<Integer, Integer> getMap() {
            return map;
       }

       public void setMap(Map<Integer, Integer> map) {
            this.map = map;
       }

       @Override
       public Integer put(Integer key, Integer value) {
            return map.put(key, value);
       }

        @Override
        public int size() {
            return map.size();
        }

        @JsonIgnore
        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        @Override
        public Integer get(Object key) {
            return map.get(key);
        }

        @Override
        public Integer remove(Object key) {
            return map.remove(key);
        }

        @Override
        public void putAll(Map<? extends Integer, ? extends Integer> m) {
            map.putAll(m);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public Set<Integer> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<Integer> values() {
            return map.values();
        }

        @Override
        public Set<java.util.Map.Entry<Integer, Integer>> entrySet() {
            return map.entrySet();
        }
    }


    /*
    /**********************************************************
    /* Test methods, serialization
    /**********************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();

    // for [databind#476]: Maps as POJOs
    public void testSerializeAsPOJOViaClass() throws Exception
    {
        String result = MAPPER.writeValueAsString(new Bean476Container(1,2,0));
        assertEquals(a2q("{'a':{'extra':13,'empty':false},'b':{'value':2}}"),
                result);
    }

    // Can't yet use per-property overrides at all, see [databind#1419]

    /*
    public void testSerializeAsPOJOViaProperty() throws Exception
    {
        String result = MAPPER.writeValueAsString(new Bean476Container(1,0,3));
        assertEquals(a2q("{'a':{'extra':13,'empty':false},'c':{'empty':false,'value':3}}"),
                result);
    }

    public void testSerializeNaturalViaOverride() throws Exception
    {
        String result = MAPPER.writeValueAsString(new Bean476Override(123));
        assertEquals(a2q("{'stuff':{'value':123}}"),
                result);
    }
    */

    /*
    /**********************************************************
    /* Test methods, deserialization/roundtrip
    /**********************************************************
     */

    // [databind#1540]
    public void testRoundTrip() throws Exception
    {
        Map1540Implementation input = new Map1540Implementation();
        input.property = 55;
        input.put(12, 45);
        input.put(6, 88);
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        String json = mapper.writeValueAsString(input);

        assertEquals(a2q("{'property':55,'map':{'12':45,'6':88}}"), json);

        Map1540Implementation result = MAPPER.readValue(json, Map1540Implementation.class);
        assertEquals(result.property, input.property);
        assertEquals(input.getMap(), input.getMap());
   }

    // [databind#1554]
    public void testDeserializeAsPOJOViaClass() throws Exception
    {
        Map476AsPOJO result = MAPPER.readValue(a2q("{'extra':42}"),
                Map476AsPOJO.class);
        assertEquals(0, result.size());
        assertEquals(42, result.extra);
    }
}
