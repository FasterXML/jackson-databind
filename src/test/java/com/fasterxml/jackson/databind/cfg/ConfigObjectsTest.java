package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;

public class ConfigObjectsTest extends BaseMapTest
{
    static class Base { }
    static class Sub extends Base { }

    public void testSubtypeResolver() throws Exception
    {
        ObjectMapper vanilla = new ObjectMapper();
        StdSubtypeResolver repl = new StdSubtypeResolver()
            .registerSubtypes(Sub.class);
        ObjectMapper mapper = ObjectMapper.builder()
                .subtypeResolver(repl)
                .build();
        assertSame(repl, mapper.getSubtypeResolver());
        assertNotSame(vanilla, mapper.getSubtypeResolver());

        assertSame(repl, mapper.deserializationConfig().getSubtypeResolver());
        assertSame(repl, mapper.serializationConfig().getSubtypeResolver());
    }
}
