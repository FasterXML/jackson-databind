package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.*;

// for [databind#2885]
public class NumberToIntegralConversionTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();
    private final JsonNodeFactory NODES = MAPPER.getNodeFactory();

    public void testFloatToIntegrals() throws Exception
    {
        assertTrue(NODES.numberNode(0f).canConvertToExactIntegral());
        assertTrue(NODES.numberNode((float) 0).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(0.000f).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(0.001f).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(0.25f).canConvertToExactIntegral());
    }

    public void testDoubleToIntegrals() throws Exception
    {
        assertTrue(NODES.numberNode(0d).canConvertToExactIntegral());
        assertTrue(NODES.numberNode((double) 0).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(0.000d).canConvertToExactIntegral());
        assertTrue(NODES.numberNode((double) Integer.MAX_VALUE).canConvertToExactIntegral());
        assertTrue(NODES.numberNode((double) Integer.MIN_VALUE).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(0.001d).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(0.25d).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(12000000.5d).canConvertToExactIntegral());
    }

    public void testNaNsToIntegrals() throws Exception
    {
        assertFalse(NODES.numberNode(Float.NaN).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Float.NEGATIVE_INFINITY).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Float.POSITIVE_INFINITY).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(Double.NaN).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Double.NEGATIVE_INFINITY).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Double.POSITIVE_INFINITY).canConvertToExactIntegral());
    }

    public void testBigDecimalToIntegrals() throws Exception
    {
        assertTrue(NODES.numberNode(BigDecimal.ZERO).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(BigDecimal.TEN).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(BigDecimal.valueOf(Integer.MAX_VALUE)).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(BigDecimal.valueOf(Integer.MIN_VALUE)).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(BigDecimal.valueOf(Long.MAX_VALUE)).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(BigDecimal.valueOf(Long.MIN_VALUE)).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(BigDecimal.valueOf(0.001)).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(BigDecimal.valueOf(0.25)).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(BigDecimal.valueOf(12000000.5)).canConvertToExactIntegral());
    }
}
