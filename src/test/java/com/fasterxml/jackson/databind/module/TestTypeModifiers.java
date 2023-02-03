package com.fasterxml.jackson.databind.module;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.*;

@SuppressWarnings("serial")
public class TestTypeModifiers extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class ModifierModule extends SimpleModule
    {
        public ModifierModule() {
            super("test", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context)
        {
            context.addSerializers(new Serializers.Base() {
                @Override
                public JsonSerializer<?> findMapLikeSerializer(SerializationConfig config,
                        MapLikeType type, BeanDescription beanDesc,
                        JsonSerializer<Object> keySerializer,
                        TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
                {
                    if (MapMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyMapSerializer(keySerializer, elementValueSerializer);
                    }
                    return null;
                }

                @Override
                public JsonSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
                        CollectionLikeType type, BeanDescription beanDesc,
                        TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
                {
                    if (CollectionMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyCollectionSerializer();
                    }
                    return null;
                }
            });
            context.addDeserializers(new SimpleDeserializers() {
                @Override
                public JsonDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type, DeserializationConfig config,
                        BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
                {
                    if (CollectionMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyCollectionDeserializer();
                    }
                    return null;
                }
                @Override
                public JsonDeserializer<?> findMapLikeDeserializer(MapLikeType type, DeserializationConfig config,
                        BeanDescription beanDesc, KeyDeserializer keyDeserializer,
                        TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
                {
                    if (MapMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyMapDeserializer();
                    }
                    return null;
                }
            });
        }
    }

    static class XxxSerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString("xxx:"+value);
        }
    }

    interface MapMarker<K,V> {
        public K getKey();
        public V getValue();
    }
    interface CollectionMarker<V> {
        public V getValue();
    }

    @JsonSerialize(contentUsing=XxxSerializer.class)
    static class MyMapLikeType implements MapMarker<String,Integer> {
        public String key;
        public int value;

        public MyMapLikeType() { }
        public MyMapLikeType(String k, int v) {
            key = k;
            value = v;
        }

        @Override
        public String getKey() { return key; }
        @Override
        public Integer getValue() { return value; }
    }

    static class MyCollectionLikeType implements CollectionMarker<Integer>
    {
        public int value;

        public MyCollectionLikeType() { }
        public MyCollectionLikeType(int v) {
            value = v;
        }

        @Override
        public Integer getValue() { return value; }
    }

    static class MyMapSerializer extends JsonSerializer<MapMarker<?,?>>
    {
        protected final JsonSerializer<Object> _keySerializer;
        protected final JsonSerializer<Object> _valueSerializer;

        public MyMapSerializer(JsonSerializer<Object> keySer, JsonSerializer<Object> valueSer) {
            _keySerializer = keySer;
            _valueSerializer = valueSer;
        }

        @Override
        public void serialize(MapMarker<?,?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            if (_keySerializer == null) {
                jgen.writeFieldName((String) value.getKey());
            } else {
                _keySerializer.serialize(value.getKey(), jgen, provider);
            }
            if (_valueSerializer == null) {
                jgen.writeNumber(((Number) value.getValue()).intValue());
            } else {
                _valueSerializer.serialize(value.getValue(), jgen, provider);
            }
            jgen.writeEndObject();
        }
    }
    static class MyMapDeserializer extends JsonDeserializer<MapMarker<?,?>>
    {
        @Override
        public MapMarker<?,?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (jp.currentToken() != JsonToken.START_OBJECT) throw new IOException("Wrong token: "+jp.currentToken());
            if (jp.nextToken() != JsonToken.FIELD_NAME) throw new IOException("Wrong token: "+jp.currentToken());
            String key = jp.currentName();
            if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) throw new IOException("Wrong token: "+jp.currentToken());
            int value = jp.getIntValue();
            if (jp.nextToken() != JsonToken.END_OBJECT) throw new IOException("Wrong token: "+jp.currentToken());
            return new MyMapLikeType(key, value);
        }
    }

    static class MyCollectionSerializer extends JsonSerializer<MyCollectionLikeType>
    {
        @Override
        public void serialize(MyCollectionLikeType value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartArray();
            jgen.writeNumber(value.value);
            jgen.writeEndArray();
        }
    }
    static class MyCollectionDeserializer extends JsonDeserializer<MyCollectionLikeType>
    {
        @Override
        public MyCollectionLikeType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (jp.currentToken() != JsonToken.START_ARRAY) throw new IOException("Wrong token: "+jp.currentToken());
            if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) throw new IOException("Wrong token: "+jp.currentToken());
            int value = jp.getIntValue();
            if (jp.nextToken() != JsonToken.END_ARRAY) throw new IOException("Wrong token: "+jp.currentToken());
            return new MyCollectionLikeType(value);
        }
    }

    static class MyTypeModifier extends TypeModifier
    {
        @Override
        public JavaType modifyType(JavaType type, Type jdkType, TypeBindings bindings, TypeFactory typeFactory)
        {
            if (!type.isContainerType()) { // not 100% required, minor optimization
                Class<?> raw = type.getRawClass();
                if (raw == MapMarker.class) {
                    return MapLikeType.upgradeFrom(type, type.containedType(0), type.containedType(1));
                }
                if (raw == CollectionMarker.class) {
                    return CollectionLikeType.upgradeFrom(type, type.containedType(0));
                }
            }
            return type;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER_WITH_MODIFIER = JsonMapper.builder()
            .typeFactory(TypeFactory.defaultInstance().withModifier(new MyTypeModifier()))
            .build();

    /**
     * Basic test for ensuring that we can get "xxx-like" types recognized.
     */
    public void testMapLikeTypeConstruction() throws Exception
    {
        JavaType type = MAPPER_WITH_MODIFIER.constructType(MyMapLikeType.class);
        assertTrue(type.isMapLikeType());
        // also, must have resolved type info
        JavaType param = ((MapLikeType) type).getKeyType();
        assertNotNull(param);
        assertSame(String.class, param.getRawClass());
        param = ((MapLikeType) type).getContentType();
        assertNotNull(param);
        assertSame(Integer.class, param.getRawClass());
    }

    public void testMapLikeTypeViaParametric() throws Exception
    {
        // [databind#2796]: should refine with another call too
        JavaType type = MAPPER_WITH_MODIFIER.getTypeFactory().constructParametricType(MapMarker.class,
                new Class<?>[] { String.class, Double.class });
        assertTrue(type.isMapLikeType());
        JavaType param = ((MapLikeType) type).getKeyType();
        assertNotNull(param);
        assertSame(String.class, param.getRawClass());

        param = ((MapLikeType) type).getContentType();
        assertNotNull(param);
        assertSame(Double.class, param.getRawClass());
    }

    // [databind#2395] Can trigger problem this way too
    // NOTE: oddly enough, seems to ONLY fail
    public void testTypeResolutionForRecursive() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                context.addTypeModifier(new MyTypeModifier());
            }
        });
        assertNotNull(mapper.readTree("{}"));
    }

    public void testCollectionLikeTypeConstruction() throws Exception
    {
        JavaType type = MAPPER_WITH_MODIFIER.constructType(MyCollectionLikeType.class);
        assertTrue(type.isCollectionLikeType());
        JavaType param = ((CollectionLikeType) type).getContentType();
        assertNotNull(param);
        assertSame(Integer.class, param.getRawClass());
    }

    public void testCollectionLikeSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        mapper.registerModule(new ModifierModule());
        assertEquals("[19]", mapper.writeValueAsString(new MyCollectionLikeType(19)));
    }

    public void testMapLikeSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        mapper.registerModule(new ModifierModule());
        // Due to custom serializer, should get:
        assertEquals("{\"x\":\"xxx:3\"}", mapper.writeValueAsString(new MyMapLikeType("x", 3)));
    }


    public void testCollectionLikeDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        mapper.registerModule(new ModifierModule());
        // !!! TBI
        MyMapLikeType result = mapper.readValue("{\"a\":13}", MyMapLikeType.class);
        assertEquals("a", result.getKey());
        assertEquals(Integer.valueOf(13), result.getValue());
    }

    public void testMapLikeDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        mapper.registerModule(new ModifierModule());
        // !!! TBI
        MyCollectionLikeType result = mapper.readValue("[-37]", MyCollectionLikeType.class);
        assertEquals(Integer.valueOf(-37), result.getValue());
    }
}
