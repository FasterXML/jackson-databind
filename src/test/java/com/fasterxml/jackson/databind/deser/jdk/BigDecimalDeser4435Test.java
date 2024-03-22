package com.fasterxml.jackson.databind.deser.jdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

// @since 2.16.3
public class BigDecimalDeser4435Test
{
    static class BigDecimalWrapper {
        public BigDecimal number;
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    public void testNumberStartingWithDot() throws Exception
    {
        String num = ".555555555555555555555555555555";
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(new BigDecimal(num), w.number);
    }

    @Test
    public void testNumberStartingWithMinusDot() throws Exception
    {
        String num = "-.555555555555555555555555555555";
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(new BigDecimal(num), w.number);
    }

    @Test
    public void testNumberStartingWithPlusDot() throws Exception
    {
        String num = "+.555555555555555555555555555555";
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(new BigDecimal(num), w.number);
    }
}
