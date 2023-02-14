package com.fasterxml.jackson.databind.struct;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.UUID;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class UnwrapSingleArrayScalarsTest extends BaseMapTest
{
    static class BooleanBean {
        public boolean v;
    }

    static class IntBean {
        public int v;
    }

    static class LongBean {
        public long v;
    }

    static class DoubleBean {
        public double v;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    private final ObjectReader NO_UNWRAPPING_READER = MAPPER.reader();
    private final ObjectReader UNWRAPPING_READER = MAPPER.reader()
            .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

    /*
    /**********************************************************
    /* Tests for boolean
    /**********************************************************
     */

    public void testBooleanPrimitiveArrayUnwrap() throws Exception
    {
        // [databind#381]
        final ObjectReader r = UNWRAPPING_READER.forType(BooleanBean.class);
        BooleanBean result = r.readValue(new StringReader("{\"v\":[true]}"));
        assertTrue(result.v);

        _verifyMultiValueArrayFail("[{\"v\":[true,true]}]", BooleanBean.class);

        result = r.readValue("{\"v\":[null]}");
        assertNotNull(result);
        assertFalse(result.v);

        result = r.readValue("[{\"v\":[null]}]");
        assertNotNull(result);
        assertFalse(result.v);

        boolean[] array = UNWRAPPING_READER.forType(boolean[].class)
                .readValue(new StringReader("[ [ null ] ]"));
        assertNotNull(array);
        assertEquals(1, array.length);
        assertFalse(array[0]);
    }

    /*
    /**********************************************************
    /* Single-element as array tests, numbers
    /**********************************************************
     */

    public void testIntPrimitiveArrayUnwrap() throws Exception
    {
        try {
            NO_UNWRAPPING_READER.forType(IntBean.class)
                .readValue("{\"v\":[3]}");
            fail("Did not throw exception when reading a value from a single value array with the UNWRAP_SINGLE_VALUE_ARRAYS feature disabled");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `int`");
        }

        ObjectReader r = UNWRAPPING_READER.forType(IntBean.class);
        IntBean result = r.readValue("{\"v\":[3]}");
        assertEquals(3, result.v);

        result = r.readValue("[{\"v\":[3]}]");
        assertEquals(3, result.v);

        try {
            r.readValue("[{\"v\":[3,3]}]");
            fail("Did not throw exception while reading a value from a multi value array with UNWRAP_SINGLE_VALUE_ARRAY feature enabled");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }

        result = r.readValue("{\"v\":[null]}");
        assertNotNull(result);
        assertEquals(0, result.v);

        int[] array = UNWRAPPING_READER.forType(int[].class).readValue("[ [ null ] ]");
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(0, array[0]);

    }

    public void testLongPrimitiveArrayUnwrap() throws Exception
    {
        final ObjectReader unwrapR = UNWRAPPING_READER.forType(LongBean.class);
        final ObjectReader noUnwrapR = NO_UNWRAPPING_READER.forType(LongBean.class);

        try {
            noUnwrapR.readValue("{\"v\":[3]}");
            fail("Did not throw exception when reading a value from a single value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `long`");
        }

        LongBean result = unwrapR.readValue("{\"v\":[3]}");
        assertEquals(3, result.v);

        result = unwrapR.readValue("[{\"v\":[3]}]");
        assertEquals(3, result.v);

        try {
            unwrapR.readValue("[{\"v\":[3,3]}]");
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }

        result = unwrapR.readValue("{\"v\":[null]}");
        assertNotNull(result);
        assertEquals(0, result.v);

        long[] array = unwrapR.forType(long[].class)
                .readValue("[ [ null ] ]");
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(0, array[0]);
    }

    public void testDoubleAsArray() throws Exception
    {
        final ObjectReader unwrapR = UNWRAPPING_READER.forType(DoubleBean.class);
        final ObjectReader noUnwrapR = NO_UNWRAPPING_READER.forType(DoubleBean.class);

        final double value = 0.016;
        try {
            noUnwrapR.readValue("{\"v\":[" + value + "]}");
            fail("Did not throw exception when reading a value from a single value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `double`");
        }

        DoubleBean result = unwrapR.readValue("{\"v\":[" + value + "]}");
        assertEquals(value, result.v);

        result = unwrapR.readValue("[{\"v\":[" + value + "]}]");
        assertEquals(value, result.v);

        try {
            unwrapR.readValue("[{\"v\":[" + value + "," + value + "]}]");
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }

        result = unwrapR.readValue("{\"v\":[null]}");
        assertNotNull(result);
        assertEquals(0d, result.v);

        double[] array = unwrapR.forType(double[].class)
                .readValue("[ [ null ] ]");
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(0d, array[0]);
    }

    public void testSingleElementScalarArrays() throws Exception {
        final int intTest = 932832;
        final double doubleTest = 32.3234;
        final long longTest = 2374237428374293423L;
        final short shortTest = (short) intTest;
        final float floatTest = 84.3743f;
        final byte byteTest = (byte) 43;
        final char charTest = 'c';

        final int intValue = UNWRAPPING_READER.readValue(asArray(intTest), Integer.TYPE);
        assertEquals(intTest, intValue);
        final Integer integerWrapperValue = UNWRAPPING_READER.readValue(asArray(Integer.valueOf(intTest)), Integer.class);
        assertEquals(Integer.valueOf(intTest), integerWrapperValue);

        final double doubleValue = UNWRAPPING_READER.readValue(asArray(doubleTest), Double.class);
        assertEquals(doubleTest, doubleValue);
        final Double doubleWrapperValue = UNWRAPPING_READER.readValue(asArray(Double.valueOf(doubleTest)), Double.class);
        assertEquals(Double.valueOf(doubleTest), doubleWrapperValue);

        final long longValue = UNWRAPPING_READER.readValue(asArray(longTest), Long.TYPE);
        assertEquals(longTest, longValue);
        final Long longWrapperValue = UNWRAPPING_READER.readValue(asArray(Long.valueOf(longTest)), Long.class);
        assertEquals(Long.valueOf(longTest), longWrapperValue);

        final short shortValue = UNWRAPPING_READER.readValue(asArray(shortTest), Short.TYPE);
        assertEquals(shortTest, shortValue);
        final Short shortWrapperValue = UNWRAPPING_READER.readValue(asArray(Short.valueOf(shortTest)), Short.class);
        assertEquals(Short.valueOf(shortTest), shortWrapperValue);

        final float floatValue = UNWRAPPING_READER.readValue(asArray(floatTest), Float.TYPE);
        assertEquals(floatTest, floatValue);
        final Float floatWrapperValue = UNWRAPPING_READER.readValue(asArray(Float.valueOf(floatTest)), Float.class);
        assertEquals(Float.valueOf(floatTest), floatWrapperValue);

        final byte byteValue = UNWRAPPING_READER.readValue(asArray(byteTest), Byte.TYPE);
        assertEquals(byteTest, byteValue);
        final Byte byteWrapperValue = UNWRAPPING_READER.readValue(asArray(Byte.valueOf(byteTest)), Byte.class);
        assertEquals(Byte.valueOf(byteTest), byteWrapperValue);

        final char charValue = UNWRAPPING_READER.readValue(asArray(q(String.valueOf(charTest))), Character.TYPE);
        assertEquals(charTest, charValue);
        final Character charWrapperValue = UNWRAPPING_READER.readValue(asArray(q(String.valueOf(charTest))), Character.class);
        assertEquals(Character.valueOf(charTest), charWrapperValue);

        final boolean booleanTrueValue = UNWRAPPING_READER.readValue(asArray(true), Boolean.TYPE);
        assertTrue(booleanTrueValue);

        final boolean booleanFalseValue = UNWRAPPING_READER.readValue(asArray(false), Boolean.TYPE);
        assertFalse(booleanFalseValue);

        final Boolean booleanWrapperTrueValue = UNWRAPPING_READER.readValue(asArray(Boolean.valueOf(true)), Boolean.class);
        assertEquals(Boolean.TRUE, booleanWrapperTrueValue);
    }

    public void testSingleElementArrayDisabled() throws Exception {
        try {
            NO_UNWRAPPING_READER.readValue("[42]", Integer.class);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("[42]", Integer.TYPE);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("[42342342342342]", Long.class);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("[42342342342342342]", Long.TYPE);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            NO_UNWRAPPING_READER.readValue("[42]", Short.class);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("[42]", Short.TYPE);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            NO_UNWRAPPING_READER.readValue("[327.2323]", Float.class);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("[82.81902]", Float.TYPE);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            NO_UNWRAPPING_READER.readValue("[22]", Byte.class);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("[22]", Byte.TYPE);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            NO_UNWRAPPING_READER.readValue("['d']", Character.class);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("['d']", Character.TYPE);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            NO_UNWRAPPING_READER.readValue("[true]", Boolean.class);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            NO_UNWRAPPING_READER.readValue("[true]", Boolean.TYPE);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
    }

    public void testMultiValueArrayException() throws IOException {
        _verifyMultiValueArrayFail("[42,42]", Integer.class);
        _verifyMultiValueArrayFail("[42,42]", Integer.TYPE);
        _verifyMultiValueArrayFail("[42342342342342,42342342342342]", Long.class);
        _verifyMultiValueArrayFail("[42342342342342342,42342342342342]", Long.TYPE);
        _verifyMultiValueArrayFail("[42,42]", Short.class);
        _verifyMultiValueArrayFail("[42,42]", Short.TYPE);
        _verifyMultiValueArrayFail("[22,23]", Byte.class);
        _verifyMultiValueArrayFail("[22,23]", Byte.TYPE);
        _verifyMultiValueArrayFail("[327.2323,327.2323]", Float.class);
        _verifyMultiValueArrayFail("[82.81902,327.2323]", Float.TYPE);
        _verifyMultiValueArrayFail("[42.273,42.273]", Double.class);
        _verifyMultiValueArrayFail("[42.2723,42.273]", Double.TYPE);
        _verifyMultiValueArrayFail(asArray(q("c") + ","  + q("d")), Character.class);
        _verifyMultiValueArrayFail(asArray(q("c") + ","  + q("d")), Character.TYPE);
        _verifyMultiValueArrayFail("[true,false]", Boolean.class);
        _verifyMultiValueArrayFail("[true,false]", Boolean.TYPE);
    }

    /*
    /**********************************************************
    /* Simple non-primitive types
    /**********************************************************
     */

    public void testSingleStringWrapped() throws Exception
    {
        String value = "FOO!";
        try {
            NO_UNWRAPPING_READER.readValue("[\""+value+"\"]", String.class);
            fail("Exception not thrown when attempting to unwrap a single value 'String' array into a simple String");
        } catch (MismatchedInputException exp) {
            _verifyNoDeserFromArray(exp);
        }

        try {
            UNWRAPPING_READER.forType(String.class)
                .readValue("[\""+value+"\",\""+value+"\"]");
            fail("Exception not thrown when attempting to unwrap a single value 'String' array that contained more than one value into a simple String");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
        String result = UNWRAPPING_READER.forType(String.class)
                .readValue("[\""+value+"\"]");
        assertEquals(value, result);
    }

    public void testBigDecimal() throws Exception
    {
        BigDecimal value = new BigDecimal("0.001");
        ObjectReader r = NO_UNWRAPPING_READER.forType(BigDecimal.class);
        BigDecimal result = r.readValue(value.toString());
        assertEquals(value, result);
        try {
            r.readValue("[" + value.toString() + "]");
            fail("Exception was not thrown when attempting to read a single value array of BigDecimal when UNWRAP_SINGLE_VALUE_ARRAYS feature is disabled");
        } catch (MismatchedInputException exp) {
            _verifyNoDeserFromArray(exp);
        }

        r = UNWRAPPING_READER.forType(BigDecimal.class);
        result = r.readValue("[" + value.toString() + "]");
        assertEquals(value, result);

        try {
            r.readValue("[" + value.toString() + "," + value.toString() + "]");
            fail("Exception was not thrown when attempting to read a muti value array of BigDecimal when UNWRAP_SINGLE_VALUE_ARRAYS feature is enabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
    }

    public void testBigInteger() throws Exception
    {
        BigInteger value = new BigInteger("-1234567890123456789012345567809");
        BigInteger result = NO_UNWRAPPING_READER.readValue(value.toString(), BigInteger.class);
        assertEquals(value, result);

        try {
            NO_UNWRAPPING_READER.readValue("[" + value.toString() + "]", BigInteger.class);
            fail("Exception was not thrown when attempting to read a single value array of BigInteger when UNWRAP_SINGLE_VALUE_ARRAYS feature is disabled");
        } catch (MismatchedInputException exp) {
            _verifyNoDeserFromArray(exp);
        }

        result = UNWRAPPING_READER.readValue("[" + value.toString() + "]", BigInteger.class);
        assertEquals(value, result);

        try {
            UNWRAPPING_READER.readValue("[" + value.toString() + "," + value.toString() + "]", BigInteger.class);
            fail("Exception was not thrown when attempting to read a multi-value array of BigInteger when UNWRAP_SINGLE_VALUE_ARRAYS feature is enabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
    }

    public void testClassAsArray() throws Exception
    {
        Class<?> result = UNWRAPPING_READER
                    .forType(Class.class)
                    .readValue(q(String.class.getName()));
        assertEquals(String.class, result);

        try {
            NO_UNWRAPPING_READER.forType(Class.class)
                .readValue("[" + q(String.class.getName()) + "]");
            fail("Did not throw exception when UNWRAP_SINGLE_VALUE_ARRAYS feature was disabled and attempted to read a Class array containing one element");
        } catch (MismatchedInputException e) {
            _verifyNoDeserFromArray(e);
        }

        _verifyMultiValueArrayFail("[" + q(Object.class.getName()) + "," + q(Object.class.getName()) +"]",
                Class.class);
        result = UNWRAPPING_READER.forType(Class.class)
                .readValue("[" + q(String.class.getName()) + "]");
        assertEquals(String.class, result);
    }

    public void testURIAsArray() throws Exception
    {
        final URI value = new URI("http://foo.com");
        try {
            NO_UNWRAPPING_READER.forType(URI.class)
                .readValue("[\""+value.toString()+"\"]");
            fail("Did not throw exception for single value array when UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException e) {
            _verifyNoDeserFromArray(e);
        }

        _verifyMultiValueArrayFail("[\""+value.toString()+"\",\""+value.toString()+"\"]", URI.class);
    }

    public void testUUIDAsArray() throws Exception
    {
        final String uuidStr = "76e6d183-5f68-4afa-b94a-922c1fdb83f8";
        UUID uuid = UUID.fromString(uuidStr);
        try {
            NO_UNWRAPPING_READER.forType(UUID.class)
                .readValue("[" + q(uuidStr) + "]");
            fail("Exception was not thrown as expected");
        } catch (MismatchedInputException e) {
            _verifyNoDeserFromArray(e);
        }
        assertEquals(uuid,
                UNWRAPPING_READER.forType(UUID.class)
                    .readValue("[" + q(uuidStr) + "]"));
        _verifyMultiValueArrayFail("[" + q(uuidStr) + "," + q(uuidStr) + "]", UUID.class);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyNoDeserFromArray(Exception e) {
        verifyException(e, "Cannot deserialize");
        verifyException(e, "from Array value");
        verifyException(e, "JsonToken.START_ARRAY");
    }

    private void _verifyMultiValueArrayFail(String input, Class<?> type) throws IOException {
        try {
            UNWRAPPING_READER.forType(type).readValue(input);
            fail("Single value array didn't throw an exception");
        } catch (MismatchedInputException e) {
            verifyException(e, "Attempted to unwrap");
        }
    }

    private static String asArray(Object value) {
        return "["+value+"]";
    }
}
