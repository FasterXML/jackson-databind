package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TestExceptionHandlingWithJsonCreatorDeserialization extends BaseMapTest
{
    static class Foo {
        private Bar bar;

        @JsonCreator
        public Foo(@JsonProperty("bar") Bar bar) {
            this.bar = bar;
        }

        public Bar getBar() {
            return bar;
        }
    }

    static class Bar {
        private Baz baz;

        @JsonCreator
        public Bar(@JsonProperty("baz") Baz baz) {
            this.baz = baz;
        }

        public Baz getBaz() {
            return baz;
        }
    }

    static class Baz {
        private String qux;

        @JsonCreator
        public Baz(@JsonProperty("qux") String qux) {
            this.qux = qux;
        }

        public String getQux() {
            return qux;
        }
    }

    public void testShouldThrowJsonMappingExceptionWithPathReference() throws IOException {
        // given
        ObjectMapper mapper = new ObjectMapper();
        String input = "{\"bar\":{\"baz\":{qux:\"quxValue\"))}";

        // when
        try {
            mapper.readValue(input, Foo.class);
            fail("Upsss! Exception has not been thrown.");
        } catch (JsonMappingException ex) {
            // then
            assertEquals("com.fasterxml.jackson.databind.deser.Foo[\"bar\"]->com.fasterxml.jackson.databind.deser.Bar[\"baz\"]",
                    ex.getPathReference());
        }
    }
}
