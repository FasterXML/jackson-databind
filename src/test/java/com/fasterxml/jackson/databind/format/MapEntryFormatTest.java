package com.fasterxml.jackson.databind.format;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;

public class MapEntryFormatTest extends BaseMapTest
{
    static class BeanWithMapEntry {
        // would work with any other shape than OBJECT, or without annotation:
        @JsonFormat(shape=JsonFormat.Shape.NATURAL)
        public Map.Entry<String,String> entry;

        protected BeanWithMapEntry() { }
        public BeanWithMapEntry(String key, String value) {
            Map<String,String> map = new LinkedHashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.OBJECT)
    @JsonPropertyOrder({ "key", "value" })
    static class MapEntryAsObject implements Map.Entry<String,String> {
        protected String key, value;

        protected MapEntryAsObject() { }
        public MapEntryAsObject(String k, String v) {
            key = k;
            value = v;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String v) {
            value = v;
            return v; // wrong, whatever
        }
    }

    static class EntryWithNullWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_NULL)
        public Map.Entry<String,String> entry;

        public EntryWithNullWrapper(String key, String value) {
            HashMap<String,String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    static class EntryWithDefaultWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_DEFAULT)
        public Map.Entry<String,String> entry;

        public EntryWithDefaultWrapper(String key, String value) {
            HashMap<String,String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    static class EntryWithNonAbsentWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_ABSENT)
        public Map.Entry<String,AtomicReference<String>> entry;

        public EntryWithNonAbsentWrapper(String key, String value) {
            HashMap<String,AtomicReference<String>> map = new HashMap<>();
            map.put(key, new AtomicReference<String>(value));
            entry = map.entrySet().iterator().next();
        }
    }

    static class EmptyEntryWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_EMPTY)
        public Map.Entry<String,String> entry;

        public EmptyEntryWrapper(String key, String value) {
            HashMap<String,String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    /*
    /**********************************************************************
    /* Test methods, basic
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testInclusion() throws Exception
    {
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EmptyEntryWrapper("a", "b")));
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EntryWithDefaultWrapper("a", "b")));
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EntryWithNullWrapper("a", "b")));

        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EmptyEntryWrapper("a", "")));
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EntryWithDefaultWrapper("a", "")));
        assertEquals(a2q("{'entry':{'a':''}}"),
                MAPPER.writeValueAsString(new EntryWithNullWrapper("a", "")));
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EntryWithNullWrapper("a", null)));
    }

    public void testInclusionWithReference() throws Exception
    {
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EntryWithNonAbsentWrapper("a", "b")));
        // empty String not excluded since reference is not absent, just points to empty
        // (so would need 3rd level inclusion definition)
        assertEquals(a2q("{'entry':{'a':''}}"),
                MAPPER.writeValueAsString(new EntryWithNonAbsentWrapper("a", "")));
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EntryWithNonAbsentWrapper("a", null)));
    }

    /*
    /**********************************************************************
    /* Test methods, as-Object (Shape)
    /**********************************************************************
     */

    public void testAsNaturalRoundtrip() throws Exception
    {
        BeanWithMapEntry input = new BeanWithMapEntry("foo" ,"bar");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'entry':{'foo':'bar'}}"), json);
        BeanWithMapEntry result = MAPPER.readValue(json, BeanWithMapEntry.class);
        assertEquals("foo", result.entry.getKey());
        assertEquals("bar", result.entry.getValue());
    }

    // should work via class annotation
    public void testAsObjectRoundtrip() throws Exception
    {
        MapEntryAsObject input = new MapEntryAsObject("foo" ,"bar");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'key':'foo','value':'bar'}"), json);

        // 16-Oct-2016, tatu: Happens to work by default because it's NOT basic
        //   `Map.Entry` but subtype.

        MapEntryAsObject result = MAPPER.readValue(json, MapEntryAsObject.class);
        assertEquals("foo", result.getKey());
        assertEquals("bar", result.getValue());
    }

    // [databind#1895]
    public void testDefaultShapeOverride() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Map.Entry.class, cfg ->
                    cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.OBJECT)))
                .build();
        Map.Entry<String,String> input = new BeanWithMapEntry("foo", "bar").entry;
        assertTrue(mapper.writeValueAsString(input).equals(a2q("{'key':'foo','value':'bar'}")) || mapper.writeValueAsString(input).equals(a2q("{'value':'bar','key':'foo'}")));
    }
}
