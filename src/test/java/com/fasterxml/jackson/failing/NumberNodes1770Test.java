package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;

/**
 * Basic tests for {@link JsonNode} implementations that
 * contain numeric values.
 */
public class NumberNodes1770Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = newObjectMapper();

    // Related to [databind#1770]
    public void testBigDecimalCoercion() throws Exception
    {
        final JsonNode jsonNode = MAPPER.reader()
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .readTree("7976931348623157e309");
        assertTrue(jsonNode.isBigDecimal());
        // the following fails with NumberFormatException, because jsonNode is a DoubleNode with a value of POSITIVE_INFINITY
//        Assert.assertTrue(jsonNode.decimalValue().compareTo(new BigDecimal("7976931348623157e309")) == 0);
    }
}
