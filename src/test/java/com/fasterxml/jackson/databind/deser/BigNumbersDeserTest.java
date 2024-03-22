package com.fasterxml.jackson.databind.deser;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil.DoubleWrapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

public class BigNumbersDeserTest
{
    static class BigIntegerWrapper {
        public BigInteger number;
    }

    static class BigDecimalWrapper {
        public BigDecimal number;
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

    @Test
    public void testDouble() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("d"), DoubleWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length", "exceeds the maximum allowed");
        }
    }

    @Test
    public void testDoubleUnlimited() throws Exception
    {
        DoubleWrapper dw =
            newJsonMapperWithUnlimitedNumberSizeSupport().readValue(generateJson("d"), DoubleWrapper.class);
        assertNotNull(dw);
    }

    @Test
    public void testBigDecimal() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigDecimalWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length ", "exceeds the maximum allowed");
        }
    }

    @Test
    public void testBigDecimalUnlimited() throws Exception
    {
        BigDecimalWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigDecimalWrapper.class);
        assertNotNull(bdw);
    }

    @Test
    public void testBigInteger() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigIntegerWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length", "exceeds the maximum allowed");
        }
    }

    @Test
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
