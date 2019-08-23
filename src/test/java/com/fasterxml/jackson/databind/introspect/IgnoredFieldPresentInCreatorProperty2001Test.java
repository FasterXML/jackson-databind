package com.fasterxml.jackson.databind.introspect;

import java.beans.ConstructorProperties;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;

public class IgnoredFieldPresentInCreatorProperty2001Test extends BaseMapTest
{
   static public class Foo {
        @JsonIgnore
        public String query;

        // 01-May-2018, tatu: Important! Without this there is no problem
        @ConstructorProperties("rawQuery")
        @JsonCreator
        public Foo(@JsonProperty("query") String rawQuery) {
        query = rawQuery;
      }
    }

    public void testIgnoredFieldPresentInPropertyCreator() throws Exception {
        Foo deserialized = newJsonMapper().readValue("{\"query\": \"bar\"}", Foo.class);
        assertEquals("bar", deserialized.query);
    }
}
