package com.fasterxml.jackson.databind.convert;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class CoerceFloatToIntTest extends BaseMapTest
{
    private final ObjectMapper DEFAULT_MAPPER = sharedMapper();
    private final ObjectReader READER_LEGACY_FAIL = DEFAULT_MAPPER.reader()
            .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

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
        _verifyCoerceFail(READER_LEGACY_FAIL, Integer.class, "1.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, Integer.TYPE, "1.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, IntWrapper.class, "{\"i\":-2.25 }");
        _verifyCoerceFail(READER_LEGACY_FAIL, int[].class, "[ 2.5 ]");

        _verifyCoerceFail(READER_LEGACY_FAIL, Long.class, "0.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, Long.TYPE, "-2.5");
        _verifyCoerceFail(READER_LEGACY_FAIL, LongWrapper.class, "{\"l\": 7.7 }");
        _verifyCoerceFail(READER_LEGACY_FAIL, long[].class, "[ -1.35 ]");

        _verifyCoerceFailShort(READER_LEGACY_FAIL, Short.class, "0.5");
        _verifyCoerceFailShort(READER_LEGACY_FAIL, Short.TYPE, "-2.5");
        _verifyCoerceFailShort(READER_LEGACY_FAIL, short[].class, "[ -1.35 ]");

        _verifyCoerceFail(READER_LEGACY_FAIL, BigInteger.class, "25236.256");
    }

    private void _verifyCoerceFail(ObjectReader r, Class<?> targetType,
            String doc) throws Exception
    {
        try {
            r.forType(targetType).readValue(doc);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce a floating-point",
                    "Cannot coerce Floating-point");
        }
    }

    private void _verifyCoerceFailShort(ObjectReader r, Class<?> targetType,
            String doc) throws Exception
    {
        try {
            r.forType(targetType).readValue(doc);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e,
                    "Cannot deserialize value of type `short` from Floating-point value",
                    "Cannot coerce Floating-point");
        }
    }
    
    public void testDoubleToLong() throws Exception
    {

    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig
    /********************************************************
     */

}
