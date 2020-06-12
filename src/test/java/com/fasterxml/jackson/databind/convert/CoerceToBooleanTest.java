package com.fasterxml.jackson.databind.convert;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class CoerceToBooleanTest extends BaseMapTest
{
    static class BooleanPOJO {
        public boolean value;
    }

    private final ObjectMapper DEFAULT_MAPPER = sharedMapper();

    private final ObjectMapper LEGACY_NONCOERCING_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    private final ObjectMapper MAPPER_TO_EMPTY; {
        MAPPER_TO_EMPTY = newJsonMapper();
        MAPPER_TO_EMPTY.coercionConfigFor(LogicalType.Boolean)
            .setCoercion(CoercionInputShape.Integer, CoercionAction.AsEmpty);
    }

    private final ObjectMapper MAPPER_TRY_CONVERT; {
        MAPPER_TRY_CONVERT = newJsonMapper();
        MAPPER_TRY_CONVERT.coercionConfigFor(LogicalType.Boolean)
            .setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert);
    }

    private final ObjectMapper MAPPER_TO_NULL; {
        MAPPER_TO_NULL = newJsonMapper();
        MAPPER_TO_NULL.coercionConfigFor(LogicalType.Boolean)
            .setCoercion(CoercionInputShape.Integer, CoercionAction.AsNull);
    }

    private final ObjectMapper MAPPER_TO_FAIL; {
        MAPPER_TO_FAIL = newJsonMapper();
        MAPPER_TO_FAIL.coercionConfigFor(LogicalType.Boolean)
            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
    }

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
    /* Unit tests: new CoercionConfig, as-null, as-empty, try-coerce
    /**********************************************************
     */

    public void testIntToNullCoercion() throws Exception
    {
        assertNull(MAPPER_TO_NULL.readValue("0", Boolean.class));
        assertNull(MAPPER_TO_NULL.readValue("1", Boolean.class));

        // but due to coercion to `boolean`, can not return null here -- however,
        // goes "1 -> false (no null for primitive) -> Boolean.FALSE
        assertEquals(Boolean.FALSE, MAPPER_TO_NULL.readValue("0", Boolean.TYPE));
        assertEquals(Boolean.FALSE, MAPPER_TO_NULL.readValue("1", Boolean.TYPE));

        // As to AtomicBoolean: that type itself IS nullable since it's of LogicalType.Boolean so
        assertNull(MAPPER_TO_NULL.readValue("0", AtomicBoolean.class));
        assertNull(MAPPER_TO_NULL.readValue("1", AtomicBoolean.class));

        BooleanPOJO p;
        p = MAPPER_TO_NULL.readValue(DOC_WITH_0, BooleanPOJO.class);
        assertFalse(p.value);
        p = MAPPER_TO_NULL.readValue(DOC_WITH_1, BooleanPOJO.class);
        assertFalse(p.value);
    }

    public void testIntToEmptyCoercion() throws Exception
    {
        // "empty" value for Boolean/boolean is False/false

        assertEquals(Boolean.FALSE, MAPPER_TO_EMPTY.readValue("0", Boolean.class));
        assertEquals(Boolean.FALSE, MAPPER_TO_EMPTY.readValue("1", Boolean.class));

        assertEquals(Boolean.FALSE, MAPPER_TO_EMPTY.readValue("0", Boolean.TYPE));
        assertEquals(Boolean.FALSE, MAPPER_TO_EMPTY.readValue("1", Boolean.TYPE));

        AtomicBoolean ab;
        ab = MAPPER_TO_EMPTY.readValue("0", AtomicBoolean.class);
        assertFalse(ab.get());
        ab = MAPPER_TO_EMPTY.readValue("1", AtomicBoolean.class);
        assertFalse(ab.get());

        BooleanPOJO p;
        p = MAPPER_TO_EMPTY.readValue(DOC_WITH_0, BooleanPOJO.class);
        assertFalse(p.value);
        p = MAPPER_TO_EMPTY.readValue(DOC_WITH_1, BooleanPOJO.class);
        assertFalse(p.value);
    }
        
    public void testIntToTryCoercion() throws Exception
    {
        // And "TryCoerce" should do what would be typically expected

        assertEquals(Boolean.FALSE, MAPPER_TRY_CONVERT.readValue("0", Boolean.class));
        assertEquals(Boolean.TRUE, MAPPER_TRY_CONVERT.readValue("1", Boolean.class));

        assertEquals(Boolean.FALSE, MAPPER_TRY_CONVERT.readValue("0", Boolean.TYPE));
        assertEquals(Boolean.TRUE, MAPPER_TRY_CONVERT.readValue("1", Boolean.TYPE));

        AtomicBoolean ab;
        ab = MAPPER_TRY_CONVERT.readValue("0", AtomicBoolean.class);
        assertFalse(ab.get());
        ab = MAPPER_TRY_CONVERT.readValue("1", AtomicBoolean.class);
        assertTrue(ab.get());

        BooleanPOJO p;
        p = MAPPER_TRY_CONVERT.readValue(DOC_WITH_0, BooleanPOJO.class);
        assertFalse(p.value);
        p = MAPPER_TRY_CONVERT.readValue(DOC_WITH_1, BooleanPOJO.class);
        assertTrue(p.value);
    }

    /*
    /**********************************************************
    /* Unit tests: new CoercionConfig, failing
    /**********************************************************
     */

    public void testFailFromInteger() throws Exception
    {
        _verifyFailFromInteger(MAPPER_TO_FAIL, BooleanPOJO.class, DOC_WITH_0, Boolean.TYPE);
        _verifyFailFromInteger(MAPPER_TO_FAIL, BooleanPOJO.class, DOC_WITH_1, Boolean.TYPE);

        _verifyFailFromInteger(MAPPER_TO_FAIL, Boolean.class, "0");
        _verifyFailFromInteger(MAPPER_TO_FAIL, Boolean.class, "42");

        _verifyFailFromInteger(MAPPER_TO_FAIL, Boolean.TYPE, "0");
        _verifyFailFromInteger(MAPPER_TO_FAIL, Boolean.TYPE, "999");

        _verifyFailFromInteger(MAPPER_TO_FAIL, AtomicBoolean.class, "0");
        _verifyFailFromInteger(MAPPER_TO_FAIL, AtomicBoolean.class, "-123");
    }

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

    private void _verifyFailFromInteger(ObjectMapper m, Class<?> targetType, String doc) throws Exception {
        _verifyFailFromInteger(m, targetType, doc, targetType);
    }

    private void _verifyFailFromInteger(ObjectMapper m, Class<?> targetType, String doc,
            Class<?> valueType) throws Exception
    {       
        try {
            m.readerFor(targetType).readValue(doc);
            fail("Should not accept Integer for "+targetType.getName()+" by default");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce Integer value");
            verifyException(e, "to `"+valueType.getName()+"`");
        }
    }
}
