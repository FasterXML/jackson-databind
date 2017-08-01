package com.fasterxml.jackson.databind.struct;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// for [databind#1106]
public class ScalarCoercionTest extends BaseMapTest
{
    private final ObjectMapper COERCING_MAPPER = new ObjectMapper(); {
        COERCING_MAPPER.enable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    }

    private final ObjectMapper NOT_COERCING_MAPPER = new ObjectMapper(); {
        NOT_COERCING_MAPPER.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    }

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
    }

    private void _verifyNullFail(Class<?> type) throws IOException
    {
        try {
            NOT_COERCING_MAPPER.readerFor(type).readValue("\"\"");
            fail("Should have failed for "+type);
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String");
            verifyException(e, "Null value for");
        }
    }

    /*
    /**********************************************************
    /* Unit tests: coercion from secondary representations
    /**********************************************************
     */

    public void testStringCoercionOk() throws Exception
    {
        // first successful coercions. Boolean has a ton...
        _verifyCoerceSuccess("1", Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess("1", Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(quote("true"), Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(quote("true"), Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(quote("True"), Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(quote("True"), Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess("0", Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess("0", Boolean.class, Boolean.FALSE);
        _verifyCoerceSuccess(quote("false"), Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(quote("false"), Boolean.class, Boolean.FALSE);
        _verifyCoerceSuccess(quote("False"), Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(quote("False"), Boolean.class, Boolean.FALSE);

        _verifyCoerceSuccess(quote("123"), Byte.TYPE, Byte.valueOf((byte) 123));
        _verifyCoerceSuccess(quote("123"), Byte.class, Byte.valueOf((byte) 123));
        _verifyCoerceSuccess(quote("123"), Short.TYPE, Short.valueOf((short) 123));
        _verifyCoerceSuccess(quote("123"), Short.class, Short.valueOf((short) 123));
        _verifyCoerceSuccess(quote("123"), Integer.TYPE, Integer.valueOf(123));
        _verifyCoerceSuccess(quote("123"), Integer.class, Integer.valueOf(123));
        _verifyCoerceSuccess(quote("123"), Long.TYPE, Long.valueOf(123));
        _verifyCoerceSuccess(quote("123"), Long.class, Long.valueOf(123));
        _verifyCoerceSuccess(quote("123.5"), Float.TYPE, Float.valueOf(123.5f));
        _verifyCoerceSuccess(quote("123.5"), Float.class, Float.valueOf(123.5f));
        _verifyCoerceSuccess(quote("123.5"), Double.TYPE, Double.valueOf(123.5));
        _verifyCoerceSuccess(quote("123.5"), Double.class, Double.valueOf(123.5));

        _verifyCoerceSuccess(quote("123"), BigInteger.class, BigInteger.valueOf(123));
        _verifyCoerceSuccess(quote("123.0"), BigDecimal.class, new BigDecimal("123.0"));
    }

    public void testStringCoercionFail() throws Exception
    {
        _verifyCoerceFail(quote("true"), Boolean.TYPE);
        _verifyCoerceFail(quote("true"), Boolean.class);
        _verifyCoerceFail(quote("123"), Byte.TYPE);
        _verifyCoerceFail(quote("123"), Byte.class);
        _verifyCoerceFail(quote("123"), Short.TYPE);
        _verifyCoerceFail(quote("123"), Short.class);
        _verifyCoerceFail(quote("123"), Integer.TYPE);
        _verifyCoerceFail(quote("123"), Integer.class);
        _verifyCoerceFail(quote("123"), Long.TYPE);
        _verifyCoerceFail(quote("123"), Long.class);
        _verifyCoerceFail(quote("123.5"), Float.TYPE);
        _verifyCoerceFail(quote("123.5"), Float.class);
        _verifyCoerceFail(quote("123.5"), Double.TYPE);
        _verifyCoerceFail(quote("123.5"), Double.class);

        _verifyCoerceFail(quote("123"), BigInteger.class);
        _verifyCoerceFail(quote("123.0"), BigDecimal.class);
    }

    public void testMiscCoercionFail() throws Exception
    {
        // And then we have coercions from more esoteric types too
        _verifyCoerceFail("1", Boolean.TYPE);
        _verifyCoerceFail("1", Boolean.class);

        _verifyCoerceFail("65", Character.class);
        _verifyCoerceFail("65", Character.TYPE);
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

    private void _verifyCoerceFail(String input, Class<?> type) throws IOException
    {
        try {
            NOT_COERCING_MAPPER.readerFor(type)
                .readValue(input);
            fail("Should not have allowed coercion");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce ");
            verifyException(e, " for type `");
            verifyException(e, "enable `MapperFeature.ALLOW_COERCION_OF_SCALARS` to allow");
        }
    }
}
