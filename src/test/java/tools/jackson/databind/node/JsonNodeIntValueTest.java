package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.testutil.DatabindTestUtil;

/**
 * Tests for [databind#4958], JsonNode.intValue() (and related) parts
 * over all types
 */
public class JsonNodeIntValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // intValue() + Numbers

    @Test
    public void testIntValueFromNumberOk() {
        
    }

    @Test
    public void testIntValueFromNumberFail() {
        
    }

    // // // intValue() + Scalars

    // // // intValue() + Structural

    // // // intValue() + Misc other
}
