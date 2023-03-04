package com.fasterxml.jackson.databind.deser;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class TestBigNumbers extends BaseMapTest
{
    static class BigDecimalWrapper {
        BigDecimal number;

        public BigDecimalWrapper() {}

        public BigDecimalWrapper(BigDecimal number) {
            this.number = number;
        }

        public void setNumber(BigDecimal number) {
            this.number = number;
        }
    }

    static class BigIntegerWrapper {
        BigInteger number;

        public BigIntegerWrapper() {}

        public BigIntegerWrapper(BigInteger number) {
            this.number = number;
        }

        public void setNumber(BigInteger number) {
            this.number = number;
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private ObjectMapper newJsonMapperWithUnlimitedNumberSizeSupport() {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        return JsonMapper.builder(jsonFactory).build();
    }

    public void testDouble() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("d"), DoubleWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Invalid numeric value ", "exceeds the maximum length");
        }
    }

    public void testDoubleUnlimited() throws Exception
    {
        DoubleWrapper dw =
            newJsonMapperWithUnlimitedNumberSizeSupport().readValue(generateJson("d"), DoubleWrapper.class);
        assertNotNull(dw);
    }

    public void testBigDecimal() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigDecimalWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Invalid numeric value ", "exceeds the maximum length");
        }
    }

    public void testBigDecimalUnlimited() throws Exception
    {
        BigDecimalWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigDecimalWrapper.class);
        assertNotNull(bdw);
    }

    public void testBigInteger() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigIntegerWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Invalid numeric value ", "exceeds the maximum length");
        }
    }

    public void testBigIntegerUnlimited() throws Exception
    {
        BigIntegerWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigIntegerWrapper.class);
        assertNotNull(bdw);
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
