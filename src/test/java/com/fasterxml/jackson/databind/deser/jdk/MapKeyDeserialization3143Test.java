package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

public class MapKeyDeserialization3143Test extends BaseMapTest
{
    // [databind#3143]
    static class Key3143Factories {
        protected String value;

        private Key3143Factories(String v, boolean bogus) {
            value = v;
        }

        // Specifically wrong one :)
        public static Key3143Factories cantUse() {
            throw new RuntimeException("Invalid factory");
        }

        @JsonCreator
        public static Key3143Factories create(String v) {
            return new Key3143Factories(v.toLowerCase(), true);
        }

        // Wrong one...
        public static Key3143Factories valueOf(String id) {
            return new Key3143Factories(id.toUpperCase(), false);
        }
    }

    // [databind#3143]: case of conflict
    static class Key3143FactoriesFail {
        @JsonCreator
        public static Key3143FactoriesFail create(String v) {
            throw new Error("Can't use");
        }

        @JsonCreator
        public static Key3143FactoriesFail valueOf(String id) {
            throw new Error("Can't use");
        }
    }

    // [databind#3143]
    static class Key3143Ctor {
        protected String value;

        public static Key3143Ctor valueOf(String id) {
            return new Key3143Ctor(id.toUpperCase());
        }

        @JsonCreator
        private Key3143Ctor(String v) {
            value = v;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3143]
    public void testKeyWithCtorAndCreator3143() throws Exception
    {
        // Use Constructor if annotated:
        Map<Key3143Ctor,Integer> map = MAPPER.readValue("{\"bar\":3}",
                new TypeReference<Map<Key3143Ctor,Integer>>() {} );
        assertEquals(1, map.size());
        assertEquals("bar", map.keySet().iterator().next().value);
    }

    // [databind#3143]
    public void testKeyWith2Creators3143() throws Exception
    {
        // Select explicitly annotated factory method
        Map<Key3143Factories,Integer> map = MAPPER.readValue("{\"Foo\":3}",
                new TypeReference<Map<Key3143Factories,Integer>>() {} );
        assertEquals(1, map.size());
        assertEquals("foo", map.keySet().iterator().next().value);
    }

    // [databind#3143]
    public void testKeyWithCreatorConflicts3143() throws Exception
    {
        try {
            MAPPER.readValue("{\"Foo\":3}",
                new TypeReference<Map<Key3143FactoriesFail,Integer>>() {} );
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Multiple");
            verifyException(e, "Creator factory methods");
        }
    }
}
