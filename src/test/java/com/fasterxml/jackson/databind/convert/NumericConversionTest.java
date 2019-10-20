package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class NumericConversionTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();
    private final ObjectReader R = MAPPER.reader().without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

    public void testDoubleToInt() throws Exception
    {
        // by default, should be ok
        Integer I = MAPPER.readValue(" 1.25 ", Integer.class);
        assertEquals(1, I.intValue());
        IntWrapper w = MAPPER.readValue("{\"i\":-2.25 }", IntWrapper.class);
        assertEquals(-2, w.i);
        int[] arr = MAPPER.readValue("[ 1.25 ]", int[].class);
        assertEquals(1, arr[0]);

        try {
            R.forType(Integer.class).readValue("1.5");
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }
        try {
            R.forType(Integer.TYPE).readValue("1.5");
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }
        try {
            R.forType(IntWrapper.class).readValue("{\"i\":-2.25 }");
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }
        try {
            R.forType(int[].class).readValue("[ 2.5 ]");
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }
    }

    public void testDoubleToLong() throws Exception
    {
        // by default, should be ok
        Long L = MAPPER.readValue(" 3.33 ", Long.class);
        assertEquals(3L, L.longValue());
        LongWrapper w = MAPPER.readValue("{\"l\":-2.25 }", LongWrapper.class);
        assertEquals(-2L, w.l);
        long[] arr = MAPPER.readValue("[ 1.25 ]", long[].class);
        assertEquals(1, arr[0]);

        try {
            R.forType(Long.class).readValue("1.5");
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }

        try {
            R.forType(Long.TYPE).readValue("1.5");
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }
        
        try {
            R.forType(LongWrapper.class).readValue("{\"l\": 7.7 }");
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }
        try {
            R.forType(long[].class).readValue("[ 2.5 ]");
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce a floating-point");
        }
    }
}
