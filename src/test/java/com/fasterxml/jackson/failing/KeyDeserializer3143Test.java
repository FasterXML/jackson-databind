package com.fasterxml.jackson.failing;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KeyDeserializer3143Test extends BaseMapTest
{
    // [databind#3143]
    static class Key3143 {
        protected String value;

        private Key3143(String v, boolean bogus) {
            value = v;
        }

        @JsonCreator
        public static Key3143 create(String v) {
            return new Key3143(v, true);
        }

        public static Key3143 valueOf(String id) {
            return new Key3143(id.toUpperCase(), false);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3143]
    public void testKeyWithCreatorAndMultipleFactoryMethods() throws Exception
    {
        Map<Key3143,Integer> map = MAPPER.readValue("{\"foo\":3}",
                new TypeReference<Map<Key3143,Integer>>() {} );
        assertEquals(1, map.size());
        assertEquals("foo", map.keySet().iterator().next().value);
    }
}
