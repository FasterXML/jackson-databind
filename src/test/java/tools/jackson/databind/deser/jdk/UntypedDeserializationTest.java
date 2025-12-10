package tools.jackson.databind.deser.jdk;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Unit tests for verifying "raw" (or "untyped") data binding from JSON to JDK objects;
 * one that only uses core JDK types; wrappers, Maps and Lists.
 */
public class UntypedDeserializationTest
{
    static class UCStringDeserializer
        extends StdScalarDeserializer<String>
    {
        public UCStringDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getString().toUpperCase();
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
        public Number deserialize(JsonParser p, DeserializationContext ctxt) {
            return value;
        }
    }

    // Let's make this Contextual, to tease out cyclic resolution issues, if any
    static class ListDeserializer extends StdDeserializer<List<Object>>
    {
        public ListDeserializer() { super(List.class); }

        @Override
        public List<Object> deserialize(JsonParser p, DeserializationContext ctxt)
        {
            ArrayList<Object> list = new ArrayList<Object>();
            while (p.nextValue() != JsonToken.END_ARRAY) {
                list.add("X"+p.getString());
            }
            return list;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property)
        {
            // For now, we just need to access "untyped" deserializer; not use it.

            /*ValueDeserializer<Object> ob = */
            ctxt.findContextualValueDeserializer(ctxt.constructType(Object.class), property);
            return this;
        }
    }

    static class YMapDeserializer extends StdDeserializer<Map<String,Object>>
    {
        public YMapDeserializer() { super(Map.class); }

        @Override
        public Map<String,Object> deserialize(JsonParser p, DeserializationContext ctxt)
        {
            Map<String,Object> map = new LinkedHashMap<String,Object>();
            while (p.nextValue() != JsonToken.END_OBJECT) {
                map.put(p.currentName(), "Y"+p.getString());
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

        protected WrappedPolymorphicUntyped() { }
        public WrappedPolymorphicUntyped(Object o) { value = o; }
    }

    // [databind#1460]
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

    private final JsonMapper MAPPER = newJsonMapper();

    @SuppressWarnings("unchecked")
    @Test
    public void testSampleDoc() throws Exception
    {
        final String JSON = SAMPLE_DOC_JSON_SPEC;

        /* To get "untyped" Mapping (to Maps, Lists, instead of beans etc),
         * we'll specify plain old Object.class as the target.
         */
        Object root = MAPPER.readValue(JSON, Object.class);

        assertInstanceOf(Map.class, root);
        Map<?,?> rootMap = (Map<?,?>) root;
        assertEquals(1, rootMap.size());
        Map.Entry<?,?> rootEntry =  rootMap.entrySet().iterator().next();
        assertEquals("Image", rootEntry.getKey());
        Object image = rootEntry.getValue();
        assertInstanceOf(Map.class, image);
        Map<?,?> imageMap = (Map<?,?>) image;
        assertEquals(5, imageMap.size());

        Object value = imageMap.get("Width");
        assertInstanceOf(Integer.class, value);
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_WIDTH), value);

        value = imageMap.get("Height");
        assertInstanceOf(Integer.class, value);
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_HEIGHT), value);

        assertEquals(SAMPLE_SPEC_VALUE_TITLE, imageMap.get("Title"));

        // Another Object, "thumbnail"
        value = imageMap.get("Thumbnail");
        assertInstanceOf(Map.class, value);
        Map<?,?> tnMap = (Map<?,?>) value;
        assertEquals(3, tnMap.size());

        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_HEIGHT), tnMap.get("Height"));
        // for some reason, width is textual, not numeric...
        assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, tnMap.get("Width"));
        assertEquals(SAMPLE_SPEC_VALUE_TN_URL, tnMap.get("Url"));

        // And then number list, "IDs"
        value = imageMap.get("IDs");
        assertInstanceOf(List.class, value);
        List<Object> ids = (List<Object>) value;
        assertEquals(4, ids.size());
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID1), ids.get(0));
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID2), ids.get(1));
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID3), ids.get(2));
        assertEquals(Integer.valueOf(SAMPLE_SPEC_VALUE_TN_ID4), ids.get(3));

        // and that's all folks!
    }

    @Test
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

    @Test
    public void testSimpleVanillaScalars() throws Exception
    {
        assertEquals("foo", MAPPER.readValue(q("foo"), Object.class));

        assertEquals(Boolean.TRUE, MAPPER.readValue(" true ", Object.class));

        assertEquals(Integer.valueOf(13), MAPPER.readValue("13 ", Object.class));
        assertEquals(Double.valueOf(0.5), MAPPER.readValue("0.5 ", Object.class));
    }

    @Test
    public void testSimpleVanillaStructured() throws Exception
    {
        List<?> list = (List<?>) MAPPER.readValue("[ 1, 2, 3]", Object.class);
        assertEquals(Integer.valueOf(1), list.get(0));
    }

    @Test
    public void testNestedUntypes() throws Exception
    {
        // 05-Apr-2014, tatu: Odd failures if using shared mapper; so work around:
        Object root = MAPPER.readValue(a2q("{'a':3,'b':[1,2]}"),
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

    @Test
    public void testUntypedWithCustomScalarDesers() throws Exception
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(String.class, new UCStringDeserializer());
        m.addDeserializer(Number.class, new CustomNumberDeserializer(13));
        final ObjectMapper mapper = jsonMapperBuilder()
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
    @Test
    public void testNonVanilla() throws Exception
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(String.class, new UCStringDeserializer());
        final ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(new NoCheckSubTypeValidator())
                .addModule(m)
                .build();
        ObjectReader r = mapper.readerFor(Object.class);
        // Also: since this is now non-vanilla variant, try more alternatives
        List<?> l = (List<?>) r.readValue("[ true, false, 7, 0.5, \"foo\", null]");
        assertEquals(6, l.size());
        assertEquals(Boolean.TRUE, l.get(0));
        assertEquals(Boolean.FALSE, l.get(1));
        assertEquals(Integer.valueOf(7), l.get(2));
        assertEquals(Double.valueOf(0.5), l.get(3));
        assertEquals("FOO", l.get(4));
        assertNull(l.get(5));

        // And Maps
        Map<?,?> map = (Map<?,?>) r.readValue(a2q("{'a':0.25,'b':3,'c':true,'d':false}"));
        assertEquals(Map.of("a", 0.25, "b", 3, "c", true, "d", false), map);

        // And Scalars too; regular and "updating" readers
        l = new ArrayList<>();
        assertEquals(Integer.valueOf(42), r.readValue("42"));
        assertEquals(Integer.valueOf(42), r.withValueToUpdate(l).readValue("42"));
        assertEquals(Double.valueOf(2.5), r.readValue("2.5"));
        assertEquals(Double.valueOf(2.5), r.withValueToUpdate(l).readValue("2.5"));
        assertEquals(true, r.readValue("true"));
        assertEquals(true, r.withValueToUpdate(l).readValue("true"));
        assertEquals(false, r.readValue("false"));
        assertEquals(false, r.withValueToUpdate(l).readValue("false"));
        assertNull(r.readValue("null"));
        assertSame(l, r.withValueToUpdate(l).readValue("null"));
        
        // and minimal nesting
        l = (List<?>) mapper.readValue("[ {}, [] ]", Object.class);
        assertEquals(2, l.size());
        assertEquals(Map.of(), l.get(0));
        assertEquals(List.of(), l.get(1));
    }

    @Test
    public void testUntypedWithListDeser() throws Exception
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(List.class, new ListDeserializer());
        ObjectMapper mapper = jsonMapperBuilder()
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

    @Test
    public void testUntypedWithMapDeser() throws Exception
    {
        SimpleModule m = new SimpleModule("test-module");
        m.addDeserializer(Map.class, new YMapDeserializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(m)
                .build();
        // And then list...
        Object ob = mapper.readValue("{\"a\":true}", Object.class);
        assertTrue(ob instanceof Map<?,?>);
        Map<?,?> map = (Map<?,?>) ob;
        assertEquals(1, map.size());
        assertEquals("Ytrue", map.get("a"));
    }

    @Test
    public void testNestedUntyped989() throws Exception
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

    @Test
    public void testUntypedWithJsonArrays() throws Exception
    {
        // by default we get:
        Object ob = MAPPER.readValue("[1]", Object.class);
        assertTrue(ob instanceof List<?>);

        // but can change to produce Object[]:
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
                .build();
        ob = mapper.readValue("[1, false, true, 0.5, {}]", Object.class);
        assertEquals(Object[].class, ob.getClass());
        assertEquals(List.of(1, false, true, 0.5, Map.of()),
                Arrays.asList((Object[]) ob));
    }

    @Test
    public void testUntypedIntAsLong() throws Exception
    {
        final String JSON = a2q("{'value':3}");
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
    @Test
    public void testSerializable() throws Exception
    {
        final String JSON1 = a2q("{ 'value' : 123 }");
        SerContainer cont = MAPPER.readValue(JSON1, SerContainer.class);
        assertEquals(Integer.valueOf(123), cont.value);

        cont = MAPPER.readValue(a2q("{ 'value' : true }"), SerContainer.class);
        assertEquals(Boolean.TRUE, cont.value);

        // But also via Map value, even key
        Map<?,?> map = MAPPER.readValue(JSON1, new TypeReference<Map<String, Serializable>>() { });
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(123), map.get("value"));

        map = MAPPER.readValue(JSON1, new TypeReference<Map<Serializable, Object>>() { });
        assertEquals(1, map.size());
        assertEquals("value", map.keySet().iterator().next());
    }

    /*
    /**********************************************************
    /* Test methods, merging
    /**********************************************************
     */

    @Test
    public void testValueUpdateVanillaUntyped() throws Exception
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 42);

        // First update Map with JSON Object
        ObjectReader r = MAPPER.readerFor(Object.class).withValueToUpdate(map);
        Object result;

        result = r.readValue(a2q("{'b': 0.25, 'c': [] }"));
        assertSame(map, result);
        assertEquals(3, map.size());
        assertEquals(0.25, map.get("b"));
        assertEquals(List.of(), map.get("c"));

        // Then List with Array
        List<Object> list = new ArrayList<>();
        list.add(1);
        r = MAPPER.readerFor(Object.class).withValueToUpdate(list);
        result = r.readValue("[ true, -0.5, { } ]");
        assertSame(list, result);
        assertEquals(List.of(1, true, -0.5, Map.of()), result);

        // Then mismatches: Map with JSON Array
        r = MAPPER.readerFor(Object.class)
                .withValueToUpdate(map);
        result = r.readValue("[ 42, -0.25, false, null ]");
        List<Object> exp = new ArrayList<>();
        exp.add(42);
        exp.add(-0.25);
        exp.add(false);
        exp.add(null);
        assertEquals(exp, result);

        // And then List with JSON Object
        r = MAPPER.readerFor(Object.class)
                .withValueToUpdate(new ArrayList<>());
        map.clear();
        map.put("a", 0.5);
        map.put("b", null);
        result = r.readValue(a2q("{'a': 0.5, 'b': null}"));
        assertEquals(map, result);
    }

    @Test
    public void testValueUpdateCustomUntyped() throws Exception
    {
        SimpleModule m = new SimpleModule("test-module")
                .addDeserializer(String.class, new UCStringDeserializer());
        final ObjectMapper customMapper = jsonMapperBuilder()
                .addModule(m)
                .build();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 42);

        ObjectReader r = customMapper.readerFor(Object.class).withValueToUpdate(map);
        Object result = r.readValue(a2q("{'b': 'value', 'c': 111222333444, 'enabled': true}"));
        assertSame(map, result);
        assertEquals(4, map.size());
        assertEquals("VALUE", map.get("b"));
        assertEquals(Long.valueOf(111222333444L), map.get("c"));
        assertEquals(Boolean.TRUE, map.get("enabled"));

        // Try same with other types, too
        List<Object> list = new ArrayList<>();
        list.add(1);
        r = customMapper.readerFor(Object.class).withValueToUpdate(list);
        result = r.readValue(a2q("[ 2, 'foobar' ]"));
        assertSame(list, result);
        assertEquals(3, list.size());
        assertEquals("FOOBAR", list.get(2));
    }

    /*
    /**********************************************************
    /* Test methods, polymorphic
    /**********************************************************
     */

    // Allow 'upgrade' of big integers into Long, BigInteger
    @Test
    public void testObjectSerializeWithLong() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.JAVA_LANG_OBJECT, As.PROPERTY)
                .build();
        final long VALUE = 1337800584532L;

        String serialized = "{\"timestamp\":"+VALUE+"}";
        // works fine as node
        JsonNode deserialized = mapper.readTree(serialized);
        assertEquals(VALUE, deserialized.get("timestamp").asLong());
        // and actually should work for Maps too
        Map<?,?> deserMap = mapper.readValue(serialized, Map.class);
        Number n = (Number) deserMap.get("timestamp");
        assertNotNull(n);
        assertSame(Long.class, n.getClass());
        assertEquals(Long.valueOf(VALUE), n);
    }

    @Test
    public void testPolymorphicUntypedVanilla() throws Exception
    {
        ObjectReader rDefault = jsonMapperBuilder()
                .polymorphicTypeValidator(new NoCheckSubTypeValidator())
                .build()
                .readerFor(WrappedPolymorphicUntyped.class);
        ObjectReader rAlt = rDefault
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                        DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        WrappedPolymorphicUntyped w;

        w = rDefault.readValue(a2q("{'value':10}"));
        assertEquals(Integer.valueOf(10), w.value);
        w = rAlt.readValue(a2q("{'value':10}"));
        assertEquals(BigInteger.TEN, w.value);

        w = rDefault.readValue(a2q("{'value':5.0}"));
        assertEquals(Double.valueOf(5.0), w.value);
        w = rAlt.readValue(a2q("{'value':5.0}"));
        assertEquals(new BigDecimal("5.0"), w.value);

        StringBuilder sb = new StringBuilder(100).append("[0");
        for (int i = 1; i < 100; ++i) {
            sb.append(", ").append(i);
        }
        sb.append("]");
        final String INT_ARRAY_JSON = sb.toString();

        // First read as-is, no type wrapping
        Object ob = MAPPER.readerFor(Object.class)
                .with(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
                .readValue(INT_ARRAY_JSON);
        assertTrue(ob instanceof Object[]);
        Object[] obs = (Object[]) ob;
        for (int i = 0; i < 100; ++i) {
            assertEquals(Integer.valueOf(i), obs[i]);
        }

        // Finally, true polymorphism
        w = rDefault.readValue(a2q("{'value': ['java.util.Date', 123]}"));
        assertThat(w.value).isInstanceOf(java.util.Date.class);
    }

    @Test
    public void testPolymorphicUntypedCustom() throws Exception
    {
        // register module just to override one deserializer, to prevent use of Vanilla deser
        SimpleModule m = new SimpleModule("test-module")
                .addDeserializer(String.class, new UCStringDeserializer());
        final ObjectMapper customMapper = jsonMapperBuilder()
                .addModule(m)
                .polymorphicTypeValidator(new NoCheckSubTypeValidator())
                .build();
        ObjectReader rDefault = customMapper.readerFor(WrappedPolymorphicUntyped.class);

        WrappedPolymorphicUntyped w = rDefault.readValue(a2q("{'value':10}"));
        assertEquals(Integer.valueOf(10), w.value);

        w = rDefault.readValue(a2q("{'value':9988776655}"));
        assertEquals(Long.valueOf(9988776655L), w.value);
        w = rDefault.readValue(a2q("{'value':0.75}"));
        assertEquals(Double.valueOf(0.75), w.value);

        w = rDefault.readValue(a2q("{'value':'abc'}"));
        assertEquals("ABC", w.value);
        w = rDefault.readValue(a2q("{'value':false}"));
        assertEquals(Boolean.FALSE, w.value);
        w = rDefault.readValue(a2q("{'value':null}"));
        assertNull(w.value);

        // but... actually how about real type id?
        final Object SHORT = Short.valueOf((short) 3);
        String json = customMapper.writeValueAsString(new WrappedPolymorphicUntyped(SHORT));

        WrappedPolymorphicUntyped result = rDefault.readValue(json);
        assertEquals(SHORT, result.value);
    }

    /*
    /**********************************************************
    /* Test methods, additional coverage
    /**********************************************************
     */

    @Test
    public void testEmptyArrayAndObject() throws Exception
    {
        assertEquals(List.of(), MAPPER.readValue("[]", Object.class));
        assertEquals(Map.of(), MAPPER.readValue("{}", Object.class));
    }

    @Test
    public void testSingleElementArrayAndObject() throws Exception
    {
        assertEquals(List.of(42), MAPPER.readValue("[42]", Object.class));
        assertEquals(Map.of("key", true),
                MAPPER.readValue("{\"key\": true}", Object.class));
    }

    @Test
    public void testFloatDeserializationWithBigDecimal() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();

        Object result = mapper.readValue("3.14159", Object.class);
        assertNotNull(result);
        assertTrue(result instanceof BigDecimal);
        assertEquals(new BigDecimal("3.14159"), result);
    }

    @Test
    public void testFloatTypesFloat32AndFloat64() throws Exception
    {
        Object result = MAPPER.readValue("1.5", Object.class);
        assertTrue(result instanceof Double);
        result = MAPPER.readValue("2.718281828", Object.class);
        assertTrue(result instanceof Double);
    }

    @Test
    public void testNaNHandling() throws Exception
    {
        // NaN values should be handled specially
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();

        Object result = mapper.readValue("NaN", Object.class);
        assertTrue(result instanceof Double);
        assertTrue(((Double) result).isNaN());
    }

    @Test
    public void testNullInDifferentContexts() throws Exception
    {
        // Null as root value
        Object result = MAPPER.readValue("null", Object.class);
        assertNull(result);

        // Null in array
        List<?> list = (List<?>) MAPPER.readValue("[null, 1, null]", Object.class);
        assertEquals(3, list.size());
        assertNull(list.get(0));
        assertEquals(Integer.valueOf(1), list.get(1));
        assertNull(list.get(2));

        // Null in object
        Map<?,?> map = (Map<?,?>) MAPPER.readValue("{\"a\":null,\"b\":2}", Object.class);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertNull(map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testBigIntegerCoercion() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .build();

        Object result = mapper.readValue("12345678901234567890", Object.class);
        assertNotNull(result);
        assertTrue(result instanceof BigInteger);
        assertEquals(new BigInteger("12345678901234567890"), result);
    }

    @Test
    public void testLargeArray() throws Exception
    {
        // Test array with more than 2 elements to trigger ObjectBuffer usage
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) json.append(",");
            json.append(i);
        }
        json.append("]");

        List<?> result = (List<?>) MAPPER.readValue(json.toString(), Object.class);
        assertEquals(100, result.size());
        assertEquals(Integer.valueOf(0), result.get(0));
        assertEquals(Integer.valueOf(99), result.get(99));
    }

    @Test
    public void testLargeObject() throws Exception
    {
        // Test object with more than 2 properties
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < 50; i++) {
            if (i > 0) json.append(",");
            json.append("\"key").append(i).append("\":").append(i);
        }
        json.append("}");

        Map<?,?> result = (Map<?,?>) MAPPER.readValue(json.toString(), Object.class);
        assertEquals(50, result.size());
        assertEquals(Integer.valueOf(0), result.get("key0"));
        assertEquals(Integer.valueOf(49), result.get("key49"));
    }

    @Test
    public void testMixedTypesInArray() throws Exception
    {
        String json = "[1, \"text\", true, null, 3.14, {\"nested\":\"object\"}, [1,2,3]]";
        List<?> result = (List<?>) MAPPER.readValue(json, Object.class);

        assertEquals(7, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals("text", result.get(1));
        assertEquals(Boolean.TRUE, result.get(2));
        assertNull(result.get(3));
        assertEquals(Double.valueOf(3.14), result.get(4));
        assertTrue(result.get(5) instanceof Map);
        assertTrue(result.get(6) instanceof List);
    }

    @Test
    public void testDeeplyNestedStructures() throws Exception
    {
        String json = "{\"level1\":{\"level2\":{\"level3\":{\"level4\":{\"value\":\"deep\"}}}}}";
        Map<?,?> result = (Map<?,?>) MAPPER.readValue(json, Object.class);

        Map<?,?> level1 = (Map<?,?>) result.get("level1");
        Map<?,?> level2 = (Map<?,?>) level1.get("level2");
        Map<?,?> level3 = (Map<?,?>) level2.get("level3");
        Map<?,?> level4 = (Map<?,?>) level3.get("level4");
        assertEquals("deep", level4.get("value"));
    }

    @Test
    public void testObjectWithArraysAndObjects() throws Exception
    {
        String json = "{\"numbers\":[1,2,3],\"nested\":{\"flag\":true},\"text\":\"hello\"}";
        Map<?,?> result = (Map<?,?>) MAPPER.readValue(json, Object.class);

        assertEquals(3, result.size());
        List<?> numbers = (List<?>) result.get("numbers");
        assertEquals(3, numbers.size());

        Map<?,?> nested = (Map<?,?>) result.get("nested");
        assertEquals(Boolean.TRUE, nested.get("flag"));

        assertEquals("hello", result.get("text"));
    }
}
