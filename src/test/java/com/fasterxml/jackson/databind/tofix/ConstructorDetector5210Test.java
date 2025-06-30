package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConstructorDetector5210Test {

    // succeeds with 2.12.7
    // also succeeds with 2.17.0, 2.17.2, 2.18.0, 2.18.1, 2.18.2
    // fails with 2.18.3+
    @JacksonTestFailureExpected
    @Test
    public void testSerialization() throws JsonProcessingException {
        String json = "{\"someFiled\":\"imSomeStringVal\"}";

        ObjectMapper objectMapper = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build();

        // no exception starting from 2.18.3
        assertThrows(JsonMappingException.class, () -> {
            objectMapper.readValue(json, WrapperClass.class);
        });
    }

    public static class WrapperClass {
        @JsonProperty("someFiled")
        private final SingleStringClass someFiled;

        private WrapperClass() {
            someFiled = null;
        }

        public WrapperClass(SingleStringClass someFiled) {
            this.someFiled = someFiled;
        }
    }

    public static class SingleStringClass {
        @JsonProperty("someStringVal")
        private final String someStringVal;

        private SingleStringClass() {
            someStringVal = null;
        }

        public SingleStringClass(String someStringVal) {
            this.someStringVal = someStringVal;
        }
    }
}
