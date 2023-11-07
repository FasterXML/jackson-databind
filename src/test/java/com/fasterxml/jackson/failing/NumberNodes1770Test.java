package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;

import java.math.BigDecimal;

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
        final String value = "7976931348623157e309";
        final JsonNode jsonNode = MAPPER.reader()
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .readTree(value);
        assertTrue("Expected DecimalNode, got: "+jsonNode.getClass().getName()+": "+jsonNode, jsonNode.isBigDecimal());
        assertEquals(new BigDecimal(value), jsonNode.decimalValue());
    }
}
