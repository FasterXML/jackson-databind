package com.fasterxml.jackson.databind.deser.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class BigNumbersDeserTest
        extends DatabindTestUtil
{
    static class BigIntegerWrapper {
        public BigInteger number;
    }

    static class BigDecimalWrapper {
        public BigDecimal number;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private ObjectMapper newJsonMapperWithUnlimitedNumberSizeSupport() {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        return JsonMapper.builder(jsonFactory).build();
    }

    @Test
    void testDouble() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("d"), DoubleWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length", "exceeds the maximum allowed");
        }
    }

    @Test
    void testDoubleUnlimited() throws Exception
    {
        DoubleWrapper dw =
            newJsonMapperWithUnlimitedNumberSizeSupport().readValue(generateJson("d"), DoubleWrapper.class);
        assertNotNull(dw);
    }

    @Test
    void testBigDecimal() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigDecimalWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length ", "exceeds the maximum allowed");
        }
    }

    @Test
    void testBigDecimalUnlimited() throws Exception
    {
        BigDecimalWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigDecimalWrapper.class);
        assertNotNull(bdw);
    }

    @Test
    void testBigInteger() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigIntegerWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length", "exceeds the maximum allowed");
        }
    }

    @Test
    void testBigIntegerUnlimited() throws Exception
    {
        BigIntegerWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigIntegerWrapper.class);
        assertNotNull(bdw);
    }

    // [databind#4435]
    @Test
    void testNumberStartingWithDot() throws Exception {
        _testNumberWith(".555555555555555555555555555555");
        _testNumberWith("-.555555555555555555555555555555");
        _testNumberWith("+.555555555555555555555555555555");
    }

    // [databind#4577]
    @Test
    void testNumberEndingWithDot() throws Exception {
        _testNumberWith("55.");
        _testNumberWith("-55.");
        _testNumberWith("+55.");
    }
    
    private void _testNumberWith(String num) throws Exception
    {
        BigDecimal exp = new BigDecimal(num);
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(exp, w.number);
    }

    private String generateJson(final String fieldName) {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"")
                .append(fieldName)
                .append("\": ");
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        sb.append("}");
        return sb.toString();
    }
}
