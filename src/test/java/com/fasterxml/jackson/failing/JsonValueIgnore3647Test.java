package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// for [databind#3647]
public class JsonValueIgnore3647Test extends BaseMapTest
{
    // for [databind#3647]
    static class Foo3647 {
        public String p1 = "hello";
        public String p2 = "world";
    }

    static class Bar3647 {
        @JsonValue
        @JsonIgnoreProperties("p1")
        public Foo3647 getFoo() {
            return new Foo3647();
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3647]
    public void testIgnorePropsAnnotatedJsonValue() throws Exception
    {
        assertEquals("{\"p2\":\"world\"}", MAPPER.writeValueAsString(new Bar3647()));
    }
}
