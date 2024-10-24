package com.fasterxml.jackson.databind.seq;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestInnerClassReaderFor extends DatabindTestUtil {

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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void readerFor() throws IOException {

        X x = new X("dummy");
        MAPPER.readerForUpdating(x).readValue("{\"value\": \"updatedX\"}");
        assertEquals("updatedX", x.getValue());

        Y y = new Y("dummy");
        MAPPER.readerForUpdating(y).readValue("{\"value\": \"updatedY\"}");
        assertEquals("updatedY", y.getValue());

    }

}
