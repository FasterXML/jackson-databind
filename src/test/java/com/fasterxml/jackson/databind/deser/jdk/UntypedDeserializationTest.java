package com.fasterxml.jackson.databind.deser.jdk;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Unit tests for verifying "raw" (or "untyped") data binding from JSON to JDK objects;
 * one that only uses core JDK types; wrappers, Maps and Lists.
 */
@SuppressWarnings("serial")
public class UntypedDeserializationTest
    extends BaseMapTest
{
    static class UCStringDeserializer
        extends StdScalarDeserializer<String>
    {
        public UCStringDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return p.getText().toUpperCase();
        }
    }

    static class CustomNumberDeserializer
        extends StdScalarDeserializer<Number>
    {
        protected final Integer value;
        
        public CustomNumberDeserializer(int nr) {
            super(Number.class);
            value = nr;
        }

        @Override
        public Number deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return value;
        }
    }

    // Let's make this Contextual, to tease out cyclic resolution issues, if any
    static class ListDeserializer extends StdDeserializer<List<Object>>
    {
        public ListDeserializer() { super(List.class); }

        @Override
        public List<Object> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            ArrayList<Object> list = new ArrayList<Object>();
            while (p.nextValue() != JsonToken.END_ARRAY) {
                list.add("X"+p.getText());
            }
            return list;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property) throws JsonMappingException
        {
            // For now, we just need to access "untyped" deserializer; not use it.
            
            /*JsonDeserializer<Object> ob = */
            ctxt.findContextualValueDeserializer(ctxt.constructType(Object.class), property);
            return this;
        }
    }

    static class YMapDeserializer extends StdDeserializer<Map<String,Object>>
    {
        public YMapDeserializer() { super(Map.class); }

        @Override
        public Map<String,Object> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            Map<String,Object> map = new LinkedHashMap<String,Object>();
            while (p.nextValue() != JsonToken.END_OBJECT) {
                map.put(p.currentName(), "Y"+p.getText());
            }
            return map;
        }
    }

    static class DelegatingUntyped {
        protected Object value;
        
        @JsonCreator // delegating
        public DelegatingUntyped(Object v) {
            value = v;
        }
    }

    static class WrappedPolymorphicUntyped {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS)
        public Object value;
    }

    static class WrappedUntyped1460 {
        public Object value;
    }

    // [databind#2115]
    static class SerContainer {
        public java.io.Serializable value;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    @SuppressWarnings("unchecked")
    public void testSampleDoc() throws Exception
    {
        final String JSON = SAMPLE_DOC_JSON_SPEC;

        /* To get "untyped" Mapping (to Maps, Lists, instead of beans etc),
         * we'll specify plain old Object.class as the target.
         */
        Object root = MAPPER.readValue(JSON, Object.class);

        assertType(root, Map.class);
        Map<?,?> rootMap = (Map<?,?>) root;
        assertEquals(1, rootMap.size());
        Map.Entry<?,?> rootEntry =  rootMap.entrySet().iterator().next();
        assertEquals("Image", rootEntry.getKey());
        Object image = rootEntry.getValue();
        assertType(image, Map.class);
        Map<?,?> imageMap = (Map<?,?>) image;
        assertEquals(5, imageMap.size());

        Object value = imageMap.get("Width");
        assertType(value, Integer.class);
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_WIDTH), value);

        value = imageMap.get("Height");
        assertType(value, Integer.class);
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_HEIGHT), value);

        assertEquals(SAMPLE_SPEC_VALUE_TITLE, imageMap.get("Title"));

        // Another Object, "thumbnail"
        value = imageMap.get("Thumbnail");
        assertType(value, Map.class);
        Map<?,?> tnMap = (Map<?,?>) value;
        assertEquals(3, tnMap.size());

        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_HEIGHT), tnMap.get("Height"));
        // for some reason, width is textual, not numeric...
        assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, tnMap.get("Width"));
        assertEquals(SAMPLE_SPEC_VALUE_TN_URL, tnMap.get("Url"));

        // And then number list, "IDs"
        value = imageMap.get("IDs");
        assertType(value, List.class);
        List<Object> ids = (List<Object>) value;
        assertEquals(4, ids.size());
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID1), ids.get(0));
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID2), ids.get(1));
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID3), ids.get(2));
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID4), ids.get(3));

        // and that's all folks!
    }

    public void testUntypedMap() throws Exception
    {
        // to get "untyped" default map-to-map, pass Object.class
        String JSON = "{ \"foo\" : \"bar\", \"crazy\" : true, \"null\" : null }";

        // Not a guaranteed cast theoretically, but will work:
        @SuppressWarnings("unchecked")
        Map<Object,Object> result = (Map<Object,Object>)MAPPER.readValue(JSON, Object.class);
        assertNotNull(result);
        assertTrue(result instanceof Map<?,?>);

        assertEquals(3, result.size());

        assertEquals("bar", result.get("foo"));
        assertEquals(Boolean.TRUE, result.get("crazy"));
        assertNull(result.get("null"));

        // Plus, non existing:
        assertNull(result.get("bar"));
        assertNull(result.get(3));
    }

    public void testNestedUntypes() throws IOException
    {
        // 05-Apr-2014, tatu: Odd failures if using shared mapper; so work around:
        Object root = MAPPER.readValue(aposToQuotes("{'a':3,'b':[1,2]}"),
                Object.class);
        assertTrue(root instanceof Map<?,?>);
        Map<?,?> map = (Map<?,?>) root;
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        Object ob = map.get("b");
        assertTrue(ob instanceof List<?>);
        List<?> l = (List<?>) ob;
        assertEquals(2, l.size());
        assertEquals(Integer.valueOf(2), l.get(1));
    }
    
    // Allow 'upgrade' of big integers into Long, BigInteger
    public void testObjectSerializeWithLong() throws IOException
    {
        final ObjectMapper mapper = ObjectMapper.builder()
                .enableDefaultTyping(DefaultTyping.JAVA_LANG_OBJECT, As.PROPERTY)
                .build();
        final long VALUE = 1337800584532L;

        String serialized = "{\"timestamp\":"+VALUE+"}";
        // works fine as node
        JsonNode deserialized = mapper.readTree(serialized);
        assertEquals(VALUE, deserialized.get("timestamp").asLong());
        // and actually should work in Maps too
        Map<?,?> deserMap = mapper.readValue(serialized, Map.class);
        Number n = (Number) deserMap.get("timestamp");
        assertNotNull(n);
        assertSame(Long.class, n.getClass());
        assertEquals(Long.valueOf(VALUE), n);
    }

    public void testUntypedWithCustomScalarDesers() throws IOException
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(String.class, new UCStringDeserializer());
        m.addDeserializer(Number.class, new CustomNumberDeserializer(13));
        final ObjectMapper mapper = ObjectMapper.builder()
            .addModule(m)
            .build();

        Object ob = mapper.readValue("{\"a\":\"b\", \"nr\":1 }", Object.class);
        assertTrue(ob instanceof Map);
        Object value = ((Map<?,?>) ob).get("a");
        assertNotNull(value);
        assertTrue(value instanceof String);
        assertEquals("B", value);

        value = ((Map<?,?>) ob).get("nr");
        assertNotNull(value);
        assertTrue(value instanceof Number);
        assertEquals(Integer.valueOf(13), value);
    }

    // Test that exercises non-vanilla variant, with just one simple custom deserializer
    public void testNonVanilla() throws IOException
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(String.class, new UCStringDeserializer());
        final ObjectMapper mapper = ObjectMapper.builder()
                .addModule(m)
                .build();
        // Also: since this is now non-vanilla variant, try more alternatives
        List<?> l = (List<?>) mapper.readValue("[ true, false, 7, 0.5, \"foo\"]", Object.class);
        assertEquals(5, l.size());
        assertEquals(Boolean.TRUE, l.get(0));
        assertEquals(Boolean.FALSE, l.get(1));
        assertEquals(Integer.valueOf(7), l.get(2));
        assertEquals(Double.valueOf(0.5), l.get(3));
        assertEquals("FOO", l.get(4));

        l = (List<?>) mapper.readValue("[ {}, [] ]", Object.class);
        assertEquals(2, l.size());
        assertTrue(l.get(0) instanceof Map<?,?>);
        assertTrue(l.get(1) instanceof List<?>);

        ObjectReader rDefault = mapper.readerFor(WrappedPolymorphicUntyped.class);
        ObjectReader rAlt = rDefault
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                        DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        WrappedPolymorphicUntyped w;

        w = rDefault.readValue(aposToQuotes("{'value':10}"));
        assertEquals(Integer.valueOf(10), w.value);
        w = rAlt.readValue(aposToQuotes("{'value':10}"));
        assertEquals(BigInteger.TEN, w.value);

        w = rDefault.readValue(aposToQuotes("{'value':5.0}"));
        assertEquals(Double.valueOf(5.0), w.value);
        w = rAlt.readValue(aposToQuotes("{'value':5.0}"));
        assertEquals(new BigDecimal("5.0"), w.value);

        StringBuilder sb = new StringBuilder(100).append("[0");
        for (int i = 1; i < 100; ++i) {
            sb.append(", ").append(i);
        }
        sb.append("]");
        final String INT_ARRAY_JSON = sb.toString();

        // First read as-is, no type wrapping
        Object ob = mapper.readerFor(Object.class)
                .with(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
                .readValue(INT_ARRAY_JSON);
        assertTrue(ob instanceof Object[]);
        Object[] obs = (Object[]) ob;
        for (int i = 0; i < 100; ++i) {
            assertEquals(Integer.valueOf(i), obs[i]);
        }
    }

    public void testUntypedWithListDeser() throws IOException
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(List.class, new ListDeserializer());
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(m)
                .build();
        // And then list...
        Object ob = mapper.readValue("[1, 2, true]", Object.class);
        assertTrue(ob instanceof List<?>);
        List<?> l = (List<?>) ob;
        assertEquals(3, l.size());
        assertEquals("X1", l.get(0));
        assertEquals("X2", l.get(1));
        assertEquals("Xtrue", l.get(2));
    }

    public void testUntypedWithMapDeser() throws IOException
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(Map.class, new YMapDeserializer());
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(m)
                .build();
        // And then list...
        Object ob = mapper.readValue("{\"a\":true}", Object.class);
        assertTrue(ob instanceof Map<?,?>);
        Map<?,?> map = (Map<?,?>) ob;
        assertEquals(1, map.size());
        assertEquals("Ytrue", map.get("a"));
    }

    public void testNestedUntyped989() throws IOException
    {
        DelegatingUntyped pojo;
        ObjectReader r = MAPPER.readerFor(DelegatingUntyped.class);

        pojo = r.readValue("[]");
        assertTrue(pojo.value instanceof List);
        pojo = r.readValue("[{}]");
        assertTrue(pojo.value instanceof List);
        
        pojo = r.readValue("{}");
        assertTrue(pojo.value instanceof Map);
        pojo = r.readValue("{\"a\":[]}");
        assertTrue(pojo.value instanceof Map);
    }

    public void testUntypedWithJsonArrays() throws Exception
    {
        // by default we get:
        Object ob = MAPPER.readValue("[1]", Object.class);
        assertTrue(ob instanceof List<?>);

        // but can change to produce Object[]:
        ObjectMapper mapper = ObjectMapper.builder()
                .enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
                .build();
        ob = mapper.readValue("[1]", Object.class);
        assertEquals(Object[].class, ob.getClass());
    }

    public void testUntypedIntAsLong() throws Exception
    {
        final String JSON = aposToQuotes("{'value':3}");
        WrappedUntyped1460 w = MAPPER.readerFor(WrappedUntyped1460.class)
                .readValue(JSON);
        assertEquals(Integer.valueOf(3), w.value);

        w = MAPPER.readerFor(WrappedUntyped1460.class)
                .with(DeserializationFeature.USE_LONG_FOR_INTS)
                .readValue(JSON);
        assertEquals(Long.valueOf(3), w.value);
    }

    // [databind#2115]: Consider `java.io.Serializable` as sort of alias of `java.lang.Object`
    // since all natural target types do implement `Serializable` so assignment works
    public void testSerializable() throws Exception
    {
        final String JSON1 = aposToQuotes("{ 'value' : 123 }");
        SerContainer cont = MAPPER.readValue(JSON1, SerContainer.class);
        assertEquals(Integer.valueOf(123), cont.value);

        cont = MAPPER.readValue(aposToQuotes("{ 'value' : true }"), SerContainer.class);
        assertEquals(Boolean.TRUE, cont.value);

        // But also via Map value, even key
        Map<?,?> map = MAPPER.readValue(JSON1, new TypeReference<Map<String, Serializable>>() { });
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(123), map.get("value"));

        map = MAPPER.readValue(JSON1, new TypeReference<Map<Serializable, Object>>() { });
        assertEquals(1, map.size());
        assertEquals("value", map.keySet().iterator().next());
    }
}
