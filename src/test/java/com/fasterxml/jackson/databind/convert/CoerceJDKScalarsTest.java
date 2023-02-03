package com.fasterxml.jackson.databind.convert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// Tests for "old" coercions (pre-2.12), with `MapperFeature.ALLOW_COERCION_OF_SCALARS`
public class CoerceJDKScalarsTest extends BaseMapTest
{
    static class BooleanPOJO {
        public Boolean value;
    }

    static class BooleanWrapper {
        public Boolean wrapper;
        public boolean primitive;

        protected Boolean ctor;

        @JsonCreator
        public BooleanWrapper(@JsonProperty("ctor") Boolean foo) {
            ctor = foo;
        }
    }

    private final ObjectMapper COERCING_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    private final ObjectMapper NOT_COERCING_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    /*
    /**********************************************************
    /* Unit tests: coercion from empty String
    /**********************************************************
     */

    public void testNullValueFromEmpty() throws Exception
    {
        // wrappers accept `null` fine
        _verifyNullOkFromEmpty(Boolean.class, null);
        // but primitives require non-null
        _verifyNullOkFromEmpty(Boolean.TYPE, Boolean.FALSE);

        _verifyNullOkFromEmpty(Byte.class, null);
        _verifyNullOkFromEmpty(Byte.TYPE, Byte.valueOf((byte) 0));
        _verifyNullOkFromEmpty(Short.class, null);
        _verifyNullOkFromEmpty(Short.TYPE, Short.valueOf((short) 0));
        _verifyNullOkFromEmpty(Character.class, null);
        _verifyNullOkFromEmpty(Character.TYPE, Character.valueOf((char) 0));
        _verifyNullOkFromEmpty(Integer.class, null);
        _verifyNullOkFromEmpty(Integer.TYPE, Integer.valueOf(0));
        _verifyNullOkFromEmpty(Long.class, null);
        _verifyNullOkFromEmpty(Long.TYPE, Long.valueOf(0L));
        _verifyNullOkFromEmpty(Float.class, null);
        _verifyNullOkFromEmpty(Float.TYPE, Float.valueOf(0.0f));
        _verifyNullOkFromEmpty(Double.class, null);
        _verifyNullOkFromEmpty(Double.TYPE, Double.valueOf(0.0));

        _verifyNullOkFromEmpty(BigInteger.class, null);
        _verifyNullOkFromEmpty(BigDecimal.class, null);
        _verifyNullOkFromEmpty(AtomicBoolean.class, null);
    }

    private void _verifyNullOkFromEmpty(Class<?> type, Object exp) throws IOException
    {
        Object result = COERCING_MAPPER.readerFor(type)
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .readValue("\"\"");
        if (exp == null) {
            assertNull(result);
        } else {
            assertEquals(exp, result);
        }
    }

    public void testNullFailFromEmpty() throws Exception
    {
        _verifyNullFail(Boolean.class);
        _verifyNullFail(Boolean.TYPE);

        _verifyNullFail(Byte.class);
        _verifyNullFail(Byte.TYPE);
        _verifyNullFail(Short.class);
        _verifyNullFail(Short.TYPE);
        _verifyNullFail(Character.class);
        _verifyNullFail(Character.TYPE);
        _verifyNullFail(Integer.class);
        _verifyNullFail(Integer.TYPE);
        _verifyNullFail(Long.class);
        _verifyNullFail(Long.TYPE);
        _verifyNullFail(Float.class);
        _verifyNullFail(Float.TYPE);
        _verifyNullFail(Double.class);
        _verifyNullFail(Double.TYPE);

        _verifyNullFail(BigInteger.class);
        _verifyNullFail(BigDecimal.class);
        _verifyNullFail(AtomicBoolean.class);
    }

    private void _verifyNullFail(Class<?> type) throws IOException
    {
        try {
            NOT_COERCING_MAPPER.readerFor(type).readValue("\"\"");
            fail("Should have failed for "+type);
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String");
        }
    }

    /*
    /**********************************************************
    /* Unit tests: coercion from secondary representations
    /**********************************************************
     */

    public void testStringToNumbersCoercionOk() throws Exception
    {
        _verifyCoerceSuccess(q("123"), Byte.TYPE, Byte.valueOf((byte) 123));
        _verifyCoerceSuccess(q("123"), Byte.class, Byte.valueOf((byte) 123));
        _verifyCoerceSuccess(q("123"), Short.TYPE, Short.valueOf((short) 123));
        _verifyCoerceSuccess(q("123"), Short.class, Short.valueOf((short) 123));
        _verifyCoerceSuccess(q("123"), Integer.TYPE, Integer.valueOf(123));
        _verifyCoerceSuccess(q("123"), Integer.class, Integer.valueOf(123));
        _verifyCoerceSuccess(q("123"), Long.TYPE, Long.valueOf(123));
        _verifyCoerceSuccess(q("123"), Long.class, Long.valueOf(123));
        _verifyCoerceSuccess(q("123.5"), Float.TYPE, Float.valueOf(123.5f));
        _verifyCoerceSuccess(q("123.5"), Float.class, Float.valueOf(123.5f));
        _verifyCoerceSuccess(q("123.5"), Double.TYPE, Double.valueOf(123.5));
        _verifyCoerceSuccess(q("123.5"), Double.class, Double.valueOf(123.5));

        _verifyCoerceSuccess(q("123"), BigInteger.class, BigInteger.valueOf(123));
        _verifyCoerceSuccess(q("123.0"), BigDecimal.class, new BigDecimal("123.0"));

        AtomicBoolean ab = COERCING_MAPPER.readValue(q("true"), AtomicBoolean.class);
        assertNotNull(ab);
        assertTrue(ab.get());
    }

    public void testStringCoercionFailInteger() throws Exception
    {
        _verifyRootStringCoerceFail("123", Byte.TYPE);
        _verifyRootStringCoerceFail("123", Byte.class);
        _verifyRootStringCoerceFail("123", Short.TYPE);
        _verifyRootStringCoerceFail("123", Short.class);
        _verifyRootStringCoerceFail("123", Integer.TYPE);
        _verifyRootStringCoerceFail("123", Integer.class);
        _verifyRootStringCoerceFail("123", Long.TYPE);
        _verifyRootStringCoerceFail("123", Long.class);
    }

    public void testStringCoercionFailFloat() throws Exception
    {
        _verifyRootStringCoerceFail("123.5", Float.TYPE);
        _verifyRootStringCoerceFail("123.5", Float.class);
        _verifyRootStringCoerceFail("123.5", Double.TYPE);
        _verifyRootStringCoerceFail("123.5", Double.class);

        _verifyRootStringCoerceFail("123", BigInteger.class);
        _verifyRootStringCoerceFail("123.0", BigDecimal.class);
    }

    public void testMiscCoercionFail() throws Exception
    {
        // And then we have coercions from more esoteric types too

        _verifyCoerceFail("65", Character.class,
                "Cannot coerce Integer value (65) to `java.lang.Character` value");
        _verifyCoerceFail("65", Character.TYPE,
                "Cannot coerce Integer value (65) to `char` value");
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyCoerceSuccess(String input, Class<?> type, Object exp) throws IOException
    {
        Object result = COERCING_MAPPER.readerFor(type)
                .readValue(input);
        assertEquals(exp, result);
    }

    private void _verifyCoerceFail(String input, Class<?> type,
            String... expMatches) throws IOException
    {
        try {
            NOT_COERCING_MAPPER.readerFor(type)
                .readValue(input);
            fail("Should not have allowed coercion");
        } catch (MismatchedInputException e) {
            verifyException(e, expMatches);
        }
    }

    private void _verifyRootStringCoerceFail(String unquotedValue, Class<?> type) throws IOException
    {
        // Test failure for root value: for both byte- and char-backed sources:

        final String input = q(unquotedValue);
        try (JsonParser p = NOT_COERCING_MAPPER.createParser(new StringReader(input))) {
            _verifyStringCoerceFail(p, unquotedValue, type);
        }
        final byte[] inputBytes = utf8Bytes(input);
        try (JsonParser p = NOT_COERCING_MAPPER.createParser(new ByteArrayInputStream(inputBytes))) {
            _verifyStringCoerceFail(p, unquotedValue, type);
        }
    }

    private void _verifyStringCoerceFail(JsonParser p,
            String unquotedValue, Class<?> type) throws IOException
    {
        try {
            NOT_COERCING_MAPPER.readerFor(type)
                .readValue(p);
            fail("Should not have allowed coercion");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce ");
            verifyException(e, " to `");
            verifyException(e, "` value");

            assertNotNull(e.getProcessor());
            assertSame(p, e.getProcessor());

            assertToken(JsonToken.VALUE_STRING, p.currentToken());
            assertEquals(unquotedValue, p.getText());
        }
    }
}
