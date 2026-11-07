package com.fasterxml.jackson.databind.ser.jdk;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeySerializerFallback6326Test extends DatabindTestUtil
{
    static class Key {
        private final String _value;

        Key(String v) { _value = v; }

        @Override
        public String toString() { return _value; }
    }

    static class JsonValueKey extends Key {
        JsonValueKey() { super("toString"); }

        @JsonValue
        public int value() { return 13; }
    }

    static class JsonKeyAnnotatedKey extends JsonValueKey {
        @JsonKey
        public String key() { return "custom-key"; }
    }

    enum EnumKey {
        @JsonProperty("renamed")
        VALUE
    }

    static class MapBean {
        public Map<Key, String> map = Collections.singletonMap(new Key("k"), "v");
    }

    @SuppressWarnings("serial")
    static class KeySerializerFactory extends BeanSerializerFactory {
        private final JsonSerializer<Object> _keySerializer;

        KeySerializerFactory(JsonSerializer<Object> keySerializer) {
            super(null);
            _keySerializer = keySerializer;
        }

        @Override
        public JsonSerializer<Object> createKeySerializer(SerializerProvider prov,
                JavaType type, JsonSerializer<Object> defaultImpl) {
            return _keySerializer;
        }
    }

    static class ContextualKeySerializer extends JsonSerializer<Object>
        implements ResolvableSerializer, ContextualSerializer
    {
        private final String _prefix;
        private boolean _resolved;

        ContextualKeySerializer(String prefix) { _prefix = prefix; }

        @Override
        public void resolve(SerializerProvider provider) {
            _resolved = true;
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider provider,
                BeanProperty property) {
            assertTrue(_resolved);
            return new ContextualKeySerializer(_prefix
                    + ((property == null) ? "root" : property.getName()));
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
            throws IOException
        {
            gen.writeFieldName(_prefix + ":" + value);
        }
    }

    @Test
    void testNullKeySerializer() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new KeySerializerFactory(null));
        Map<Key, String> input = Collections.singletonMap(new Key("k"), "v");

        assertEquals(a2q("{'k':'v'}"), mapper.writeValueAsString(input));
        assertEquals(a2q("{'k':'v'}"), mapper.writerFor(new TypeReference<Map<Key, String>>() { })
                .writeValueAsString(input));
    }

    @Test
    void testMapEntryKeySerializer() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new KeySerializerFactory(null));
        Map.Entry<Key, String> input = new AbstractMap.SimpleEntry<>(new Key("k"), "v");

        assertEquals(a2q("{'k':'v'}"), mapper.writerFor(
                new TypeReference<Map.Entry<Key, String>>() { }).writeValueAsString(input));
    }

    @Test
    void testStandardKeySerializers() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new KeySerializerFactory(null));
        Map<Object, String> input = new LinkedHashMap<>();
        input.put("string", "s");
        input.put(42, "i");
        input.put(String.class, "c");
        input.put(new byte[] { 1, 2, 3 }, "b");
        input.put(EnumKey.VALUE, "e");

        String expected = a2q("{'string':'s','42':'i','java.lang.String':'c',"
                + "'AQID':'b','renamed':'e'}");
        assertEquals(expected, newJsonMapper().writeValueAsString(input));
        assertEquals(expected, mapper.writeValueAsString(input));
        assertEquals(a2q("{'0':'e'}"), mapper.writer()
                .with(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX)
                .writeValueAsString(Collections.singletonMap(EnumKey.VALUE, "e")));
    }

    @Test
    void testJsonValueKey() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new KeySerializerFactory(null));
        Map<JsonValueKey, String> input = Collections.singletonMap(new JsonValueKey(), "v");

        assertEquals(a2q("{'13':'v'}"), mapper.writerFor(
                new TypeReference<Map<JsonValueKey, String>>() { }).writeValueAsString(input));
    }

    @Test
    void testJsonKey() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new KeySerializerFactory(null));
        Map<JsonKeyAnnotatedKey, String> input = Collections.singletonMap(
                new JsonKeyAnnotatedKey(), "v");

        assertEquals(a2q("{'custom-key':'v'}"), mapper.writerFor(
                new TypeReference<Map<JsonKeyAnnotatedKey, String>>() { })
                .writeValueAsString(input));
    }

    @Test
    void testConfiguredDefaultKeySerializer() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new KeySerializerFactory(null));
        mapper.getSerializerProvider().setDefaultKeySerializer(
                new ContextualKeySerializer("default-"));

        assertEquals(a2q("{'default-root:k':'v'}"), mapper.writeValueAsString(new MapBean().map));
        assertEquals(a2q("{'map':{'default-map:k':'v'}}"),
                mapper.writeValueAsString(new MapBean()));
        assertEquals(a2q("{'default-root:string':'v'}"),
                mapper.writeValueAsString(Collections.singletonMap("string", "v")));
    }

    @Test
    void testFactoryKeySerializer() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new KeySerializerFactory(
                new ContextualKeySerializer("factory-")));
        mapper.getSerializerProvider().setDefaultKeySerializer(
                new ContextualKeySerializer("default-"));

        assertEquals(a2q("{'factory-root:k':'v'}"), mapper.writeValueAsString(new MapBean().map));
        assertEquals(a2q("{'map':{'factory-map:k':'v'}}"),
                mapper.writeValueAsString(new MapBean()));
    }
}
