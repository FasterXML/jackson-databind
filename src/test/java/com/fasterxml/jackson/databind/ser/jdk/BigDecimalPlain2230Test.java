package com.fasterxml.jackson.databind.ser.jdk;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.StreamWriteFeature;

import com.fasterxml.jackson.databind.*;

public class BigDecimalPlain2230Test extends BaseMapTest
{
    static class BigDecimalAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public BigDecimal value;

        public BigDecimalAsString() { this(BigDecimal.valueOf(0.25)); }
        public BigDecimalAsString(BigDecimal v) { value = v; }
    }

    private final ObjectMapper MAPPER = objectMapper();

    public void testBigIntegerAsPlainTest() throws Exception
    {
        final String NORM_VALUE = "0.0000000005";
        final BigDecimal BD_VALUE = new BigDecimal(NORM_VALUE);
        final BigDecimalAsString INPUT = new BigDecimalAsString(BD_VALUE);
        // by default, use the default `toString()`
        assertEquals("{\"value\":\""+BD_VALUE.toString()+"\"}", MAPPER.writeValueAsString(INPUT));

        // but can force to "plain" notation
        final ObjectMapper m = jsonMapperBuilder()
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();
        assertEquals("{\"value\":\""+NORM_VALUE+"\"}", m.writeValueAsString(INPUT));
    }
}
