package com.fasterxml.jackson.databind.convert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class CoerceToBooleanTest extends BaseMapTest
{
    static class BooleanPOJO {
        public boolean value;

        public void setValue(boolean v) { value = v; }
    }

    static class BooleanPrimitiveBean
    {
        public boolean booleanValue = true;

        public void setBooleanValue(boolean v) { booleanValue = v; }
    }

    static class BooleanWrapper {
        public Boolean wrapper;
        public boolean primitive;

        protected Boolean ctor;

        @JsonCreator
        public BooleanWrapper(@JsonProperty("ctor") Boolean foo) {
            ctor = foo;
        }

        public void setWrapper(Boolean v) { wrapper = v; }
        public void setPrimitive(boolean v) { primitive = v; }
    }

    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper LEGACY_NONCOERCING_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    private final ObjectMapper MAPPER_INT_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Boolean, cfg ->
                cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.AsEmpty))
            .build();

    private final ObjectMapper MAPPER_INT_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Boolean, cfg ->
                cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert))
            .build();

    private final ObjectMapper MAPPER_INT_TO_NULL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Boolean, cfg ->
                cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.AsNull))
            .build();

    private final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Boolean, cfg ->
                cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .build();

    private final static String DOC_WITH_0 = a2q("{'value':0}");
    private final static String DOC_WITH_1 = a2q("{'value':1}");

    /*
    /**********************************************************
    /* Unit tests: default, legacy configuration, from String
    /**********************************************************
     */

    // for [databind#403]
    public void testEmptyStringFailForBooleanPrimitive() throws IOException
    {
        final ObjectReader reader = DEFAULT_MAPPER
                .readerFor(BooleanPrimitiveBean.class)
                .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        try {
            reader.readValue(a2q("{'booleanValue':''}"));
            fail("Expected failure for boolean + empty String");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce `null` to `boolean`");
            verifyException(e, "FAIL_ON_NULL_FOR_PRIMITIVES");
        }
    }

    public void testStringToBooleanCoercionOk() throws Exception
    {
        // first successful coercions. Boolean has a ton...
        _verifyCoerceSuccess(DEFAULT_MAPPER, "1", Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, "1", Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("true"), Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("true"), Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("True"), Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("True"), Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("TRUE"), Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("TRUE"), Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, "0", Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, "0", Boolean.class, Boolean.FALSE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("false"), Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("false"), Boolean.class, Boolean.FALSE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("False"), Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("False"), Boolean.class, Boolean.FALSE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("FALSE"), Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(DEFAULT_MAPPER, q("FALSE"), Boolean.class, Boolean.FALSE);
    }

    private void _verifyCoerceSuccess(ObjectMapper mapper,
            String input, Class<?> type, Object exp) throws IOException
    {
        Object result = mapper.readerFor(type)
                .readValue(input);
        assertEquals(exp, result);
    }

    public void testStringToBooleanCoercionFail() throws Exception
    {
        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "true", Boolean.TYPE);
        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "true", Boolean.class);
        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "True", Boolean.TYPE);
        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "True", Boolean.class);
        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "TRUE", Boolean.TYPE);
        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "TRUE", Boolean.class);

        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "false", Boolean.TYPE);
        _verifyRootStringCoerceFail(LEGACY_NONCOERCING_MAPPER, "false", Boolean.class);
    }

    private void _verifyRootStringCoerceFail(ObjectMapper nonCoercingMapper,
            String unquotedValue, Class<?> type) throws IOException
    {
        // Test failure for root value: for both byte- and char-backed sources:

        final String input = q(unquotedValue);
        try (JsonParser p = nonCoercingMapper.createParser(new StringReader(input))) {
            _verifyStringCoerceFail(nonCoercingMapper, p, unquotedValue, type);
        }
        final byte[] inputBytes = utf8Bytes(input);
        try (JsonParser p = nonCoercingMapper.createParser(new ByteArrayInputStream(inputBytes))) {
            _verifyStringCoerceFail(nonCoercingMapper, p, unquotedValue, type);
        }
    }

    private void _verifyStringCoerceFail(ObjectMapper nonCoercingMapper,
            JsonParser p,
            String unquotedValue, Class<?> type) throws IOException
    {
        try {
            nonCoercingMapper.readerFor(type)
                .readValue(p);
            fail("Should not have allowed coercion");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce ");
            verifyException(e, " to `");
            verifyException(e, "` value");

            assertSame(p, e.getProcessor());

            assertToken(JsonToken.VALUE_STRING, p.currentToken());
            assertEquals(unquotedValue, p.getText());
        }
    }

    /*
    /**********************************************************
    /* Unit tests: default, legacy configuration, from Int
    /**********************************************************
     */

    public void testIntToBooleanCoercionSuccessPojo() throws Exception
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

    public void testIntToBooleanCoercionSuccessRoot() throws Exception
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

    // Test for verifying that Long values are coerced to boolean correctly as well
    public void testLongToBooleanCoercionOk() throws Exception
    {
        long value = 1L + Integer.MAX_VALUE;
        BooleanWrapper b = DEFAULT_MAPPER.readValue("{\"primitive\" : "+value+", \"wrapper\":"+value+", \"ctor\":"+value+"}",
                BooleanWrapper.class);
        assertEquals(Boolean.TRUE, b.wrapper);
        assertTrue(b.primitive);
        assertEquals(Boolean.TRUE, b.ctor);

        // but ensure we can also get `false`
        b = DEFAULT_MAPPER.readValue("{\"primitive\" : 0 , \"wrapper\":0, \"ctor\":0}",
                BooleanWrapper.class);
        assertEquals(Boolean.FALSE, b.wrapper);
        assertFalse(b.primitive);
        assertEquals(Boolean.FALSE, b.ctor);

        boolean[] boo = DEFAULT_MAPPER.readValue("[ 0, 15, \"\", \"false\", \"True\" ]",
                boolean[].class);
        assertEquals(5, boo.length);
        assertFalse(boo[0]);
        assertTrue(boo[1]);
        assertFalse(boo[2]);
        assertFalse(boo[3]);
        assertTrue(boo[4]);
    }

    // [databind#2635], [databind#2770]
    public void testIntToBooleanCoercionFailBytes() throws Exception
    {
        _verifyBooleanCoerceFail(a2q("{'value':1}"), true, JsonToken.VALUE_NUMBER_INT, "1", BooleanPOJO.class);

        _verifyBooleanCoerceFail("1", true, JsonToken.VALUE_NUMBER_INT, "1", Boolean.TYPE);
        _verifyBooleanCoerceFail("1", true, JsonToken.VALUE_NUMBER_INT, "1", Boolean.class);

        _verifyBooleanCoerceFail("1.25", true, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.TYPE);
        _verifyBooleanCoerceFail("1.25", true, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.class);
    }

    // [databind#2635], [databind#2770]
    public void testIntToBooleanCoercionFailChars() throws Exception
    {
        _verifyBooleanCoerceFail(a2q("{'value':1}"), false, JsonToken.VALUE_NUMBER_INT, "1", BooleanPOJO.class);

        _verifyBooleanCoerceFail("1", false, JsonToken.VALUE_NUMBER_INT, "1", Boolean.TYPE);
        _verifyBooleanCoerceFail("1", false, JsonToken.VALUE_NUMBER_INT, "1", Boolean.class);

        _verifyBooleanCoerceFail("1.25", false, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.TYPE);
        _verifyBooleanCoerceFail("1.25", false, JsonToken.VALUE_NUMBER_FLOAT, "1.25", Boolean.class);
    }

    /*
    /**********************************************************
    /* Unit tests: new CoercionConfig, as-null, as-empty, try-coerce
    /**********************************************************
     */

    public void testIntToNullCoercion() throws Exception
    {
        assertNull(MAPPER_INT_TO_NULL.readValue("0", Boolean.class));
        assertNull(MAPPER_INT_TO_NULL.readValue("1", Boolean.class));

        // but due to coercion to `boolean`, can not return null here -- however,
        // goes "1 -> false (no null for primitive) -> Boolean.FALSE
        assertEquals(Boolean.FALSE, MAPPER_INT_TO_NULL.readValue("0", Boolean.TYPE));
        assertEquals(Boolean.FALSE, MAPPER_INT_TO_NULL.readValue("1", Boolean.TYPE));

        // As to AtomicBoolean: that type itself IS nullable since it's of LogicalType.Boolean so
        assertNull(MAPPER_INT_TO_NULL.readValue("0", AtomicBoolean.class));
        assertNull(MAPPER_INT_TO_NULL.readValue("1", AtomicBoolean.class));

        BooleanPOJO p;
        p = MAPPER_INT_TO_NULL.readValue(DOC_WITH_0, BooleanPOJO.class);
        assertFalse(p.value);
        p = MAPPER_INT_TO_NULL.readValue(DOC_WITH_1, BooleanPOJO.class);
        assertFalse(p.value);
    }

    public void testIntToEmptyCoercion() throws Exception
    {
        // "empty" value for Boolean/boolean is False/false

        assertEquals(Boolean.FALSE, MAPPER_INT_TO_EMPTY.readValue("0", Boolean.class));
        assertEquals(Boolean.FALSE, MAPPER_INT_TO_EMPTY.readValue("1", Boolean.class));

        assertEquals(Boolean.FALSE, MAPPER_INT_TO_EMPTY.readValue("0", Boolean.TYPE));
        assertEquals(Boolean.FALSE, MAPPER_INT_TO_EMPTY.readValue("1", Boolean.TYPE));

        AtomicBoolean ab;
        ab = MAPPER_INT_TO_EMPTY.readValue("0", AtomicBoolean.class);
        assertFalse(ab.get());
        ab = MAPPER_INT_TO_EMPTY.readValue("1", AtomicBoolean.class);
        assertFalse(ab.get());

        BooleanPOJO p;
        p = MAPPER_INT_TO_EMPTY.readValue(DOC_WITH_0, BooleanPOJO.class);
        assertFalse(p.value);
        p = MAPPER_INT_TO_EMPTY.readValue(DOC_WITH_1, BooleanPOJO.class);
        assertFalse(p.value);
    }

    public void testIntToTryCoercion() throws Exception
    {
        // And "TryCoerce" should do what would be typically expected

        assertEquals(Boolean.FALSE, MAPPER_INT_TRY_CONVERT.readValue("0", Boolean.class));
        assertEquals(Boolean.TRUE, MAPPER_INT_TRY_CONVERT.readValue("1", Boolean.class));

        assertEquals(Boolean.FALSE, MAPPER_INT_TRY_CONVERT.readValue("0", Boolean.TYPE));
        assertEquals(Boolean.TRUE, MAPPER_INT_TRY_CONVERT.readValue("1", Boolean.TYPE));

        AtomicBoolean ab;
        ab = MAPPER_INT_TRY_CONVERT.readValue("0", AtomicBoolean.class);
        assertFalse(ab.get());
        ab = MAPPER_INT_TRY_CONVERT.readValue("1", AtomicBoolean.class);
        assertTrue(ab.get());

        BooleanPOJO p;
        p = MAPPER_INT_TRY_CONVERT.readValue(DOC_WITH_0, BooleanPOJO.class);
        assertFalse(p.value);
        p = MAPPER_INT_TRY_CONVERT.readValue(DOC_WITH_1, BooleanPOJO.class);
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
        verifyException(e, "Cannot coerce ", "Cannot deserialize value of type ");

        JsonParser p = (JsonParser) e.getProcessor();

        assertToken(tokenType, p.currentToken());

        final String text = p.getText();
        if (!tokenValue.equals(text)) {
            String textDesc = (text == null) ? "NULL" : q(text);
            fail("Token text ("+textDesc+") via parser of type "+p.getClass().getName()
                    +" not as expected ("+q(tokenValue)+")");
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
