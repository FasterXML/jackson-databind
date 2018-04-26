package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("serial")
public class TestKeySerializers extends BaseMapTest
{
    public static class KarlSerializer extends StdSerializer<String>
    {
        public KarlSerializer() { super(String.class); }
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeFieldName("Karl");
        }
    }

    public static class NullKeySerializer extends StdSerializer<Object>
    {
        private String _null;
        public NullKeySerializer(String s) {
            super(String.class);
            _null = s;
        }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeFieldName(_null);
        }
    }

    public static class NullValueSerializer extends StdSerializer<Object>
    {
        private String _null;
        public NullValueSerializer(String s) { super(String.class);
            _null = s;
            }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(_null);
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

    enum ABC {
        A, B, C
    }

    enum AbcLC {
        A, B, C;

        @JsonValue
        public String toLC() {
            return name().toLowerCase();
        }
    }
    
    static class ABCKeySerializer extends StdSerializer<ABC> {
        public ABCKeySerializer() { super(ABC.class); }
        @Override
        public void serialize(ABC value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeFieldName("xxx"+value);
        }
    }

    @JsonSerialize(keyUsing = ABCKeySerializer.class)
    public static enum ABCMixin { }

    public static enum Outer {
        inner;
    }

    static class ABCMapWrapper {
        public Map<ABC,String> stuff = new HashMap<ABC,String>();
        public ABCMapWrapper() {
            stuff.put(ABC.B, "bar");
        }
    }

    static class BAR<T>{
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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
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
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod)
                .build();
        String json = mapper.writeValueAsString(new ABCMapWrapper());
        assertEquals("{\"stuff\":{\"xxxB\":\"bar\"}}", json);
    }

    public void testCustomNullSerializers() throws IOException
    {
        final ObjectMapper mapper = ObjectMapper.builder()
                .addModule(new SimpleModule()
                        .setDefaultNullKeySerializer(new NullKeySerializer("NULL-KEY"))
                        .setDefaultNullValueSerializer(new NullValueSerializer("NULL"))
                        )
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
        SimpleModule mod = new SimpleModule("test");
        mod.setMixInAnnotation(ABC.class, ABCMixin.class);
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod)
                .build();
        JsonNode tree = mapper.convertValue(outerMap, JsonNode.class);

        JsonNode innerNode = tree.get("inner");
        String key = innerNode.fieldNames().next();
        assertEquals("xxxA", key);
    }

    // [databind#838]
    public void testUnWrappedMapWithDefaultType() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY, JsonTypeInfo.Id.NAME, null)
            .typeIdVisibility(true);
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod)
                .setDefaultTyping(typer)
                .build();

        Map<ABC,String> stuff = new HashMap<ABC,String>();
        stuff.put(ABC.B, "bar");
        String json = mapper.writerFor(new TypeReference<Map<ABC, String>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"@type\":\"HashMap\",\"xxxB\":\"bar\"}", json);
    }

    // [databind#838]
    public void testUnWrappedMapWithKeySerializer() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        final ObjectMapper mapper = ObjectMapper.builder()
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

    // [databind#943]
    public void testDynamicMapKeys() throws Exception
    {
        Map<Object,Integer> stuff = new LinkedHashMap<Object,Integer>();
        stuff.put(AbcLC.B, Integer.valueOf(3));
        stuff.put(new UCString("foo"), Integer.valueOf(4));
        String json = MAPPER.writeValueAsString(stuff);
        assertEquals(aposToQuotes("{'b':3,'FOO':4}"), json);
    }
}
