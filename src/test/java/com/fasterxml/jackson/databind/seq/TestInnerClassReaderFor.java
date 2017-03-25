package com.fasterxml.jackson.databind.seq;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;

import java.io.IOException;

public class TestInnerClassReaderFor extends BaseMapTest {

    class X {
        private String value;

        X(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    class Y extends X {
        Y(@JsonProperty("value") String value) {
            super(value);
        }
    }

    public void testReaderFor() throws IOException {

        X x = new X("dummy");
        objectMapper().readerForUpdating(x).readValue("{\"value\": \"updatedX\"}");
        assertEquals(x.getValue(), "updatedX");

        Y y = new Y("dummy");
        objectMapper().readerForUpdating(y).readValue("{\"value\": \"updatedY\"}");
        assertEquals(y.getValue(), "updatedY");

    }

}
