package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [databind#1770], [databind#4194]
public class NumberNodes1770Test extends DatabindTestUtil
{
    // For to [databind#1770] (broken due to fix for #1028): `JsonNodeDeserializer`
    // would coerce ok but does `parser.isNaN()` check which ends up parsing
    // as Double, gets `POSITIVE_INFINITY` and returns `true`: this results in
    // `DoubleNode` being used even tho `BigDecimal` could fit the number.
    @Test
    public void testBigDecimalCoercion() throws Exception
    {
        final String value = "7976931348623157e309";
        final JsonNode jsonNode = newJsonMapper().reader()
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .readTree(value);
        assertTrue(jsonNode.isBigDecimal(), "Expected DecimalNode, got: "+jsonNode.getClass().getName()+": "+jsonNode);
        assertEquals(new BigDecimal(value), jsonNode.decimalValue());
    }

    @Test
    public void testBigDecimalCoercionInf() throws Exception
    {
        final String value = "+INF";
        JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        final JsonNode jsonNode = new JsonMapper(factory).reader()
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(value);
        assertTrue(jsonNode.isDouble(), "Expected DoubleNode, got: "+jsonNode.getClass().getName()+": "+jsonNode);
        assertEquals(Double.POSITIVE_INFINITY, jsonNode.doubleValue());
    }

    // [databind#4194]: should be able to, by configuration, fail coercing NaN to BigDecimal
    @Test
    public void testBigDecimalCoercionNaN() throws Exception
    {
        JsonNode n = _tryBigDecimalCoercionNaNWithOption(false);
        if (!n.isDouble()) {
            fail("Expected DoubleNode, got: "+n.getClass().getName());
        }
        assertEquals(Double.NaN, n.doubleValue());

        try {
            n = _tryBigDecimalCoercionNaNWithOption(true);
            fail("Should not pass without allowing coercion: produced JsonNode of type "
                    +n.getClass().getName());
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot convert NaN");
        }
    }

    private JsonNode _tryBigDecimalCoercionNaNWithOption(boolean isEnabled) throws Exception
    {
        JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        final ObjectReader reader = new JsonMapper(factory)
                .reader()
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        final String value = "NaN";
        // depending on option
        return isEnabled
                ? reader.with(JsonNodeFeature.FAIL_ON_NAN_TO_BIG_DECIMAL_COERCION).readTree(value)
                : reader.without(JsonNodeFeature.FAIL_ON_NAN_TO_BIG_DECIMAL_COERCION).readTree(value);
    }
}
