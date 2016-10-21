package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;

public class ConfigObjectsTest extends BaseMapTest
{
    static class Base { }
    static class Sub extends Base { }

    public void testSubtypeResolver() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SubtypeResolver res = mapper.getSubtypeResolver();
        assertTrue(res instanceof StdSubtypeResolver);

        StdSubtypeResolver repl = new StdSubtypeResolver();
        repl.registerSubtypes(Sub.class);
        mapper.setSubtypeResolver(repl);
        assertSame(repl, mapper.getSubtypeResolver());
    }

    public void testMics() throws Exception
    {
        assertFalse(MapperFeature.AUTO_DETECT_FIELDS.enabledIn(0));
        assertTrue(MapperFeature.AUTO_DETECT_FIELDS.enabledIn(-1));
    }
}
