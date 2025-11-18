package tools.jackson.databind.deser.inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MissingInjectableValueExcepion;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacksonInject1381Test extends DatabindTestUtil
{
    static class InputDefault
    {
        @JacksonInject(value = "key")
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
        public InputDefaultConstructor(@JacksonInject(value = "key")
                                       @JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }
    }

    static class InputTrue
    {
        @JacksonInject(value = "key", useInput = OptBoolean.TRUE)
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
        public InputTrueConstructor(@JacksonInject(value = "key", useInput = OptBoolean.TRUE)
                                    @JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }

    }

    static class InputFalse
    {
        @JacksonInject(value = "key", useInput = OptBoolean.FALSE)
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
        public InputFalseConstructor(@JacksonInject(value = "key", useInput = OptBoolean.FALSE)
                                     @JsonProperty("field") final String field) {
            _field = field;
        }

        public String getField() {
            return _field;
        }
    }

    private final String empty = "{}";
    private final String input = "{\"field\": \"input\"}";

    private final ObjectMapper plainMapper = newJsonMapper();
    private final ObjectMapper injectedMapper = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE)
            .injectableValues(new InjectableValues.Std().addValue("key", "injected"))
            .build();

    @Test
    @DisplayName("input NO, injectable NO, useInput DEFAULT|TRUE|FALSE => exception")
    void test1() {
        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(empty, InputDefault.class));
        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(empty, InputDefaultConstructor.class));

        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(empty, InputTrue.class));
        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(empty, InputTrueConstructor.class));

        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(empty, InputFalse.class));
        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(empty, InputFalseConstructor.class));
    }

    @Test
    @DisplayName("input NO, injectable YES, useInput DEFAULT|TRUE|FALSE => injected")
    void test2() throws Exception {
        assertEquals("injected", injectedMapper.readValue(empty, InputDefault.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputDefaultConstructor.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputTrue.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputTrueConstructor.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputFalse.class).getField());
        assertEquals("injected", injectedMapper.readValue(empty, InputFalseConstructor.class).getField());
    }

    @Test
    @DisplayName("input YES, injectable NO, useInput DEFAULT|FALSE => exception")
    void test3() {
        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(input, InputDefault.class));
        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(input, InputDefaultConstructor.class));

        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(input, InputFalse.class));
        assertThrows(MissingInjectableValueExcepion.class,
                () -> plainMapper.readValue(input, InputFalseConstructor.class));
    }

    @Test
    @DisplayName("input YES, injectable NO, useInput TRUE => input")
    void test4() throws Exception {
        assertEquals("input", plainMapper.readValue(input, InputTrue.class).getField());
        assertEquals("input", plainMapper.readValue(input, InputTrueConstructor.class).getField());
    }

    @Test
    @DisplayName("input YES, injectable YES, useInput DEFAULT|FALSE => injected")
    void test5() throws Exception {
        assertEquals("injected", injectedMapper.readValue(input, InputDefault.class).getField());
        assertEquals("injected", injectedMapper.readValue(input, InputDefaultConstructor.class).getField());
        assertEquals("injected", injectedMapper.readValue(input, InputFalse.class).getField());
        assertEquals("injected", injectedMapper.readValue(input, InputFalseConstructor.class).getField());
    }

    @Test
    @DisplayName("input YES, injectable YES, useInput TRUE => input")
    void test6() throws Exception {
        assertEquals("input", injectedMapper.readValue(input, InputTrue.class).getField());
        assertEquals("input", injectedMapper.readValue(input, InputTrueConstructor.class).getField());
    }
}
