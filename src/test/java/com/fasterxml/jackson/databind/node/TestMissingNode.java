package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonToken;

public class TestMissingNode extends NodeTestBase
{
    public void testMissing()
    {
        MissingNode n = MissingNode.getInstance();
        assertTrue(n.isMissingNode());
        assertEquals(JsonToken.NOT_AVAILABLE, n.asToken());
        // as per [JACKSON-775]
        assertEquals("", n.asText());
        assertStandardEquals(n);
        assertEquals("", n.toString());

        /* As of 2.0, MissingNode is considered non-numeric, meaning
         * that default values are served.
         */
        assertNodeNumbersForNonNumeric(n);

        // [JACKSON-823]
        assertTrue(n.asBoolean(true));
        assertEquals(4, n.asInt(4));
        assertEquals(5L, n.asLong(5));
        assertEquals(0.25, n.asDouble(0.25));
    }
}
