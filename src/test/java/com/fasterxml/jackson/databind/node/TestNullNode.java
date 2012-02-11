package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestNullNode extends NodeTestBase
{
    public void testBasicsWithNullNode() throws Exception
    {
        // Let's use something that doesn't add much beyond JsonNode base
        NullNode n = NullNode.instance;

        // basic properties
        assertFalse(n.isContainerNode());
        assertFalse(n.isBigDecimal());
        assertFalse(n.isBigInteger());
        assertFalse(n.isBinary());
        assertFalse(n.isBoolean());
        assertFalse(n.isPojo());
        assertFalse(n.isMissingNode());

        // fallback accessors
        assertFalse(n.booleanValue());
        assertNull(n.numberValue());
        assertEquals(0, n.intValue());
        assertEquals(0L, n.longValue());
        assertEquals(BigDecimal.ZERO, n.decimalValue());
        assertEquals(BigInteger.ZERO, n.bigIntegerValue());

        assertEquals(0, n.size());
        assertFalse(n.elements().hasNext());
        assertFalse(n.fieldNames().hasNext());
        // path is never null; but does point to missing node
        assertNotNull(n.path("xyz"));
        assertTrue(n.path("xyz").isMissingNode());

        assertFalse(n.has("field"));
        assertFalse(n.has(3));

        // 1.6:
        assertNodeNumbersForNonNumeric(n);
    }
}
