package com.fasterxml.jackson.databind.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.junit.jupiter.api.Assertions.*;

public class TextNodeTest extends NodeTestBase
{
    @Test
    public void testText()
    {
        assertNull(TextNode.valueOf(null));
        TextNode empty = TextNode.valueOf("");
        assertStandardEquals(empty);
        assertSame(TextNode.EMPTY_STRING_NODE, empty);

        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());

        assertNodeNumbers(TextNode.valueOf("-3"), -3, -3.0);
        assertNodeNumbers(TextNode.valueOf("17.75"), 17, 17.75);

        long value = 127353264013893L;
        TextNode n = TextNode.valueOf(String.valueOf(value));
        assertEquals(value, n.asLong());

        assertFalse(n.isNumber());
        assertFalse(n.canConvertToInt());
        assertFalse(n.canConvertToLong());
        assertFalse(n.canConvertToExactIntegral());

        // and then with non-numeric input
        n = TextNode.valueOf("foobar");
        assertNodeNumbersForNonNumeric(n);

        assertEquals("foobar", n.asText());
        assertEquals("", empty.asText());

        assertTrue(TextNode.valueOf("true").asBoolean(true));
        assertTrue(TextNode.valueOf("true").asBoolean(false));
        assertFalse(TextNode.valueOf("false").asBoolean(true));
        assertFalse(TextNode.valueOf("false").asBoolean(false));
    }

    @Test
    public void testEquals()
    {
        assertEquals(new TextNode(null), new TextNode(null));
        assertEquals(new TextNode("abc"), new TextNode("abc"));
        assertNotEquals(new TextNode(null), new TextNode("def"));
        assertNotEquals(new TextNode("abc"), new TextNode("def"));
        assertNotEquals(new TextNode("abc"), new TextNode(null));
    }

    @Test
    public void testHashCode()
    {
        assertEquals(0, new TextNode(null).hashCode());
        assertEquals("abc".hashCode(), new TextNode("abc").hashCode());
    }
}
