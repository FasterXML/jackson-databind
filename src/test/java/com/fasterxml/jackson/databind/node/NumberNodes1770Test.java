package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

// Tests for [databind#1770]
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

    public void testBigDecimalCoercionInf() throws Exception
    {
        final String value = "+INF";
        JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        final JsonNode jsonNode = new JsonMapper(factory).reader()
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(value);
        assertTrue("Expected DoubleNode, got: "+jsonNode.getClass().getName()+": "+jsonNode, jsonNode.isDouble());
        assertEquals(Double.POSITIVE_INFINITY, jsonNode.doubleValue());
    }
}
