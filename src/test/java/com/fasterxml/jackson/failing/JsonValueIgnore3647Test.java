package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// for [databind#3647]
public class JsonValueIgnore3647Test extends BaseMapTest
{
    // for [databind#3647]
    static class Foo {
        String p1 = "hello";
        String p2 = "world";

        public String getP1() {
            return p1;
        }

        public void setP1(String p1) {
            this.p1 = p1;
        }

        public String getP2() {
            return p2;
        }

        public void setP2(String p2) {
            this.p2 = p2;
        }
    }

    // for [databind#3647]
    static class Bar {
        @JsonValue
        @JsonIgnoreProperties("p1")
        Foo foo = new Foo();
        public Foo getFoo() {
            return foo;
        }
        public void setFoo(Foo foo) {
            this.foo = foo;
        }
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
    */

    // [databind#3647]
    public void testIgnorePropsAnnotatedJsonValue() throws Exception
    {
        final String result = "{\"p2\":\"world\"}";
        final String jsonStringWithIgnoredProps = MAPPER.writeValueAsString(new Bar());
        assertEquals(result, jsonStringWithIgnoredProps);
    }
}
