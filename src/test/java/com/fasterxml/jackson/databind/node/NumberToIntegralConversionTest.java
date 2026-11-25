package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// for [databind#2885]
public class NumberToIntegralConversionTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = sharedMapper();
    private final JsonNodeFactory NODES = MAPPER.getNodeFactory();

    @Test
    public void testFloatToIntegrals() throws Exception
    {
        assertTrue(NODES.numberNode(0f).canConvertToExactIntegral());
        assertTrue(NODES.numberNode((float) 0).canConvertToExactIntegral());
        assertTrue(NODES.numberNode(0.000f).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(0.001f).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(0.25f).canConvertToExactIntegral());
    }

    // [databind#6493]: `float` values outside of `int` range have no fractional
    // part, and should be reported the same way as matching `double` values
    @Test
    public void testLargeFloatToIntegrals() throws Exception
    {
        final float[] integralValues = new float[] {
                2.0e9f, 3.0e9f, -3.0e9f, 1.0e10f, -1.0e10f,
                (float) Long.MAX_VALUE, (float) Long.MIN_VALUE,
                Float.MAX_VALUE, -Float.MAX_VALUE
        };
        for (float f : integralValues) {
            assertTrue(NODES.numberNode(f).canConvertToExactIntegral(),
                    "FloatNode with value "+f+" should be exact integral");
            assertTrue(NODES.numberNode((double) f).canConvertToExactIntegral(),
                    "DoubleNode with value "+((double) f)+" should be exact integral");
        }

        // Largest `float` magnitude that still has a fractional part (2^23 - 0.5)
        assertFalse(NODES.numberNode(8388607.5f).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(-8388607.5f).canConvertToExactIntegral());
    }

    @Test
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

    @Test
    public void testNaNsToIntegrals() throws Exception
    {
        assertFalse(NODES.numberNode(Float.NaN).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Float.NEGATIVE_INFINITY).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Float.POSITIVE_INFINITY).canConvertToExactIntegral());

        assertFalse(NODES.numberNode(Double.NaN).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Double.NEGATIVE_INFINITY).canConvertToExactIntegral());
        assertFalse(NODES.numberNode(Double.POSITIVE_INFINITY).canConvertToExactIntegral());
    }

    @Test
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
