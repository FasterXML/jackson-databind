package com.fasterxml.jackson.databind.introspect;

import java.beans.ConstructorProperties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;

// Tests for [databind#2001]
public class IgnoredFieldPresentInCreatorProperty2001Test extends BaseMapTest {
    static public class Foo {

        @JsonIgnore
        public String query;

        @JsonCreator
        @ConstructorProperties("rawQuery")
        public Foo(@JsonProperty("query") String rawQuery) {
        query = rawQuery;
      }
    }

    public void testIgnoredFieldPresentInPropertyCreator() throws Exception {
        Foo deserialized = newJsonMapper().readValue("{\"query\": \"bar\"}", Foo.class);
        assertEquals("bar", deserialized.query);
    }
}
