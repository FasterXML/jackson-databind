package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.*;

public class NotANumberConversionTest extends BaseMapTest
{
    public void testBigDecimalWithNaN() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

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
}
