package com.fasterxml.jackson.databind.module;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestKeyDeserializers extends DatabindTestUtil
{
    static class FooKeyDeserializer extends KeyDeserializer
    {
        @Override
        public Foo deserializeKey(String key, DeserializationContext ctxt)
        {
            return new Foo(key);
        }
    }

    static class Foo {
        public String value;

        public Foo(String v) { value = v; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    @Test
    public void testKeyDeserializers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addKeyDeserializer(Foo.class, new FooKeyDeserializer());
        mapper.registerModule(mod);
        Map<Foo,Integer> map = mapper.readValue("{\"a\":3}",
                new TypeReference<Map<Foo,Integer>>() {} );
        assertNotNull(map);
        assertEquals(1, map.size());
        Foo foo = map.keySet().iterator().next();
        assertEquals("a", foo.value);
    }
}
