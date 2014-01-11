package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;

public class TestNodeTypingIssue88 extends BaseMapTest
{
    public static class Foo {
        public String bar;

        public Foo() { }
        public Foo(String b) { bar = b; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testValueAsStringWithDefaultTyping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        Foo foo = new Foo("baz");
        String json = mapper.writeValueAsString(foo);

        JsonNode jsonNode = mapper.readTree(json);
        assertEquals(jsonNode.get("bar").textValue(), foo.bar);
    }

    public void testValueToTreeWithDefaultTyping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        Foo foo = new Foo("baz");
        JsonNode jsonNode = mapper.valueToTree(foo);
        assertEquals(jsonNode.get("bar").textValue(), foo.bar);
    }
}
