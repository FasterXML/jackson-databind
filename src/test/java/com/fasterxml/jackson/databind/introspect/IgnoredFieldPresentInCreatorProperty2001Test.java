package com.fasterxml.jackson.databind.introspect;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IgnoredFieldPresentInCreatorProperty2001Test extends DatabindTestUtil
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

    @Test
    public void testIgnoredFieldPresentInPropertyCreator() throws Exception {
        Foo deserialized = newJsonMapper().readValue("{\"query\": \"bar\"}", Foo.class);
        assertEquals("bar", deserialized.query);
    }
}
