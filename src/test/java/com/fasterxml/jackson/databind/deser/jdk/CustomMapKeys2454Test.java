package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class CustomMapKeys2454Test extends BaseMapTest
{
    @JsonDeserialize(keyUsing = Key2454Deserializer.class)
    @JsonSerialize(keyUsing = Key2454Serializer.class)
    static class Key2454 {
        String id;

        public Key2454(String id, boolean bogus) {
            this.id = id;
        }
    }

    static class Key2454Deserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new Key2454(key, false);
        }
    }

    static class Key2454Serializer extends JsonSerializer<Key2454> {
        @Override
        public void serialize(Key2454 value, JsonGenerator gen,
                SerializerProvider serializers) throws IOException {
            gen.writeFieldName("id="+value.id);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testCustomSerializer() throws Exception
    {
        assertEquals(a2q("{'id=a':'b'}"),
                MAPPER.writeValueAsString(Collections.singletonMap(new Key2454("a", true), "b")));
    }

    public void testCustomDeserializer() throws Exception
    {
        Map<Key2454, String> result = MAPPER.readValue(a2q("{'a':'b'}"),
                new TypeReference<Map<Key2454, String>>() { });
        assertEquals(1, result.size());
        Key2454 key = result.keySet().iterator().next();
        assertEquals("a", key.id);
    }
}
