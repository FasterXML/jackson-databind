package com.fasterxml.jackson.databind.ser.jdk;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class MapKeySerializationTest extends BaseMapTest
{
    public static class KarlSerializer extends JsonSerializer<String>
    {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeFieldName("Karl");
        }
    }

    public static class NotKarlBean
    {
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    public static class KarlBean
    {
        @JsonSerialize(keyUsing = KarlSerializer.class)
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    public static enum Outer {
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
    public static enum ABCMixin { }

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

    static class ABCKeySerializer extends JsonSerializer<ABC> {
        @Override
        public void serialize(ABC value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeFieldName("xxx"+value);
        }
    }

    public static class NullKeySerializer extends JsonSerializer<Object>
    {
        private String _null;
        public NullKeySerializer(String s) { _null = s; }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeFieldName(_null);
        }
    }

    public static class NullValueSerializer extends JsonSerializer<Object>
    {
        private String _null;
        public NullValueSerializer(String s) { _null = s; }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(_null);
        }
    }

    static class DefaultKeySerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException
        {
            g.writeFieldName("DEFAULT:"+value);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();

    public void testNotKarl() throws IOException {
        final String serialized = MAPPER.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", serialized);
    }

    public void testKarl() throws IOException {
        final String serialized = MAPPER.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", serialized);
    }

    // [databind#75]: caching of KeySerializers
    public void testBoth() throws IOException
    {
        // Let's NOT use shared one, to ensure caching starts from clean slate
        final ObjectMapper mapper = new ObjectMapper();
        final String value1 = mapper.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", value1);
        final String value2 = mapper.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", value2);
    }

    // Test custom key serializer for enum
    public void testCustomForEnum() throws IOException
    {
        // cannot use shared mapper as we are registering a module
        final ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        mapper.registerModule(mod);

        String json = mapper.writeValueAsString(new ABCMapWrapper());
        assertEquals("{\"stuff\":{\"xxxB\":\"bar\"}}", json);
    }

    public void testCustomNullSerializers() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer("NULL-KEY"));
        mapper.getSerializerProvider().setNullValueSerializer(new NullValueSerializer("NULL"));
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
        final ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test");
        mod.setMixInAnnotation(ABC.class, ABCMixin.class);
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        mapper.registerModule(mod);

        JsonNode tree = mapper.convertValue(outerMap, JsonNode.class);

        JsonNode innerNode = tree.get("inner");
        String key = innerNode.fieldNames().next();
        assertEquals("xxxA", key);
    }

    public void testDefaultKeySerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.getSerializerProvider().setDefaultKeySerializer(new DefaultKeySerializer());
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        assertEquals("{\"DEFAULT:a\":\"b\"}", m.writeValueAsString(map));
    }

    // [databind#682]
    public void testClassKey() throws IOException
    {
        Map<Class<?>,Integer> map = new LinkedHashMap<Class<?>,Integer>();
        map.put(String.class, 2);
        String json = MAPPER.writeValueAsString(map);
        assertEquals(a2q("{'java.lang.String':2}"), json);
    }

    // [databind#838]
    @SuppressWarnings("deprecation")
    public void testUnWrappedMapWithKeySerializer() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        final ObjectMapper mapper = new ObjectMapper()
            .registerModule(mod)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_NULL_MAP_VALUES)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            ;

        Map<ABC,BAR<?>> stuff = new HashMap<ABC,BAR<?>>();
        stuff.put(ABC.B, new BAR<String>("bar"));
        String json = mapper.writerFor(new TypeReference<Map<ABC,BAR<?>>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"xxxB\":\"bar\"}", json);
    }

    // [databind#838]
    public void testUnWrappedMapWithDefaultType() throws Exception{
        final ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        mapper.registerModule(mod);

        TypeResolverBuilder<?> typer = ObjectMapper.DefaultTypeResolverBuilder.construct(
                ObjectMapper.DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(JsonTypeInfo.Id.NAME, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        //typer = typer.typeProperty(TYPE_FIELD);
        typer = typer.typeIdVisibility(true);
        mapper.setDefaultTyping(typer);

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
