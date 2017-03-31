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
        boolean _v;
        void setV(boolean v) { _v = v; }
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
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        BooleanBean result = mapper.readValue(new StringReader("{\"v\":[true]}"), BooleanBean.class);
        assertTrue(result._v);

        _verifyMultiValueArrayFail("[{\"v\":[true,true]}]", BooleanBean.class);

        result = mapper.readValue("{\"v\":[null]}", BooleanBean.class);
        assertNotNull(result);
        assertFalse(result._v);
        
        result = mapper.readValue("[{\"v\":[null]}]", BooleanBean.class);
        assertNotNull(result);
        assertFalse(result._v);
        
        boolean[] array = mapper.readValue(new StringReader("[ [ null ] ]"), boolean[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertFalse(array[0]);
    }

    /*
    /**********************************************************
    /* Single-element as array tests, numbers
    /**********************************************************
     */
    
    // [databind#381]
    public void testSingleElementScalarArrays() throws Exception {
        final int intTest = 932832;
        final double doubleTest = 32.3234;
        final long longTest = 2374237428374293423L;
        final short shortTest = (short) intTest;
        final float floatTest = 84.3743f;
        final byte byteTest = (byte) 43;
        final char charTest = 'c';

        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        final int intValue = mapper.readValue(asArray(intTest), Integer.TYPE);
        assertEquals(intTest, intValue);
        final Integer integerWrapperValue = mapper.readValue(asArray(Integer.valueOf(intTest)), Integer.class);
        assertEquals(Integer.valueOf(intTest), integerWrapperValue);

        final double doubleValue = mapper.readValue(asArray(doubleTest), Double.class);
        assertEquals(doubleTest, doubleValue);
        final Double doubleWrapperValue = mapper.readValue(asArray(Double.valueOf(doubleTest)), Double.class);
        assertEquals(Double.valueOf(doubleTest), doubleWrapperValue);

        final long longValue = mapper.readValue(asArray(longTest), Long.TYPE);
        assertEquals(longTest, longValue);
        final Long longWrapperValue = mapper.readValue(asArray(Long.valueOf(longTest)), Long.class);
        assertEquals(Long.valueOf(longTest), longWrapperValue);

        final short shortValue = mapper.readValue(asArray(shortTest), Short.TYPE);
        assertEquals(shortTest, shortValue);
        final Short shortWrapperValue = mapper.readValue(asArray(Short.valueOf(shortTest)), Short.class);
        assertEquals(Short.valueOf(shortTest), shortWrapperValue);

        final float floatValue = mapper.readValue(asArray(floatTest), Float.TYPE);
        assertEquals(floatTest, floatValue);
        final Float floatWrapperValue = mapper.readValue(asArray(Float.valueOf(floatTest)), Float.class);
        assertEquals(Float.valueOf(floatTest), floatWrapperValue);

        final byte byteValue = mapper.readValue(asArray(byteTest), Byte.TYPE);
        assertEquals(byteTest, byteValue);
        final Byte byteWrapperValue = mapper.readValue(asArray(Byte.valueOf(byteTest)), Byte.class);
        assertEquals(Byte.valueOf(byteTest), byteWrapperValue);

        final char charValue = mapper.readValue(asArray(quote(String.valueOf(charTest))), Character.TYPE);
        assertEquals(charTest, charValue);
        final Character charWrapperValue = mapper.readValue(asArray(quote(String.valueOf(charTest))), Character.class);
        assertEquals(Character.valueOf(charTest), charWrapperValue);

        final boolean booleanTrueValue = mapper.readValue(asArray(true), Boolean.TYPE);
        assertTrue(booleanTrueValue);

        final boolean booleanFalseValue = mapper.readValue(asArray(false), Boolean.TYPE);
        assertFalse(booleanFalseValue);

        final Boolean booleanWrapperTrueValue = mapper.readValue(asArray(Boolean.valueOf(true)), Boolean.class);
        assertEquals(Boolean.TRUE, booleanWrapperTrueValue);
    }

    public void testSingleElementArrayDisabled() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        try {
            mapper.readValue("[42]", Integer.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42]", Integer.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42342342342342]", Long.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42342342342342342]", Long.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[42]", Short.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42]", Short.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[327.2323]", Float.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[82.81902]", Float.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[22]", Byte.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[22]", Byte.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("['d']", Character.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("['d']", Character.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[true]", Boolean.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[true]", Boolean.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
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
        _verifyMultiValueArrayFail(asArray(quote("c") + ","  + quote("d")), Character.class);
        _verifyMultiValueArrayFail(asArray(quote("c") + ","  + quote("d")), Character.TYPE);
        _verifyMultiValueArrayFail("[true,false]", Boolean.class);
        _verifyMultiValueArrayFail("[true,false]", Boolean.TYPE);
    }

    /*
    /**********************************************************
    /* Simple non-primitive types
    /**********************************************************
     */

    public void testSingleString() throws Exception
    {
        String value = "FOO!";
        String result = MAPPER.readValue("\""+value+"\"", String.class);
        assertEquals(value, result);
    }
    
    public void testSingleStringWrapped() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        
        String value = "FOO!";
        try {
            mapper.readValue("[\""+value+"\"]", String.class);
            fail("Exception not thrown when attempting to unwrap a single value 'String' array into a simple String");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Can not deserialize");
            verifyException(exp, "out of START_ARRAY");
        }
        
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        
        try {
            mapper.readValue("[\""+value+"\",\""+value+"\"]", String.class);
            fail("Exception not thrown when attempting to unwrap a single value 'String' array that contained more than one value into a simple String");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
        String result = mapper.readValue("[\""+value+"\"]", String.class);
        assertEquals(value, result);
    }

    public void testBigDecimal() throws Exception
    {
        final ObjectMapper mapper = objectMapper();
        mapper.disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        
        BigDecimal value = new BigDecimal("0.001");
        BigDecimal result = mapper.readValue(value.toString(), BigDecimal.class);
        assertEquals(value, result);
        try {
            mapper.readValue("[" + value.toString() + "]", BigDecimal.class);
            fail("Exception was not thrown when attempting to read a single value array of BigDecimal when UNWRAP_SINGLE_VALUE_ARRAYS feature is disabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Can not deserialize");
            verifyException(exp, "out of START_ARRAY");
        }
        
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        result = mapper.readValue("[" + value.toString() + "]", BigDecimal.class);
        assertEquals(value, result);
        
        try {
            mapper.readValue("[" + value.toString() + "," + value.toString() + "]", BigDecimal.class);
            fail("Exception was not thrown when attempting to read a muti value array of BigDecimal when UNWRAP_SINGLE_VALUE_ARRAYS feature is enabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
    }

    public void testBigInteger() throws Exception
    {
        final ObjectMapper mapper = objectMapper();
        mapper.disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        
        BigInteger value = new BigInteger("-1234567890123456789012345567809");
        BigInteger result = mapper.readValue(value.toString(), BigInteger.class);
        assertEquals(value, result);

        try {
            mapper.readValue("[" + value.toString() + "]", BigInteger.class);
            fail("Exception was not thrown when attempting to read a single value array of BigInteger when UNWRAP_SINGLE_VALUE_ARRAYS feature is disabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Can not deserialize");
            verifyException(exp, "out of START_ARRAY");
        }
        
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        result = mapper.readValue("[" + value.toString() + "]", BigInteger.class);
        assertEquals(value, result);
        
        try {
            mapper.readValue("[" + value.toString() + "," + value.toString() + "]", BigInteger.class);
            fail("Exception was not thrown when attempting to read a multi-value array of BigInteger when UNWRAP_SINGLE_VALUE_ARRAYS feature is enabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }        
    }

    public void testClassAsArray() throws Exception
    {
        Class<?> result = MAPPER
                    .readerFor(Class.class)
                    .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                    .readValue(quote(String.class.getName()));
        assertEquals(String.class, result);

        try {
            MAPPER.readerFor(Class.class)
                .without(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[" + quote(String.class.getName()) + "]");
            fail("Did not throw exception when UNWRAP_SINGLE_VALUE_ARRAYS feature was disabled and attempted to read a Class array containing one element");
        } catch (MismatchedInputException e) {
            verifyException(e, "out of START_ARRAY token");
        }

        _verifyMultiValueArrayFail("[" + quote(Object.class.getName()) + "," + quote(Object.class.getName()) +"]",
                Class.class);
        result = MAPPER.readerFor(Class.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[" + quote(String.class.getName()) + "]");
        assertEquals(String.class, result);
    }

    public void testURIAsArray() throws Exception
    {
        final ObjectReader reader = MAPPER.readerFor(URI.class);
        final URI value = new URI("http://foo.com");
        try {
            reader.without(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[\""+value.toString()+"\"]");
            fail("Did not throw exception for single value array when UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException e) {
            verifyException(e, "out of START_ARRAY token");
        }
        
        _verifyMultiValueArrayFail("[\""+value.toString()+"\",\""+value.toString()+"\"]", URI.class);
    }

    public void testUUIDAsArray() throws Exception
    {
        final ObjectReader reader = MAPPER.readerFor(UUID.class);
        final String uuidStr = "76e6d183-5f68-4afa-b94a-922c1fdb83f8";
        UUID uuid = UUID.fromString(uuidStr);
        try {
            NO_UNWRAPPING_READER.forType(UUID.class)
                .readValue("[" + quote(uuidStr) + "]");
            fail("Exception was not thrown when UNWRAP_SINGLE_VALUE_ARRAYS is disabled and attempted to read a single value array as a single element");
        } catch (MismatchedInputException e) {
            verifyException(e, "out of START_ARRAY token");
        }
        assertEquals(uuid,
                reader.with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                    .readValue("[" + quote(uuidStr) + "]"));
        _verifyMultiValueArrayFail("[" + quote(uuidStr) + "," + quote(uuidStr) + "]", UUID.class);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyMultiValueArrayFail(String input, Class<?> type) throws IOException {
        try {
            UNWRAPPING_READER.forType(type).readValue(input);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException e) {
            verifyException(e, "Attempted to unwrap");
        }
    }

    private static String asArray(Object value) {
        return "["+value+"]";
    }
}
