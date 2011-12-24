package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.JSONPObject;

public class TestJSONP
    extends BaseMapTest
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
    
    public void testSimpleScalars() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("callback(\"abc\")",
                serializeAsString(m, new JSONPObject("callback", "abc")));
        assertEquals("calc(123)",
                serializeAsString(m, new JSONPObject("calc", Integer.valueOf(123))));
        assertEquals("dummy(null)",
                serializeAsString(m, new JSONPObject("dummy", null)));
    }

    public void testSimpleBean() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("xxx({\"a\":\"123\",\"b\":\"456\"})",
                serializeAsString(m, new JSONPObject("xxx",
                        new Impl("123", "456"))));
    }
    
    /**
     * Test to ensure that it is possible to force a static type for wrapped
     * value.
     */
    public void testWithType() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Object ob = new Impl("abc", "def");
        JavaType type = TypeFactory.defaultInstance().uncheckedSimpleType(Base.class);
        assertEquals("do({\"a\":\"abc\"})",
                serializeAsString(m, new JSONPObject("do", ob, type)));
    }
}
