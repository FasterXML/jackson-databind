package com.fasterxml.jackson.databind.deser.inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JacksonInject1381WithOptionalDeserializationFeatureDisabledTest extends DatabindTestUtil
{
    static class InputDefault
    {
        @JacksonInject(value = "key", optional = OptBoolean.TRUE)
        @JsonProperty("field")
        private final String _field;

        @JsonCreator
        public InputDefault(@JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }
    }

    static class InputDefaultConstructor
    {
        private final String _field;

        @JsonCreator
        public InputDefaultConstructor(@JacksonInject(value = "key", optional = OptBoolean.TRUE)
                            @JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }
    }

    static class InputTrue
    {
        @JacksonInject(value = "key", useInput = OptBoolean.TRUE, optional = OptBoolean.TRUE)
        @JsonProperty("field")
        private final String _field;

        @JsonCreator
        public InputTrue(@JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }
    }

    static class InputTrueConstructor
    {
        private final String _field;

        @JsonCreator
        public InputTrueConstructor(@JacksonInject(value = "key", useInput = OptBoolean.TRUE, optional = OptBoolean.TRUE)
                         @JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }

    }

    static class InputFalse
    {
        @JacksonInject(value = "key", useInput = OptBoolean.FALSE, optional = OptBoolean.TRUE)
        @JsonProperty("field")
        private final String _field;

        @JsonCreator
        public InputFalse(@JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }
    }

    static class InputFalseConstructor
    {
        private final String _field;

        @JsonCreator
        public InputFalseConstructor(@JacksonInject(value = "key", useInput = OptBoolean.FALSE, optional = OptBoolean.TRUE)
                          @JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }
    }

    private final String empty = "{}";
    private final String input = "{\"field\": \"input\"}";

    private final ObjectMapper plainMapper = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE)
            .build();
    private final ObjectMapper injectedMapper = jsonMapperBuilder()
            .injectableValues(new InjectableValues.Std().addValue("key", "injected"))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE)
            .build();

    // When optional = YES, missing injectable should NOT fail
    @Test
    @DisplayName("FAIL_ON_UNKNOWN_INJECT_VALUE NO, optional YES, input NO, injectable NO, useInput DEFAULT|TRUE|FALSE => exception")
    void test1() throws Exception {
        assertNull(plainMapper.readValue(empty, InputDefault.class).getField());
        assertNull(plainMapper.readValue(empty, InputDefaultConstructor.class).getField());
        assertNull(plainMapper.readValue(empty, InputTrue.class).getField());
        assertNull(plainMapper.readValue(empty, InputTrueConstructor.class).getField());
        assertNull(plainMapper.readValue(empty, InputFalse.class).getField());
        assertNull(plainMapper.readValue(empty, InputFalseConstructor.class).getField());
    }

    @Test
    @DisplayName("FAIL_ON_UNKNOWN_INJECT_VALUE NO, optional YES, input NO, injectable YES, useInput DEFAULT|TRUE|FALSE => injected")
    void test2() throws Exception {
        assertEquals("injected", injectedMapper.readValue(empty, InputDefault.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputDefaultConstructor.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputTrue.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputTrueConstructor.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputFalse.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputFalseConstructor.class).getField());
    }

    @Test
    @DisplayName("FAIL_ON_UNKNOWN_INJECT_VALUE NO, optional YES, input YES, injectable NO, useInput DEFAULT|TRUE|FALSE => [varied]")
    void test3() throws Exception {
        assertEquals("input", plainMapper.readValue(input, InputDefault.class).getField());
        assertEquals("input", plainMapper.readValue(input, InputDefaultConstructor.class).getField());
        assertNull(plainMapper.readValue(input, InputFalse.class).getField());
        assertNull(plainMapper.readValue(input, InputFalseConstructor.class).getField());
        assertEquals("input", plainMapper.readValue(input, InputTrue.class).getField());
        assertEquals("input", plainMapper.readValue(input, InputTrueConstructor.class).getField());
    }

    @Test
    @DisplayName("FAIL_ON_UNKNOWN_INJECT_VALUE NO, optional YES, input YES, injectable YES, useInput DEFAULT|FALSE => injected")
    void test4() throws Exception {
        assertEquals("injected", injectedMapper.readValue(input, InputDefault.class).getField());
        assertEquals("injected", injectedMapper.readValue(input, InputDefaultConstructor.class).getField());
        assertEquals("injected", injectedMapper.readValue(input, InputFalse.class).getField());
        assertEquals("injected", injectedMapper.readValue(input, InputFalseConstructor.class).getField());
    }

    @Test
    @DisplayName("FAIL_ON_UNKNOWN_INJECT_VALUE NO, optional YES, input YES, injectable YES, useInput TRUE => input")
    void test5() throws Exception {
        assertEquals("input", injectedMapper.readValue(input, InputTrue.class).getField());
        assertEquals("input", injectedMapper.readValue(input, InputTrueConstructor.class).getField());
    }
}
