package com.fasterxml.jackson.databind.ser.jdk;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

@SuppressWarnings("serial")
public class MapSerializationTest extends BaseMapTest
{
    @JsonSerialize(using=PseudoMapSerializer.class)
    static class PseudoMap extends LinkedHashMap<String,String>
    {
        public PseudoMap(String... values) {
            for (int i = 0, len = values.length; i < len; i += 2) {
                put(values[i], values[i+1]);
            }
        }
    }

    static class PseudoMapSerializer extends JsonSerializer<Map<String,String>>
    {
        @Override
        public void serialize(Map<String,String> value,
                JsonGenerator gen, SerializerProvider provider) throws IOException
        {
            // just use standard Map.toString(), output as JSON String
            gen.writeString(value.toString());
        }
    }

    // [databind#335]
    static class MapOrderingBean {
        @JsonPropertyOrder(alphabetic=true)
        public LinkedHashMap<String,Integer> map;

        public MapOrderingBean(String... keys) {
            map = new LinkedHashMap<String,Integer>();
            int ix = 1;
            for (String key : keys) {
                map.put(key, ix++);
            }
        }
    }

    // [databind#565]: Support ser/deser of Map.Entry
    static class StringIntMapEntry implements Map.Entry<String,Integer> {
        public final String k;
        public final Integer v;
        public StringIntMapEntry(String k, Integer v) {
            this.k = k;
            this.v = v;
        }

        @Override
        public String getKey() {
            return k;
        }

        @Override
        public Integer getValue() {
            return v;
        }

        @Override
        public Integer setValue(Integer value) {
            throw new UnsupportedOperationException();
        }
    }

    static class StringIntMapEntryWrapper {
        public StringIntMapEntry value;

        public StringIntMapEntryWrapper(String k, Integer v) {
            value = new StringIntMapEntry(k, v);
        }
    }

    // for [databind#691]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    @JsonTypeName("mymap")
    static class MapWithTypedValues extends LinkedHashMap<String,String> { }

    @JsonTypeInfo(use = Id.CLASS)
    public static class Mixin691 { }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testUsingObjectWriter() throws IOException
    {
        ObjectWriter w = MAPPER.writerFor(Object.class);
        Map<String,Object> map = new LinkedHashMap<String,Object>();
        map.put("a", 1);
        String json = w.writeValueAsString(map);
        assertEquals(a2q("{'a':1}"), json);
    }

    public void testMapSerializer() throws IOException
    {
        assertEquals("\"{a=b, c=d}\"", MAPPER.writeValueAsString(new PseudoMap("a", "b", "c", "d")));
    }

    // problems with map entries, values
    public void testMapKeySetValuesSerialization() throws IOException
    {
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        assertEquals("[\"a\"]", MAPPER.writeValueAsString(map.keySet()));
        assertEquals("[\"b\"]", MAPPER.writeValueAsString(map.values()));

        // TreeMap has similar inner class(es):
        map = new TreeMap<String,String>();
        map.put("c", "d");
        assertEquals("[\"c\"]", MAPPER.writeValueAsString(map.keySet()));
        assertEquals("[\"d\"]", MAPPER.writeValueAsString(map.values()));

        // and for [JACKSON-533], same for concurrent maps
        map = new ConcurrentHashMap<String,String>();
        map.put("e", "f");
        assertEquals("[\"e\"]", MAPPER.writeValueAsString(map.keySet()));
        assertEquals("[\"f\"]", MAPPER.writeValueAsString(map.values()));
    }

    // sort Map entries by key
    public void testOrderByKey() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertFalse(m.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        LinkedHashMap<String,Integer> map = new LinkedHashMap<String,Integer>();
        map.put("b", 3);
        map.put("a", 6);
        // by default, no (re)ordering:
        assertEquals("{\"b\":3,\"a\":6}", m.writeValueAsString(map));
        // but can be changed
        ObjectWriter sortingW =  m.writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        assertEquals("{\"a\":6,\"b\":3}", sortingW.writeValueAsString(map));
    }

    // related to [databind#1411]
    public void testOrderByWithNulls() throws IOException
    {
        ObjectWriter sortingW = MAPPER.writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        // 16-Oct-2016, tatu: but mind the null key, if any
        Map<String,Integer> mapWithNullKey = new LinkedHashMap<String,Integer>();
        mapWithNullKey.put(null, 1);
        mapWithNullKey.put("b", 2);
        // 16-Oct-2016, tatu: By default, null keys are not accepted...
        try {
            /*String json =*/ sortingW.writeValueAsString(mapWithNullKey);
            //assertEquals(a2q("{'':1,'b':2}"), json);
        } catch (DatabindException e) {
            verifyException(e, "Null key for a Map not allowed");
        }
    }

    // [Databind#335]
    public void testOrderByKeyViaProperty() throws IOException
    {
        MapOrderingBean input = new MapOrderingBean("c", "b", "a");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'map':{'a':3,'b':2,'c':1}}"), json);
    }

    // [Databind#565]
    public void testMapEntry() throws IOException
    {
        StringIntMapEntry input = new StringIntMapEntry("answer", 42);
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'answer':42}"), json);

        StringIntMapEntry[] array = new StringIntMapEntry[] { input };
        json = MAPPER.writeValueAsString(array);
        assertEquals(a2q("[{'answer':42}]"), json);

        // and maybe with bit of extra typing?
        ObjectMapper mapper = new ObjectMapper().activateDefaultTyping(NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_FINAL);
        json = mapper.writeValueAsString(input);
        assertEquals(a2q("['"+StringIntMapEntry.class.getName()+"',{'answer':42}]"),
                json);
    }

    public void testMapEntryWrapper() throws IOException
    {
        StringIntMapEntryWrapper input = new StringIntMapEntryWrapper("answer", 42);
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'value':{'answer':42}}"), json);
    }

    // [databind#691]
    public void testNullJsonMapping691() throws Exception
    {
        MapWithTypedValues input = new MapWithTypedValues();
        input.put("id", "Test");
        input.put("NULL", null);

        String json = MAPPER.writeValueAsString(input);

        assertEquals(a2q("{'@type':'mymap','id':'Test','NULL':null}"),
                json);
    }

    // [databind#691]
    public void testNullJsonInTypedMap691() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("NULL", null);

        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Object.class, Mixin691.class);
        String json = mapper.writeValueAsString(map);
        assertEquals("{\"@class\":\"java.util.HashMap\",\"NULL\":null}", json);
    }

    // [databind#1513]
    public void testConcurrentMaps() throws Exception
    {
        final ObjectWriter w = MAPPER.writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

        Map<String,String> input = new ConcurrentSkipListMap<String,String>();
        input.put("x", "y");
        input.put("a", "b");
        String json = w.writeValueAsString(input);
        assertEquals(a2q("{'a':'b','x':'y'}"), json);

        input = new ConcurrentHashMap<String,String>();
        input.put("x", "y");
        input.put("a", "b");
        json = w.writeValueAsString(input);
        assertEquals(a2q("{'a':'b','x':'y'}"), json);

        // One more: while not technically concurrent map at all, exhibits same issue
        input = new Hashtable<String,String>();
        input.put("x", "y");
        input.put("a", "b");
        json = w.writeValueAsString(input);
        assertEquals(a2q("{'a':'b','x':'y'}"), json);
    }
}
