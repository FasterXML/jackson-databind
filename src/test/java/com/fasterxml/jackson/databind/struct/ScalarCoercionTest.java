package com.fasterxml.jackson.databind.struct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class ScalarCoercionTest extends BaseMapTest
{
    static class BooleanPOJO {
        public boolean value;
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
        _verifyRootStringCoerceFail("true", Boolean.TYPE);
        _verifyRootStringCoerceFail("true", Boolean.class);
        _verifyRootStringCoerceFail("123", Byte.TYPE);
        _verifyRootStringCoerceFail("123", Byte.class);
        _verifyRootStringCoerceFail("123", Short.TYPE);
        _verifyRootStringCoerceFail("123", Short.class);
        _verifyRootStringCoerceFail("123", Integer.TYPE);
        _verifyRootStringCoerceFail("123", Integer.class);
        _verifyRootStringCoerceFail("123", Long.TYPE);
        _verifyRootStringCoerceFail("123", Long.class);
        _verifyRootStringCoerceFail("123.5", Float.TYPE);
        _verifyRootStringCoerceFail("123.5", Float.class);
        _verifyRootStringCoerceFail("123.5", Double.TYPE);
        _verifyRootStringCoerceFail("123.5", Double.class);

        _verifyRootStringCoerceFail("123", BigInteger.class);
        _verifyRootStringCoerceFail("123.0", BigDecimal.class);
    }

    // [databind#2635], [databind#2770]
    public void testToBooleanCoercionFailBytes() throws Exception
    {
        final String beanDoc = aposToQuotes("{'value':1}");
        _verifyBooleanCoerceFail("1", true, JsonToken.VALUE_NUMBER_INT, "1", Boolean.TYPE);
        _verifyBooleanCoerceFail("1", true, JsonToken.VALUE_NUMBER_INT, "1", Boolean.class);
        _verifyBooleanCoerceFail(beanDoc, true, JsonToken.VALUE_NUMBER_INT, "1", BooleanPOJO.class);

        _verifyBooleanCoerceFail("1.25", true, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.TYPE);
        _verifyBooleanCoerceFail("1.25", true, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.class);
    }

    // [databind#2635], [databind#2770]
    public void testToBooleanCoercionFailChars() throws Exception
    {
        final String beanDoc = aposToQuotes("{'value':1}");
        _verifyBooleanCoerceFail("1", false, JsonToken.VALUE_NUMBER_INT, "1", Boolean.TYPE);
        _verifyBooleanCoerceFail("1", false, JsonToken.VALUE_NUMBER_INT, "1", Boolean.class);
        _verifyBooleanCoerceFail(beanDoc, false, JsonToken.VALUE_NUMBER_INT, "1", BooleanPOJO.class);

        _verifyBooleanCoerceFail("1.25", false, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.TYPE);
        _verifyBooleanCoerceFail("1.25", false, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.class);
    }

    public void testMiscCoercionFail() throws Exception
    {
        // And then we have coercions from more esoteric types too

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

    private void _verifyRootStringCoerceFail(String unquotedValue, Class<?> type) throws IOException
    {
        // Test failure for root value: for both byte- and char-backed sources:

        final String input = quote(unquotedValue);
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
            verifyException(e, " for type `");
            verifyException(e, "enable `MapperFeature.ALLOW_COERCION_OF_SCALARS` to allow");

            assertNotNull(e.getProcessor());
            assertSame(p, e.getProcessor());

            assertToken(JsonToken.VALUE_STRING, p.currentToken());
            assertEquals(unquotedValue, p.getText());
        }
    }

    private void _verifyBooleanCoerceFail(String doc, boolean useBytes,
            JsonToken tokenType, String tokenValue, Class<?> targetType) throws IOException
    {
        // Test failure for root value: for both byte- and char-backed sources.

        // [databind#2635]: important, need to use `readValue()` that takes content and NOT
        // JsonParser, as this forces closing of underlying parser and exposes more issues.

        final ObjectReader r = NOT_COERCING_MAPPER.readerFor(targetType);
        try {
            if (useBytes) {
                r.readValue(utf8Bytes(doc));
            } else {
                r.readValue(doc);
            }
            fail("Should not have allowed coercion");
        } catch (MismatchedInputException e) {
            _verifyBooleanCoerceFailReason(e, tokenType, tokenValue);
        }
    }

    @SuppressWarnings("resource")
    private void _verifyBooleanCoerceFailReason(MismatchedInputException e,
            JsonToken tokenType, String tokenValue) throws IOException
    {
        // 2 different possibilities here
        verifyException(e, "Cannot coerce Number", "Cannot deserialize instance of `");

        JsonParser p = (JsonParser) e.getProcessor();

        assertToken(tokenType, p.currentToken());

        final String text = p.getText();

        if (!tokenValue.equals(text)) {
            String textDesc = (text == null) ? "NULL" : quote(text);
            fail("Token text ("+textDesc+") via parser of type "+p.getClass().getName()
                    +" not as expected ("+quote(tokenValue)+"); exception message: '"+e.getMessage()+"'");
        }
    }
}
