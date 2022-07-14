package com.fasterxml.jackson.databind.module;

import java.lang.reflect.Type;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.*;

@SuppressWarnings("serial")
public class TestTypeModifiers extends BaseMapTest
{
    private static class ModifierModule extends SimpleModule
    {
        public ModifierModule() {
            super("test", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context)
        {
            context.addSerializers(new Serializers.Base() {
                @Override
                public ValueSerializer<?> findMapLikeSerializer(SerializationConfig config,
                        MapLikeType type, BeanDescription beanDesc, JsonFormat.Value format,
                        ValueSerializer<Object> keySerializer,
                        TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
                {
                    if (MapMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyMapSerializer(keySerializer, elementValueSerializer);
                    }
                    return null;
                }

                @Override
                public ValueSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
                        CollectionLikeType type, BeanDescription beanDesc, JsonFormat.Value format,
                        TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
                {
                    if (CollectionMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyCollectionSerializer();
                    }
                    return null;
                }
            });
            context.addDeserializers(new SimpleDeserializers() {
                @Override
                public ValueDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type, DeserializationConfig config,
                        BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
                {
                    if (CollectionMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyCollectionDeserializer();
                    }
                    return null;
                }
                @Override
                public ValueDeserializer<?> findMapLikeDeserializer(MapLikeType type, DeserializationConfig config,
                        BeanDescription beanDesc, KeyDeserializer keyDeserializer,
                        TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
                {
                    if (MapMarker.class.isAssignableFrom(type.getRawClass())) {
                        return new MyMapDeserializer();
                    }
                    return null;
                }
            });
        }
    }

    static class XxxSerializer extends StdSerializer<Object>
    {
        public XxxSerializer() { super(Object.class); }
        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) {
            g.writeString("xxx:"+value);
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

    static class MyMapSerializer extends StdSerializer<MapMarker<?,?>>
    {
        protected final ValueSerializer<Object> _keySerializer;
        protected final ValueSerializer<Object> _valueSerializer;

        public MyMapSerializer(ValueSerializer<Object> keySer, ValueSerializer<Object> valueSer) {
            super(MapMarker.class);
            _keySerializer = keySer;
            _valueSerializer = valueSer;
        }
        
        @Override
        public void serialize(MapMarker<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        {
            jgen.writeStartObject();
            if (_keySerializer == null) {
                jgen.writeName((String) value.getKey());
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
    static class MyMapDeserializer extends ValueDeserializer<MapMarker<?,?>>
    {
        @Override
        public MapMarker<?,?> deserialize(JsonParser p, DeserializationContext ctxt)
        {
            if (p.currentToken() != JsonToken.START_OBJECT) throw new StreamReadException(p, "Wrong token: "+p.currentToken());
            if (p.nextToken() != JsonToken.PROPERTY_NAME) throw new StreamReadException(p, "Wrong token: "+p.currentToken());
            String key = p.currentName();
            if (p.nextToken() != JsonToken.VALUE_NUMBER_INT) throw new StreamReadException(p, "Wrong token: "+p.currentToken());
            int value = p.getIntValue();
            if (p.nextToken() != JsonToken.END_OBJECT) throw new StreamReadException(p, "Wrong token: "+p.currentToken());
            return new MyMapLikeType(key, value);
        }        
    }

    static class MyCollectionSerializer extends StdSerializer<MyCollectionLikeType>
    {
        public MyCollectionSerializer() { super(MyCollectionLikeType.class); }
        @Override
        public void serialize(MyCollectionLikeType value, JsonGenerator g, SerializerProvider provider) {
            g.writeStartArray();
            g.writeNumber(value.value);
            g.writeEndArray();
        }
    }
    static class MyCollectionDeserializer extends ValueDeserializer<MyCollectionLikeType>
    {
        @Override
        public MyCollectionLikeType deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.currentToken() != JsonToken.START_ARRAY) throw new StreamReadException(p, "Wrong token: "+p.currentToken());
            if (p.nextToken() != JsonToken.VALUE_NUMBER_INT) throw new StreamReadException(p, "Wrong token: "+p.currentToken());
            int value = p.getIntValue();
            if (p.nextToken() != JsonToken.END_ARRAY) throw new StreamReadException(p, "Wrong token: "+p.currentToken());
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

    private final ObjectMapper MY_TYPE_MAPPER = jsonMapperBuilder()
            .typeFactory(TypeFactory.defaultInstance().withModifier(new MyTypeModifier()))
            .build();

    private final ObjectMapper MAPPER_WITH_MODIFIER = jsonMapperBuilder()
            .typeFactory(TypeFactory.defaultInstance().withModifier(new MyTypeModifier()))
            .addModule(new ModifierModule())
            .build();

    /**
     * Basic test for ensuring that we can get "xxx-like" types recognized.
     */
    public void testMapLikeTypeConstruction() throws Exception
    {
        JavaType type = MY_TYPE_MAPPER.constructType(MyMapLikeType.class);
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
        final ObjectMapper mapper = jsonMapperBuilder()
                .typeFactory(TypeFactory.defaultInstance().withModifier(new MyTypeModifier()))
                .build();
        assertNotNull(mapper.readTree("{}"));
    }

    public void testCollectionLikeTypeConstruction() throws Exception
    {
        JavaType type = MY_TYPE_MAPPER.constructType(MyCollectionLikeType.class);
        assertTrue(type.isCollectionLikeType());
        JavaType param = ((CollectionLikeType) type).getContentType();
        assertNotNull(param);
        assertSame(Integer.class, param.getRawClass());
    }

    public void testCollectionLikeSerialization() throws Exception
    {
        assertEquals("[19]", MAPPER_WITH_MODIFIER.writeValueAsString(new MyCollectionLikeType(19)));
    }

    public void testMapLikeSerialization() throws Exception
    {
        // Due to custom serializer, should get:
        assertEquals("{\"x\":\"xxx:3\"}", MAPPER_WITH_MODIFIER.writeValueAsString(new MyMapLikeType("x", 3)));
    }

    public void testCollectionLikeDeserialization() throws Exception
    {
        MyMapLikeType result = MAPPER_WITH_MODIFIER.readValue("{\"a\":13}", MyMapLikeType.class);
        assertEquals("a", result.getKey());
        assertEquals(Integer.valueOf(13), result.getValue());
    }

    public void testMapLikeDeserialization() throws Exception
    {
        MyCollectionLikeType result = MAPPER_WITH_MODIFIER.readValue("[-37]", MyCollectionLikeType.class);
        assertEquals(Integer.valueOf(-37), result.getValue());
    }
}
