package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotANumberConversionTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    @Test
    public void testBigDecimalWithNaN() throws Exception
    {
        JsonNode tree = MAPPER.valueToTree(new DoubleWrapper(Double.NaN));
        assertNotNull(tree);
        String json = MAPPER.writeValueAsString(tree);
        assertNotNull(json);

        tree = MAPPER.valueToTree(new DoubleWrapper(Double.NEGATIVE_INFINITY));
        assertNotNull(tree);
        json = MAPPER.writeValueAsString(tree);
        assertNotNull(json);

        tree = MAPPER.valueToTree(new DoubleWrapper(Double.POSITIVE_INFINITY));
        assertNotNull(tree);
        json = MAPPER.writeValueAsString(tree);
        assertNotNull(json);
    }

    // for [databind#1315]: no accidental coercion to DoubleNode
    @Test
    public void testBigDecimalWithoutNaN() throws Exception
    {
        BigDecimal input = new BigDecimal(Double.MIN_VALUE).divide(new BigDecimal(10L));
        JsonNode tree = MAPPER.readTree(input.toString());
        assertTrue(tree.isBigDecimal());
        BigDecimal output = tree.decimalValue();
        assertEquals(input, output);
    }
}
