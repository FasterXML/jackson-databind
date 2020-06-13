package com.fasterxml.jackson.databind.convert;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class CoerceFloatToIntTest extends BaseMapTest
{
    private final ObjectMapper DEFAULT_MAPPER = sharedMapper();
    private final ObjectReader READER_LEGACY_FAIL = DEFAULT_MAPPER.reader()
            .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

    private final ObjectMapper MAPPER_TO_EMPTY; {
        MAPPER_TO_EMPTY = newJsonMapper();
        MAPPER_TO_EMPTY.coercionConfigFor(LogicalType.Integer)
            .setCoercion(CoercionInputShape.Float, CoercionAction.AsEmpty);
    }

    private final ObjectMapper MAPPER_TRY_CONVERT; {
        MAPPER_TRY_CONVERT = newJsonMapper();
        MAPPER_TRY_CONVERT.coercionConfigFor(LogicalType.Integer)
            .setCoercion(CoercionInputShape.Float, CoercionAction.TryConvert);
    }

    private final ObjectMapper MAPPER_TO_NULL; {
        MAPPER_TO_NULL = newJsonMapper();
        MAPPER_TO_NULL.coercionConfigFor(LogicalType.Integer)
            .setCoercion(CoercionInputShape.Float, CoercionAction.AsNull);
    }

    private final ObjectMapper MAPPER_TO_FAIL; {
        MAPPER_TO_FAIL = newJsonMapper();
        MAPPER_TO_FAIL.coercionConfigFor(LogicalType.Integer)
            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
    }

    /*
    /********************************************************
    /* Test methods, defaults (legacy)
    /********************************************************
     */
    
    public void testLegacyDoubleToIntCoercion() throws Exception
    {
        // by default, should be ok
        Integer I = DEFAULT_MAPPER.readValue(" 1.25 ", Integer.class);
        assertEquals(1, I.intValue());
        {
            IntWrapper w = DEFAULT_MAPPER.readValue("{\"i\":-2.25 }", IntWrapper.class);
            assertEquals(-2, w.i);
            int[] arr = DEFAULT_MAPPER.readValue("[ 1.25 ]", int[].class);
            assertEquals(1, arr[0]);
        }

        Long L = DEFAULT_MAPPER.readValue(" 3.33 ", Long.class);
        assertEquals(3L, L.longValue());
        {
            LongWrapper w = DEFAULT_MAPPER.readValue("{\"l\":-2.25 }", LongWrapper.class);
            assertEquals(-2L, w.l);
            long[] arr = DEFAULT_MAPPER.readValue("[ 1.25 ]", long[].class);
            assertEquals(1, arr[0]);
        }

        Short S = DEFAULT_MAPPER.readValue("42.33", Short.class);
        assertEquals(42, S.intValue());

        BigInteger biggie = DEFAULT_MAPPER.readValue("95.3", BigInteger.class);
        assertEquals(95L, biggie.longValue());
    }

    public void testLegacyFailDoubleToInt() throws Exception
    {
        _verifyCoerceFail(READER_LEGACY_FAIL, Integer.class, "1.5", "java.lang.Integer");
        _verifyCoerceFail(READER_LEGACY_FAIL, Integer.TYPE, "1.5", "int");
        _verifyCoerceFail(READER_LEGACY_FAIL, IntWrapper.class, "{\"i\":-2.25 }", "int");
        _verifyCoerceFail(READER_LEGACY_FAIL, int[].class, "[ 2.5 ]", "element of `int[]`");
    }

    public void testLegacyFailDoubleToLong() throws Exception
    {
        _verifyCoerceFail(READER_LEGACY_FAIL, Long.class, "0.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, Long.TYPE, "-2.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, LongWrapper.class, "{\"l\": 7.7 }");
        _verifyCoerceFail(READER_LEGACY_FAIL, long[].class, "[ -1.35 ]", "element of `long[]`");
    }

    public void testLegacyFailDoubleToOther() throws Exception
    {
        _verifyCoerceFail(READER_LEGACY_FAIL, Short.class, "0.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, Short.TYPE, "-2.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, short[].class, "[ -1.35 ]", "element of `short[]`");

        _verifyCoerceFail(READER_LEGACY_FAIL, Byte.class, "0.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, Byte.TYPE, "-2.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, byte[].class, "[ -1.35 ]", "element of `byte[]`");

        _verifyCoerceFail(READER_LEGACY_FAIL, BigInteger.class, "25236.256");

        // 13-Jun-2020, tatu: No explicit deserializer for `AtomicLong` yet
//        _verifyCoerceFail(READER_LEGACY_FAIL, AtomicLong.class, "25236.256");
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, to null
    /********************************************************
     */

    public void testCoerceConfigFloatToNull() throws Exception
    {
        assertNull(MAPPER_TO_NULL.readValue("1.5", Integer.class));
        // `null` not possible for primitives, must use empty (aka default) value
        assertEquals(Integer.valueOf(0), MAPPER_TO_NULL.readValue("1.5", Integer.TYPE));
        {
            IntWrapper w = MAPPER_TO_NULL.readValue( "{\"i\":-2.25 }", IntWrapper.class);
            assertEquals(0, w.i);
            int[] ints = MAPPER_TO_NULL.readValue("[ 2.5 ]", int[].class);
            assertEquals(1, ints.length);
            assertEquals(0, ints[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue("2.5", Long.class));
        assertEquals(Long.valueOf(0L), MAPPER_TO_NULL.readValue("-4.25", Long.TYPE));
        {
            LongWrapper w = MAPPER_TO_NULL.readValue( "{\"l\":-2.25 }", LongWrapper.class);
            assertEquals(0L, w.l);
            long[] l = MAPPER_TO_NULL.readValue("[ 2.5 ]", long[].class);
            assertEquals(1, l.length);
            assertEquals(0L, l[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue("2.5", Short.class));
        assertEquals(Short.valueOf((short) 0), MAPPER_TO_NULL.readValue("-4.25", Short.TYPE));
        {
            short[] s = MAPPER_TO_NULL.readValue("[ 2.5 ]", short[].class);
            assertEquals(1, s.length);
            assertEquals((short) 0, s[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue("2.5", Byte.class));
        assertEquals(Byte.valueOf((byte) 0), MAPPER_TO_NULL.readValue("-4.25", Byte.TYPE));
        {
            byte[] arr = MAPPER_TO_NULL.readValue("[ 2.5 ]", byte[].class);
            assertEquals(1, arr.length);
            assertEquals((byte) 0, arr[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue("2.5", BigInteger.class));
        {
            BigInteger[] arr = MAPPER_TO_NULL.readValue("[ 2.5 ]", BigInteger[].class);
            assertEquals(1, arr.length);
            assertNull(arr[0]);
        }
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, to empty
    /********************************************************
     */

    public void testCoerceConfigFloatToEmpty() throws Exception
    {
        assertEquals(Integer.valueOf(0), MAPPER_TO_EMPTY.readValue("1.2", Integer.class));
        assertEquals(Integer.valueOf(0), MAPPER_TO_EMPTY.readValue("1.5", Integer.TYPE));
        {
            IntWrapper w = MAPPER_TO_EMPTY.readValue( "{\"i\":-2.25 }", IntWrapper.class);
            assertEquals(0, w.i);
            int[] ints = MAPPER_TO_EMPTY.readValue("[ 2.5 ]", int[].class);
            assertEquals(1, ints.length);
            assertEquals(0, ints[0]);
        }

        assertEquals(Long.valueOf(0), MAPPER_TO_EMPTY.readValue("1.2", Long.class));
        assertEquals(Long.valueOf(0), MAPPER_TO_EMPTY.readValue("1.5", Long.TYPE));
        {
            LongWrapper w = MAPPER_TO_EMPTY.readValue( "{\"l\":-2.25 }", LongWrapper.class);
            assertEquals(0L, w.l);
            long[] l = MAPPER_TO_EMPTY.readValue("[ 2.5 ]", long[].class);
            assertEquals(1, l.length);
            assertEquals(0L, l[0]);
        }

        assertEquals(Short.valueOf((short)0), MAPPER_TO_EMPTY.readValue("1.2", Short.class));
        assertEquals(Short.valueOf((short) 0), MAPPER_TO_EMPTY.readValue("1.5", Short.TYPE));

        assertEquals(Byte.valueOf((byte)0), MAPPER_TO_EMPTY.readValue("1.2", Byte.class));
        assertEquals(Byte.valueOf((byte) 0), MAPPER_TO_EMPTY.readValue("1.5", Byte.TYPE));

        assertEquals(BigInteger.valueOf(0L), MAPPER_TO_EMPTY.readValue("124.5", BigInteger.class));
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, coerce
    /********************************************************
     */

    public void testCoerceConfigFloatSuccess() throws Exception
    {
        assertEquals(Integer.valueOf(1), MAPPER_TRY_CONVERT.readValue("1.2", Integer.class));
        assertEquals(Integer.valueOf(3), MAPPER_TRY_CONVERT.readValue("3.4", Integer.TYPE));
        {
            IntWrapper w = MAPPER_TRY_CONVERT.readValue( "{\"i\":-2.25 }", IntWrapper.class);
            assertEquals(-2, w.i);
            int[] ints = MAPPER_TRY_CONVERT.readValue("[ 22.10 ]", int[].class);
            assertEquals(1, ints.length);
            assertEquals(22, ints[0]);
        }

        assertEquals(Long.valueOf(1), MAPPER_TRY_CONVERT.readValue("1.2", Long.class));
        assertEquals(Long.valueOf(1), MAPPER_TRY_CONVERT.readValue("1.5", Long.TYPE));
        {
            LongWrapper w = MAPPER_TRY_CONVERT.readValue( "{\"l\":-2.25 }", LongWrapper.class);
            assertEquals(-2L, w.l);
            long[] l = MAPPER_TRY_CONVERT.readValue("[ 2.2 ]", long[].class);
            assertEquals(1, l.length);
            assertEquals(2L, l[0]);
        }

        assertEquals(Short.valueOf((short)1), MAPPER_TRY_CONVERT.readValue("1.2", Short.class));
        assertEquals(Short.valueOf((short) 19), MAPPER_TRY_CONVERT.readValue("19.2", Short.TYPE));

        assertEquals(Byte.valueOf((byte)1), MAPPER_TRY_CONVERT.readValue("1.2", Byte.class));
        assertEquals(Byte.valueOf((byte) 1), MAPPER_TRY_CONVERT.readValue("1.5", Byte.TYPE));

        assertEquals(BigInteger.valueOf(124L), MAPPER_TRY_CONVERT.readValue("124.2", BigInteger.class));
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, fail
    /********************************************************
     */

    public void testCoerceConfigFailFromFloat() throws Exception
    {
        _verifyCoerceFail(MAPPER_TO_FAIL, Integer.class, "1.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, Integer.TYPE, "1.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, IntWrapper.class, "{\"i\":-2.25 }", "int");
        _verifyCoerceFail(MAPPER_TO_FAIL, int[].class, "[ 2.5 ]", "element of `int[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, Long.class, "0.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, Long.TYPE, "-2.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, LongWrapper.class, "{\"l\": 7.7 }");
        _verifyCoerceFail(MAPPER_TO_FAIL, long[].class, "[ -1.35 ]", "element of `long[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, Short.class, "0.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, Short.TYPE, "-2.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, short[].class, "[ -1.35 ]", "element of `short[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, Byte.class, "0.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, Byte.TYPE, "-2.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, byte[].class, "[ -1.35 ]", "element of `byte[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, BigInteger.class, "25236.256");
    }

    /*
    /********************************************************
    /* Helper methods
    /********************************************************
     */

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
            String doc) throws Exception
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetType.getName());
    }

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
            String doc, String targetTypeDesc) throws Exception
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetTypeDesc);
    }

    private void _verifyCoerceFail(ObjectReader r, Class<?> targetType,
            String doc) throws Exception
    {
        _verifyCoerceFail(r, targetType, doc, targetType.getName());
    }

    private void _verifyCoerceFail(ObjectReader r, Class<?> targetType,
            String doc, String targetTypeDesc) throws Exception
    {
        try {
            r.forType(targetType).readValue(doc);
            fail("Should not accept Float for "+targetType.getName()+" by default");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce Floating-point");
            verifyException(e, targetTypeDesc);
        }
    }
}
