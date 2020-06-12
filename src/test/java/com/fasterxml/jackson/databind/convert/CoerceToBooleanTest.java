package com.fasterxml.jackson.databind.convert;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class CoerceToBooleanTest extends BaseMapTest
{
    static class BooleanPOJO {
        public boolean value;
    }

    private final ObjectMapper DEFAULT_MAPPER = sharedMapper();

    private final ObjectMapper LEGACY_NONCOERCING_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    private final static String DOC_WITH_0 = aposToQuotes("{'value':0}");
    private final static String DOC_WITH_1 = aposToQuotes("{'value':1}");
    
    /*
    /**********************************************************
    /* Unit tests: default, legacy configuration
    /**********************************************************
     */

    public void testToBooleanCoercionSuccessPojo() throws Exception
    {        
        BooleanPOJO p;
        final ObjectReader r = DEFAULT_MAPPER.readerFor(BooleanPOJO.class);

        p = r.readValue(DOC_WITH_0);
        assertEquals(false, p.value);
        p = r.readValue(utf8Bytes(DOC_WITH_0));
        assertEquals(false, p.value);

        p = r.readValue(DOC_WITH_1);
        assertEquals(true, p.value);
        p = r.readValue(utf8Bytes(DOC_WITH_1));
        assertEquals(true, p.value);
    }

    public void testToBooleanCoercionSuccessRoot() throws Exception
    {        
        final ObjectReader br = DEFAULT_MAPPER.readerFor(Boolean.class);

        assertEquals(Boolean.FALSE, br.readValue(" 0"));
        assertEquals(Boolean.FALSE, br.readValue(utf8Bytes(" 0")));
        assertEquals(Boolean.TRUE, br.readValue(" -1"));
        assertEquals(Boolean.TRUE, br.readValue(utf8Bytes(" -1")));

        final ObjectReader atomicR = DEFAULT_MAPPER.readerFor(AtomicBoolean.class);

        AtomicBoolean ab;
        
        ab = atomicR.readValue(" 0");
        ab = atomicR.readValue(utf8Bytes(" 0"));
        assertEquals(false, ab.get());

        ab = atomicR.readValue(" 111");
        assertEquals(true, ab.get());
        ab = atomicR.readValue(utf8Bytes(" 111"));
        assertEquals(true, ab.get());
    }

    public void testToBooleanCoercionFailBytes() throws Exception
    {
        _verifyBooleanCoerceFail(aposToQuotes("{'value':1}"), true, JsonToken.VALUE_NUMBER_INT, "1", BooleanPOJO.class);

        _verifyBooleanCoerceFail("1", true, JsonToken.VALUE_NUMBER_INT, "1", Boolean.TYPE);
        _verifyBooleanCoerceFail("1", true, JsonToken.VALUE_NUMBER_INT, "1", Boolean.class);
    }

    public void testToBooleanCoercionFailChars() throws Exception
    {
        _verifyBooleanCoerceFail(aposToQuotes("{'value':1}"), false, JsonToken.VALUE_NUMBER_INT, "1", BooleanPOJO.class);

        _verifyBooleanCoerceFail("1", false, JsonToken.VALUE_NUMBER_INT, "1", Boolean.TYPE);
        _verifyBooleanCoerceFail("1", false, JsonToken.VALUE_NUMBER_INT, "1", Boolean.class);
    }

    /*
    /**********************************************************
    /* Unit tests: new CoercionConfig
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyBooleanCoerceFail(String doc, boolean useBytes,
            JsonToken tokenType, String tokenValue, Class<?> targetType) throws IOException
    {
        // Test failure for root value: for both byte- and char-backed sources.

        // [databind#2635]: important, need to use `readValue()` that takes content and NOT
        // JsonParser, as this forces closing of underlying parser and exposes more issues.

        final ObjectReader r = LEGACY_NONCOERCING_MAPPER.readerFor(targetType);
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
        verifyException(e, "Cannot coerce ");
        verifyException(e, " to `");

        JsonParser p = (JsonParser) e.getProcessor();

        assertToken(tokenType, p.currentToken());

        final String text = p.getText();
        if (!tokenValue.equals(text)) {
            String textDesc = (text == null) ? "NULL" : quote(text);
            fail("Token text ("+textDesc+") via parser of type "+p.getClass().getName()
                    +" not as expected ("+quote(tokenValue)+")");
        }
    }
}
