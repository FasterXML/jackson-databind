package com.fasterxml.jackson.databind.misc;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.databind.util.JSONWrappedObject;

import static org.junit.jupiter.api.Assertions.*;

public class TestJSONP
    extends DatabindTestUtil
{
    static class Base {
        public String a;
    }
    static class Impl extends Base {
        public String b;

        public Impl(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSimpleScalars() throws Exception
    {
        assertEquals("callback(\"abc\")",
                MAPPER.writeValueAsString(new JSONPObject("callback", "abc")));
        assertEquals("calc(123)",
                MAPPER.writeValueAsString(new JSONPObject("calc", Integer.valueOf(123))));
        assertEquals("dummy(null)",
                MAPPER.writeValueAsString(new JSONPObject("dummy", null)));
    }

    @Test
    public void testSimpleBean() throws Exception
    {
        assertEquals("xxx({\"a\":\"123\",\"b\":\"456\"})",
                MAPPER.writeValueAsString(new JSONPObject("xxx",
                        new Impl("123", "456"))));
    }

    /**
     * Test to ensure that it is possible to force a static type for wrapped
     * value.
     */
    @Test
    public void testWithType() throws Exception
    {
        Object ob = new Impl("abc", "def");
        JavaType type = MAPPER.constructType(Base.class);
        assertEquals("do({\"a\":\"abc\"})",
                MAPPER.writeValueAsString(new JSONPObject("do", ob, type)));
    }

    @Test
    public void testGeneralWrapping() throws Exception
    {
        JSONWrappedObject input = new JSONWrappedObject("/*Foo*/", "\n// the end",
                Arrays.asList());
        assertEquals("/*Foo*/[]\n// the end", MAPPER.writeValueAsString(input));
    }
}
