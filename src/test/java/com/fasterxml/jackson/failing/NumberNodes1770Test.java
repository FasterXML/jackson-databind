package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;

/**
 * Basic tests for {@link JsonNode} implementations that
 * contain numeric values.
 */
public class NumberNodes1770Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // For to [databind#1770] (broken due to fix for #1028): `JsonNodeDeserializer`
    // would coerce ok but does `parser.isNaN()` check which ends up parsing
    // as Double, gets `POSITIVE_INFINITY` and returns `true`: this results in
    // `DoubleNode` being used even tho `BigDecimal` could fit the number.
    public void testBigDecimalCoercion() throws Exception
    {
        final JsonNode jsonNode = MAPPER.reader()
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .readTree("7976931348623157e309");
        assertTrue("Expected DecimalNode, got: "+jsonNode.getClass().getName()+": "+jsonNode, jsonNode.isBigDecimal());
        // the following fails with NumberFormatException, because jsonNode is a DoubleNode with a value of POSITIVE_INFINITY
//        Assert.assertTrue(jsonNode.decimalValue().compareTo(new BigDecimal("7976931348623157e309")) == 0);
    }
}
