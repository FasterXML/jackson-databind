package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.*;

public class NotANumberConversionTest extends BaseMapTest
{
    private final ObjectMapper m = new ObjectMapper();
    {
        m.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    public void testBigDecimalWithNaN() throws Exception
    {
        JsonNode tree = m.valueToTree(new DoubleWrapper(Double.NaN));
        assertNotNull(tree);
        String json = m.writeValueAsString(tree);
        assertNotNull(json);

        tree = m.valueToTree(new DoubleWrapper(Double.NEGATIVE_INFINITY));
        assertNotNull(tree);
        json = m.writeValueAsString(tree);
        assertNotNull(json);

        tree = m.valueToTree(new DoubleWrapper(Double.POSITIVE_INFINITY));
        assertNotNull(tree);
        json = m.writeValueAsString(tree);
        assertNotNull(json);
    }

    // for [databind#1315]: no accidental coercion to DoubleNode
    public void testBigDecimalWithoutNaN() throws Exception
    {
        BigDecimal input = new BigDecimal(Double.MIN_VALUE).divide(new BigDecimal(10L));
        JsonNode tree = m.readTree(input.toString());
        assertTrue(tree.isBigDecimal());
        BigDecimal output = tree.decimalValue();
        assertEquals(input, output);
    }
}
