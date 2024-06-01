package com.fasterxml.jackson.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class UnwrappedWithUnknown650Test extends DatabindTestUtil {
    static class A {
        @JsonUnwrapped
        public B b;
    }

    static class B {
        public String field;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void failOnUnknownPropertyUnwrapped() throws Exception {
        assertTrue(MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        final String JSON = "{'field': 'value', 'bad':'bad value'}";
        try {
            MAPPER.readValue(a2q(JSON), A.class);
            fail("Exception was not thrown on unkown property");
        } catch (DatabindException e) {
            verifyException(e, "Unrecognized field");
        }
    }
}
