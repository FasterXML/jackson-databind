package com.fasterxml.jackson.databind.node;

public class TextNodeTest extends NodeTestBase
{
    public void testText()
    {
        assertNull(TextNode.valueOf(null));
        TextNode empty = TextNode.valueOf("");
        assertStandardEquals(empty);
        assertSame(TextNode.EMPTY_STRING_NODE, empty);

        assertNodeNumbers(TextNode.valueOf("-3"), -3, -3.0);
        assertNodeNumbers(TextNode.valueOf("17.75"), 17, 17.75);
    
        long value = 127353264013893L;
        TextNode n = TextNode.valueOf(String.valueOf(value));
        assertEquals(value, n.asLong());
        
        // and then with non-numeric input
        n = TextNode.valueOf("foobar");
        assertNodeNumbersForNonNumeric(n);

        assertEquals("foobar", n.asText("barf"));
        assertEquals("", empty.asText("xyz"));

        assertTrue(TextNode.valueOf("true").asBoolean(true));
        assertTrue(TextNode.valueOf("true").asBoolean(false));
        assertFalse(TextNode.valueOf("false").asBoolean(true));
        assertFalse(TextNode.valueOf("false").asBoolean(false));
    }
}
