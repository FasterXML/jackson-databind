package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.JsonSetter;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

public class BuilderErrorHandling extends BaseMapTest
{
    @JsonDeserialize(builder=SimpleBuilderXY.class)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    static class SimpleBuilderXY
    {
        int x, y;

        public SimpleBuilderXY withX(int x0) {
              this.x = x0;
              return this;
        }

        public SimpleBuilderXY withY(int y0) {
              this.y = y0;
              return this;
        }

        public ValueClassXY build() {
              return new ValueClassXY(x, y);
        }
    }

    // [databind#2938]
    @JsonDeserialize(builder = ValidatingValue.Builder.class)
    static class ValidatingValue
    {
        final String first;
        final String second;

        ValidatingValue(String first, String second) {
            this.first = first;
            this.second = second;
        }

        @SuppressWarnings("serial")
        static class ValidationException extends RuntimeException
        {
            ValidationException(String message) {
                super(message);
            }
        }

        static class Builder
        {
            private String first;
            private String second;

            @JsonSetter("a")
            Builder first(String value) {
                this.first = value;
                return this;
            }

            @JsonSetter("b")
            Builder second(String value) {
                this.second = value;
                return this;
            }

            ValidatingValue build() {
                if (first == null) {
                    throw new ValidationException("Missing first");
                }
                if (second == null) {
                    throw new ValidationException("Missing second");
                }
                return new ValidatingValue(first, second);
            }
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_WITH_WRAPPING = jsonMapperBuilder()
            .enable(DeserializationFeature.WRAP_EXCEPTIONS)
            .build();

    private final ObjectMapper MAPPER_NO_WRAPPING = jsonMapperBuilder()
            .disable(DeserializationFeature.WRAP_EXCEPTIONS)
            .build();

    public void testUnknownProperty() throws Exception
    {
        // first, default failure
        String json = a2q("{'x':1,'z':2,'y':4}");
        try {
            MAPPER.readValue(json, ValueClassXY.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unrecognized field");
        }
        // but pass if ok to ignore
        ValueClassXY result = MAPPER.readerFor(ValueClassXY.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertEquals(2, result._x);
        assertEquals(5, result._y);
    }

    public void testWrongShape() throws Exception
    {
        try {
            MAPPER.readValue("123", ValueClassXY.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot construct instance of ");
            // should report Builder class, not value here, right?
            verifyException(e, "$SimpleBuilderXY");
        }
    }

    // [databind#2938]

    public void testSuccessfulValidatingBuilder() throws Exception
    {
        ValidatingValue result = MAPPER.readValue(a2q("{'a':'1','b':'2'}"), ValidatingValue.class);
        assertEquals("1", result.first);
        assertEquals("2", result.second);
    }

    public void testFailingValidatingBuilderWithExceptionWrapping() throws Exception
    {
        try {
            MAPPER_WITH_WRAPPING.readValue(a2q("{'a':'1'}"), ValidatingValue.class);
            fail("Expected an exception");
        } catch (ValueInstantiationException e) {
            verifyException(e, "Missing second");
            assertTrue(e.getCause() instanceof ValidatingValue.ValidationException);
        }
    }

    public void testFailingValidatingBuilderWithExceptionWrappingFromTree() throws Exception
    {
        try {
            JsonNode tree = MAPPER_WITH_WRAPPING.readTree(a2q("{'a':'1'}"));
            MAPPER_WITH_WRAPPING.treeToValue(tree, ValidatingValue.class);
            fail("Expected an exception");
        } catch (ValueInstantiationException e) {
            verifyException(e, "Missing second");
            assertTrue(e.getCause() instanceof ValidatingValue.ValidationException);
        }
    }

    public void testFailingValidatingBuilderWithoutExceptionWrapping() throws Exception
    {
        try {
            MAPPER_NO_WRAPPING
                    .readValue(a2q("{'a':'1'}"), ValidatingValue.class);
            fail("Expected an exception");
        } catch (ValidatingValue.ValidationException e) {
            assertEquals("Missing second", e.getMessage());
        }
    }

    public void testFailingValidatingBuilderWithoutExceptionWrappingFromTree() throws Exception
    {
        try {
            JsonNode tree = MAPPER_NO_WRAPPING.readTree(a2q("{'a':'1'}"));
            MAPPER_NO_WRAPPING.treeToValue(tree, ValidatingValue.class);
            fail("Expected an exception");
        } catch (ValidatingValue.ValidationException e) {
            assertEquals("Missing second", e.getMessage());
        }
    }
}
