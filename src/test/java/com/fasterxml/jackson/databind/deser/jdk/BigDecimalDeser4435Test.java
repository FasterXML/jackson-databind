package com.fasterxml.jackson.databind.deser.jdk;

import java.math.BigDecimal;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

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

    // Disabled until [databind#4435 fixed]
    @Disabled
    @Test
    public void testNumberStartingWithDot() throws Exception
    {
        String num = ".555555555555555555555555555555";
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(new BigDecimal(num), w.number);
    }

    // Disabled until [databind#4435 fixed]
    @Disabled
    @Test
    public void testNumberStartingWithMinusDot() throws Exception
    {
        String num = "-.555555555555555555555555555555";
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(new BigDecimal(num), w.number);
    }

    // Disabled until [databind#4435 fixed]
    @Disabled
    @Test
    public void testNumberStartingWithPlusDot() throws Exception
    {
        String num = "+.555555555555555555555555555555";
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(new BigDecimal(num), w.number);
    }
}
