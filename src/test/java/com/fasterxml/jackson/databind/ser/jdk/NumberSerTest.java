package com.fasterxml.jackson.databind.ser.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class NumberSerTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    static class IntWrapper {
        public int i;

        public IntWrapper(int value) { i = value; }
    }
    
    static class IntAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        @JsonProperty("value")
        public int foo = 3;
    }

    static class LongAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public long value = 4;
    }

    static class DoubleAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public double value = -0.5;
    }

    static class NumberWrapper {
        // ensure it will use `Number` as statically force type, when looking for serializer
        @JsonSerialize(as=Number.class)
        public Number value;

        public NumberWrapper(Number v) { value = v; }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testDouble() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double d : values) {
            String expected = String.valueOf(d);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                expected = "\""+d+"\"";
            }
            assertEquals(expected, MAPPER.writeValueAsString(Double.valueOf(d)));
        }
    }

    public void testBigInteger() throws Exception
    {
        BigInteger[] values = new BigInteger[] {
                BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO,
                BigInteger.valueOf(1234567890L),
                new BigInteger("123456789012345678901234568"),
                new BigInteger("-1250000124326904597090347547457")
                };

        for (BigInteger value : values) {
            String expected = value.toString();
            assertEquals(expected, MAPPER.writeValueAsString(value));
        }
    }

    public void testNumbersAsString() throws Exception
    {
        assertEquals(aposToQuotes("{'value':'3'}"), MAPPER.writeValueAsString(new IntAsString()));
        assertEquals(aposToQuotes("{'value':'4'}"), MAPPER.writeValueAsString(new LongAsString()));
        assertEquals(aposToQuotes("{'value':'-0.5'}"), MAPPER.writeValueAsString(new DoubleAsString()));
    }

    public void testConfigOverridesForNumbers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Integer.TYPE) // for `int`
            .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        assertEquals(aposToQuotes("{'i':'3'}"),
                mapper.writeValueAsString(new IntWrapper(3)));
    }

    public void testNumberType() throws Exception
    {
        assertEquals(aposToQuotes("{'value':1}"), MAPPER.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 1))));
        assertEquals(aposToQuotes("{'value':2}"), MAPPER.writeValueAsString(new NumberWrapper(Short.valueOf((short) 2))));
        assertEquals(aposToQuotes("{'value':3}"), MAPPER.writeValueAsString(new NumberWrapper(Integer.valueOf(3))));
        assertEquals(aposToQuotes("{'value':4}"), MAPPER.writeValueAsString(new NumberWrapper(Long.valueOf(4L))));
        assertEquals(aposToQuotes("{'value':0.5}"), MAPPER.writeValueAsString(new NumberWrapper(Float.valueOf(0.5f))));
        assertEquals(aposToQuotes("{'value':0.05}"), MAPPER.writeValueAsString(new NumberWrapper(Double.valueOf(0.05))));
        assertEquals(aposToQuotes("{'value':123}"), MAPPER.writeValueAsString(new NumberWrapper(BigInteger.valueOf(123))));
        assertEquals(aposToQuotes("{'value':0.025}"), MAPPER.writeValueAsString(new NumberWrapper(BigDecimal.valueOf(0.025))));
    }
}
