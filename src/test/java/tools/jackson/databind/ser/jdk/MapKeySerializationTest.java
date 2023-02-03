package tools.jackson.databind.ser.jdk;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.Base64Variants;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

public class MapKeySerializationTest extends BaseMapTest
{
    static class KarlSerializer extends ValueSerializer<String>
    {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider provider) {
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

    static enum Outer {
        inner;
    }

    enum ABC {
        A, B, C
    }

    static class ABCMapWrapper {
        public Map<ABC,String> stuff = new HashMap<ABC,String>();
        public ABCMapWrapper() {
            stuff.put(ABC.B, "bar");
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

    static class ABCKeySerializer extends ValueSerializer<ABC> {
        @Override
        public void serialize(ABC value, JsonGenerator gen,
                SerializerProvider provider) {
            gen.writeName("xxx"+value);
        }
    }

    static class NullKeySerializer extends ValueSerializer<Object>
    {
        private String _null;
        public NullKeySerializer(String s) { _null = s; }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) {
            gen.writeName(_null);
        }
    }

    static class NullValueSerializer extends ValueSerializer<Object>
    {
        private String _null;
        public NullValueSerializer(String s) { _null = s; }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) {
            gen.writeString(_null);
        }
    }

    static class DefaultKeySerializer extends ValueSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) {
            g.writeName("DEFAULT:"+value);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();

    public void testNotKarl() throws Exception {
        final String serialized = MAPPER.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", serialized);
    }

    public void testKarl() throws Exception {
        final String serialized = MAPPER.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", serialized);
    }

    // [databind#75]: caching of KeySerializers
    public void testBoth() throws Exception
    {
        // Let's NOT use shared one, to ensure caching starts from clean slate
        final ObjectMapper mapper = new ObjectMapper();
        final String value1 = mapper.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", value1);
        final String value2 = mapper.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", value2);
    }

    // Test custom key serializer for enum
    public void testCustomForEnum() throws Exception
    {
        // cannot use shared mapper as we are registering a module
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();

        String json = mapper.writeValueAsString(new ABCMapWrapper());
        assertEquals("{\"stuff\":{\"xxxB\":\"bar\"}}", json);
    }

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

    public void testCustomEnumInnerMapKey() throws Exception {
        Map<Outer, Object> outerMap = new HashMap<Outer, Object>();
        Map<ABC, Map<String, String>> map = new EnumMap<ABC, Map<String, String>>(ABC.class);
        Map<String, String> innerMap = new HashMap<String, String>();
        innerMap.put("one", "1");
        map.put(ABC.A, innerMap);
        outerMap.put(Outer.inner, map);
        SimpleModule mod = new SimpleModule("test")
                .setMixInAnnotation(ABC.class, ABCMixin.class)
                .addKeySerializer(ABC.class, new ABCKeySerializer())
        ;
        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();

        JsonNode tree = mapper.convertValue(outerMap, JsonNode.class);

        JsonNode innerNode = tree.get("inner");
        String key = innerNode.propertyNames().next();
        assertEquals("xxxA", key);
    }

    // 02-Nov-2020, tatu: No more "default key serializer" in 3.0, hence no test
    /*
    public void testDefaultKeySerializer() throws IOException
    {
        final SimpleModule mod = new SimpleModule()
                .setDefaultNullKeySerializer(new NullKeySerializer("NULL-KEY"))
                // 10-Oct-2019, tatu: Does not exist in 3.0.0 any more./..
                .setDefaultKeySerializer(new DefaultKeySerializer());
                ;
        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        assertEquals("{\"DEFAULT:a\":\"b\"}", m.writeValueAsString(map));
    }
    */

    // [databind#682]
    public void testClassKey() throws Exception
    {
        Map<Class<?>,Integer> map = new LinkedHashMap<Class<?>,Integer>();
        map.put(String.class, 2);
        String json = MAPPER.writeValueAsString(map);
        assertEquals(a2q("{'java.lang.String':2}"), json);
    }

    // [databind#838]
    public void testUnWrappedMapWithKeySerializer() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        final ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(mod)
                .build()
            ;
        Map<ABC,BAR<?>> stuff = new HashMap<ABC,BAR<?>>();
        stuff.put(ABC.B, new BAR<String>("bar"));
        String json = mapper.writerFor(new TypeReference<Map<ABC,BAR<?>>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"xxxB\":\"bar\"}", json);
    }

    // [databind#838]
    public void testUnWrappedMapWithDefaultType() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(
                NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY, JsonTypeInfo.Id.NAME, null)
            .typeIdVisibility(true);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .setDefaultTyping(typer)
                .build();

        Map<ABC,String> stuff = new HashMap<ABC,String>();
        stuff.put(ABC.B, "bar");
        String json = mapper.writerFor(new TypeReference<Map<ABC, String>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"@type\":\"HashMap\",\"xxxB\":\"bar\"}", json);
    }

    // [databind#1552]
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
    public void testMapKeyRecursion1679() throws Exception
    {
        Map<Object, Object> objectMap = new HashMap<Object, Object>();
        objectMap.put(new Object(), "foo");
        String json = MAPPER.writeValueAsString(objectMap);
        assertNotNull(json);
    }
}
