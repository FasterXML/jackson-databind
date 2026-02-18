package tools.jackson.databind.ser.jdk;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.core.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("serial")
public class MapSerializationTest extends DatabindTestUtil
{
    // // // Inner types from MapSerializationTest

    @JsonSerialize(using=PseudoMapSerializer.class)
    static class PseudoMap extends LinkedHashMap<String,String>
    {
        public PseudoMap(String... values) {
            for (int i = 0, len = values.length; i < len; i += 2) {
                put(values[i], values[i+1]);
            }
        }
    }

    static class PseudoMapSerializer extends ValueSerializer<Map<String,String>>
    {
        @Override
        public void serialize(Map<String,String> value,
                JsonGenerator gen, SerializationContext provider)
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

    // for [databind#691]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    @JsonTypeName("mymap")
    static class MapWithTypedValues extends LinkedHashMap<String,String> { }

    @JsonTypeInfo(use = Id.CLASS)
    public static class Mixin691 { }

    // // // Inner types from MapKeyAnnotationsTest

    // [databind#47]
    public static class Wat
    {
        private final String wat;

        @JsonCreator
        Wat(String wat) {
            this.wat = wat;
        }

        @JsonValue
        public String getWat() {
            return wat;
        }

        @Override
        public String toString() {
            return "(String)[Wat: " + wat + "]";
        }
    }

    @SuppressWarnings("serial")
    static class WatMap extends HashMap<Wat,Boolean> { }

    // [databind#943]
    static class UCString {
        private String value;

        public UCString(String v) {
            value = v.toUpperCase();
        }

        @JsonValue
        public String asString() {
            return value;
        }
    }

    enum AbcLC {
        A, B, C;

        @JsonValue
        public String toLC() {
            return name().toLowerCase();
        }
    }

    // [databind#2306]
    static class JsonValue2306Key {
        @JsonValue
        private String id;

        public JsonValue2306Key(String id) {
            this.id = id;
        }
    }

    // [databind#2871]
    static class Inner2871 {
        @JsonKey
        String key;

        @JsonValue
        String value;

        Inner2871(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Inner2871(" + this.key + "," + this.value + ")";
        }
    }

    static class Outer2871 {
        @JsonKey
        @JsonValue
        Inner2871 inner;

        Outer2871(Inner2871 inner) {
            this.inner = inner;
        }
    }

    static class NoKeyOuter {
        @JsonValue
        Inner2871 inner;

        NoKeyOuter(Inner2871 inner) {
            this.inner = inner;
        }
    }

    // // // Inner types from MapKeySerializationTest

    static class KarlSerializer extends ValueSerializer<String>
    {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
            gen.writeName("Karl");
        }
    }

    static class NotKarlBean
    {
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    static class KarlBean
    {
        @JsonSerialize(keyUsing = KarlSerializer.class)
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    static enum OuterEnum {
        inner;
    }

    enum ABCKey {
        A, B, C
    }

    static class ABCMapWrapper {
        public Map<ABCKey,String> stuff = new HashMap<ABCKey,String>();
        public ABCMapWrapper() {
            stuff.put(ABCKey.B, "bar");
        }
    }

    @JsonSerialize(keyUsing = ABCKeySerializer.class)
    static enum ABCMixin { }

    static class BAR<T> {
        T value;

        public BAR(T value) {
            this.value = value;
        }

        @JsonValue
        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName()
                    + ", value:" + value
                    ;
        }
    }

    static class ABCKeySerializer extends ValueSerializer<ABCKey> {
        @Override
        public void serialize(ABCKey value, JsonGenerator gen,
                SerializationContext provider) {
            gen.writeName("xxx"+value);
        }
    }

    static class NullKeySerializer extends ValueSerializer<Object>
    {
        private String _null;
        public NullKeySerializer(String s) { _null = s; }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext provider) {
            gen.writeName(_null);
        }
    }

    static class NullValueSerializer extends ValueSerializer<Object>
    {
        private String _null;
        public NullValueSerializer(String s) { _null = s; }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext provider) {
            gen.writeString(_null);
        }
    }

    static class DefaultKeySerializer extends ValueSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
            g.writeName("DEFAULT:"+value);
        }
    }

    // // // Inner types from MapSerializationSorted4773Test

    public static class IncomparableContainer4773 {
        public Map<Currency, String> exampleMap = new HashMap<>();
    }

    public static class ObjectContainer4773 {
        public Map<Object, String> exampleMap = new HashMap<>();
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper SORTING_MAPPER = jsonMapperBuilder()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .build();

    // // // Tests from MapSerializationTest

    @Test
    public void testUsingObjectWriter() throws IOException
    {
        ObjectWriter w = MAPPER.writerFor(Object.class);
        Map<String,Object> map = new LinkedHashMap<String,Object>();
        map.put("a", 1);
        String json = w.writeValueAsString(map);
        assertEquals(a2q("{'a':1}"), json);
    }

    @Test
    public void testMapSerializer() throws IOException
    {
        assertEquals("\"{a=b, c=d}\"", MAPPER.writeValueAsString(new PseudoMap("a", "b", "c", "d")));
    }

    // problems with map entries, values
    @Test
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
    @Test
    public void testOrderByKey() throws IOException
    {
        ObjectMapper m = newJsonMapper();
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
    @Test
    public void testOrderByWithNulls() throws IOException
    {
        ObjectWriter sortingW = MAPPER.writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .without(SerializationFeature.FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY);
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
    @Test
    public void testOrderByKeyViaProperty() throws IOException
    {
        MapOrderingBean input = new MapOrderingBean("c", "b", "a");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'map':{'a':3,'b':2,'c':1}}"), json);
    }

    // [databind#691]
    @Test
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
    @Test
    public void testNullJsonInTypedMap691() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("NULL", null);

        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(Object.class, Mixin691.class)
                .build();
        String json = mapper.writeValueAsString(map);
        assertEquals("{\"@class\":\"java.util.HashMap\",\"NULL\":null}", json);
    }

    // [databind#1513]
    @Test
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

    // // // Tests from MapKeyAnnotationsTest

    // [databind#47]
    @Test
    public void testMapJsonValueKey47() throws Exception
    {
        WatMap input = new WatMap();
        input.put(new Wat("3"), true);

        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'3':true}"), json);
    }

    // [databind#943]
    @Test
    public void testDynamicMapKeys() throws Exception
    {
        Map<Object,Integer> stuff = new LinkedHashMap<Object,Integer>();
        stuff.put(AbcLC.B, Integer.valueOf(3));
        stuff.put(new UCString("foo"), Integer.valueOf(4));
        String json = MAPPER.writeValueAsString(stuff);
        assertEquals(a2q("{'b':3,'FOO':4}"), json);
    }

    // [databind#2306]
    @Test
    public void testMapKeyWithJsonValue() throws Exception
    {
        final Map<JsonValue2306Key, String> map = Collections.singletonMap(
                new JsonValue2306Key("myId"), "value");
        assertEquals(a2q("{'myId':'value'}"),
                MAPPER.writeValueAsString(map));
    }

    // [databind#2871]
    @Test
    public void testClassAsKey() throws Exception {
        Outer2871 outer = new Outer2871(new Inner2871("innerKey", "innerValue"));
        Map<Outer2871, String> map = Collections.singletonMap(outer, "value");
        String actual = MAPPER.writeValueAsString(map);
        assertEquals("{\"innerKey\":\"value\"}", actual);
    }

    // [databind#2871]
    @Test
    public void testClassAsValue() throws Exception {
        Map<String, Outer2871> mapA = Collections.singletonMap("key", new Outer2871(new Inner2871("innerKey", "innerValue")));
        String actual = MAPPER.writeValueAsString(mapA);
        assertEquals("{\"key\":\"innerValue\"}", actual);
    }

    // [databind#2871]
    @Test
    public void testNoKeyOuter() throws Exception {
        Map<String, NoKeyOuter> mapA = Collections.singletonMap("key", new NoKeyOuter(new Inner2871("innerKey", "innerValue")));
        String actual = MAPPER.writeValueAsString(mapA);
        assertEquals("{\"key\":\"innerValue\"}", actual);
    }

    // // // Tests from MapKeySerializationTest

    @Test
    public void testNotKarl() throws Exception {
        final String serialized = MAPPER.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", serialized);
    }

    @Test
    public void testKarl() throws Exception {
        final String serialized = MAPPER.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", serialized);
    }

    // [databind#75]: caching of KeySerializers
    @Test
    public void testBoth() throws Exception
    {
        // Let's NOT use shared one, to ensure caching starts from clean slate
        final ObjectMapper mapper = newJsonMapper();
        final String value1 = mapper.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", value1);
        final String value2 = mapper.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", value2);
    }

    // Test custom key serializer for enum
    @Test
    public void testCustomForEnum() throws Exception
    {
        // cannot use shared mapper as we are registering a module
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABCKey.class, new ABCKeySerializer());
        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();

        String json = mapper.writeValueAsString(new ABCMapWrapper());
        assertEquals("{\"stuff\":{\"xxxB\":\"bar\"}}", json);
    }

    @Test
    public void testCustomNullSerializers() throws Exception
    {
        final SimpleModule mod = new SimpleModule()
                .setDefaultNullKeySerializer(new NullKeySerializer("NULL-KEY"))
                .setDefaultNullValueSerializer(new NullValueSerializer("NULL"));
        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        Map<String,Integer> input = new HashMap<>();
        input.put(null, 3);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"NULL-KEY\":3}", json);
        json = mapper.writeValueAsString(new Object[] { 1, null, true });
        assertEquals("[1,\"NULL\",true]", json);
    }

    @Test
    public void testCustomEnumInnerMapKey() throws Exception {
        Map<OuterEnum, Object> outerMap = new HashMap<OuterEnum, Object>();
        Map<ABCKey, Map<String, String>> map = new EnumMap<ABCKey, Map<String, String>>(ABCKey.class);
        Map<String, String> innerMap = new HashMap<String, String>();
        innerMap.put("one", "1");
        map.put(ABCKey.A, innerMap);
        outerMap.put(OuterEnum.inner, map);
        SimpleModule mod = new SimpleModule("test")
                .setMixInAnnotation(ABCKey.class, ABCMixin.class)
                .addKeySerializer(ABCKey.class, new ABCKeySerializer())
        ;
        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();

        JsonNode tree = mapper.convertValue(outerMap, JsonNode.class);

        JsonNode innerNode = tree.get("inner");
        String key = innerNode.propertyNames().iterator().next();
        assertEquals("xxxA", key);
    }

    // [databind#682]
    @Test
    public void testClassKey() throws Exception
    {
        Map<Class<?>,Integer> map = new LinkedHashMap<Class<?>,Integer>();
        map.put(String.class, 2);
        String json = MAPPER.writeValueAsString(map);
        assertEquals(a2q("{'java.lang.String':2}"), json);
    }

    // [databind#838]
    @Test
    public void testUnWrappedMapWithKeySerializer() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABCKey.class, new ABCKeySerializer());
        final ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(mod)
                .build()
            ;
        Map<ABCKey,BAR<?>> stuff = new HashMap<ABCKey,BAR<?>>();
        stuff.put(ABCKey.B, new BAR<String>("bar"));
        String json = mapper.writerFor(new TypeReference<Map<ABCKey,BAR<?>>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"xxxB\":\"bar\"}", json);
    }

    // [databind#838]
    @Test
    public void testUnWrappedMapWithDefaultType() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABCKey.class, new ABCKeySerializer());
        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(
                NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY, JsonTypeInfo.Id.NAME, null)
            .typeIdVisibility(true);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .setDefaultTyping(typer)
                .build();

        Map<ABCKey,String> stuff = new HashMap<ABCKey,String>();
        stuff.put(ABCKey.B, "bar");
        String json = mapper.writerFor(new TypeReference<Map<ABCKey, String>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"@type\":\"HashMap\",\"xxxB\":\"bar\"}", json);
    }

    // [databind#1552]
    @Test
    public void testMapsWithBinaryKeys() throws Exception
    {
        byte[] binary = new byte[] { 1, 2, 3, 4, 5 };

        // First, using wrapper
        MapWrapper<byte[], String> input = new MapWrapper<>(binary, "stuff");
        String expBase64 = Base64Variants.MIME.encode(binary);

        assertEquals(a2q("{'map':{'"+expBase64+"':'stuff'}}"),
                MAPPER.writeValueAsString(input));

        // and then dynamically..
        Map<byte[],String> map = new LinkedHashMap<>();
        map.put(binary, "xyz");
        assertEquals(a2q("{'"+expBase64+"':'xyz'}"),
                MAPPER.writeValueAsString(map));
    }

    // [databind#1679]
    @Test
    public void testMapKeyRecursion1679() throws Exception
    {
        Map<Object, Object> objectMap = new HashMap<Object, Object>();
        objectMap.put(new Object(), "foo");
        String json = MAPPER.writeValueAsString(objectMap);
        assertNotNull(json);
    }

    // // // Tests from MapSerializationSorted4773Test

    // [databind#4773]
    @Test
    void testSerializationFailureWhenEnabledWithIncomparableKeys()
            throws Exception
    {
        IncomparableContainer4773 entity = new IncomparableContainer4773();
        entity.exampleMap.put(Currency.getInstance("GBP"), "GBP_TEXT");
        entity.exampleMap.put(Currency.getInstance("AUD"), "AUD_TEXT");

        try {
            SORTING_MAPPER.writer()
                .with(SerializationFeature.FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY)
                .writeValueAsString(entity);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot order Map entries by key of incomparable type");
        }
    }

    // [databind#4773]
    @Test
    void testSerializationWithGenericObjectKeys()
            throws Exception
    {
        ObjectContainer4773 entity = new ObjectContainer4773();
        entity.exampleMap.put(5, "N_TEXT");
        entity.exampleMap.put(1, "GBP_TEXT");
        entity.exampleMap.put(3, "T_TEXT");
        entity.exampleMap.put(4, "AUD_TEXT");
        entity.exampleMap.put(2, "KRW_TEXT");

        String jsonResult = SORTING_MAPPER.writeValueAsString(entity);

        assertEquals(a2q("{'exampleMap':{" +
                "'1':'GBP_TEXT'," +
                "'2':'KRW_TEXT'," +
                "'3':'T_TEXT'," +
                "'4':'AUD_TEXT'," +
                "'5':'N_TEXT'}}"), jsonResult);
    }

    // [databind#4773]
    @Test
    void testSerWithNullType()
            throws Exception
    {
        ObjectContainer4773 entity = new ObjectContainer4773();
        entity.exampleMap.put(null, "AUD_TEXT");

        try {
            SORTING_MAPPER.writer()
                .with(SerializationFeature.FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY)
                .writeValueAsString(entity);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot order Map entries by key of incomparable type [null]");
        }
    }
}
