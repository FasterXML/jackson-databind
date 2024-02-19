package tools.jackson.databind.introspect;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests for [databind#2001]
public class IgnoredFieldPresentInCreatorProperty2001Test extends DatabindTestUtil
{
   static public class Foo {
        @JsonIgnore
        public String query;

        @JsonCreator
        @ConstructorProperties("rawQuery")
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
