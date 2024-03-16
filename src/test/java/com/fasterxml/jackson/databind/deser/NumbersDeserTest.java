package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumbersDeserTest
{
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

}
