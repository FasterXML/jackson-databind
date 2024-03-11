package tools.jackson.databind.module;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

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
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addKeyDeserializer(Foo.class, new FooKeyDeserializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        Map<Foo,Integer> map = mapper.readValue("{\"a\":3}",
                new TypeReference<Map<Foo,Integer>>() {} );
        assertNotNull(map);
        assertEquals(1, map.size());
        Foo foo = map.keySet().iterator().next();
        assertEquals("a", foo.value);
    }
}
