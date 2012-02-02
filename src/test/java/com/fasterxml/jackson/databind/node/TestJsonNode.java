package com.fasterxml.jackson.databind.node;

import java.math.BigInteger;
import java.math.BigDecimal;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

/**
 * Basic tests for {@link JsonNode} base class and some features
 * of implementation classes
 */
public class TestJsonNode
    extends BaseMapTest
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
        assertNodeNumbers(n, 0, 0.0);
    }

    public void testText()
    {
        assertNull(TextNode.valueOf(null));
        TextNode empty = TextNode.valueOf("");
        assertStandardEquals(empty);
        assertSame(TextNode.EMPTY_STRING_NODE, empty);

        // 1.6:
        assertNodeNumbers(TextNode.valueOf("-3"), -3, -3.0);
        assertNodeNumbers(TextNode.valueOf("17.75"), 17, 17.75);
    
        // [JACKSON-587]
        long value = 127353264013893L;
        TextNode n = TextNode.valueOf(String.valueOf(value));
        assertEquals(value, n.asLong());
        
        // and then with non-numeric input
        assertNodeNumbersForNonNumeric(TextNode.valueOf("foobar"));

    }

    public void testBoolean()
    {
        BooleanNode f = BooleanNode.getFalse();
        assertNotNull(f);
        assertTrue(f.isBoolean());
        assertSame(f, BooleanNode.valueOf(false));
        assertStandardEquals(f);
        assertFalse(f.booleanValue());
        assertEquals("false", f.asText());
        assertEquals(JsonToken.VALUE_FALSE, f.asToken());

        // and ditto for true
        BooleanNode t = BooleanNode.getTrue();
        assertNotNull(t);
        assertTrue(t.isBoolean());
        assertSame(t, BooleanNode.valueOf(true));
        assertStandardEquals(t);
        assertTrue(t.booleanValue());
        assertEquals("true", t.asText());
        assertEquals(JsonToken.VALUE_TRUE, t.asToken());

        // 1.6:
        assertNodeNumbers(f, 0, 0.0);
        assertNodeNumbers(t, 1, 1.0);
    }


    public void testBinary() throws Exception
    {
        assertNull(BinaryNode.valueOf(null));
        assertNull(BinaryNode.valueOf(null, 0, 0));

        BinaryNode empty = BinaryNode.valueOf(new byte[1], 0, 0);
        assertSame(BinaryNode.EMPTY_BINARY_NODE, empty);
        assertStandardEquals(empty);

        byte[] data = new byte[3];
        data[1] = (byte) 3;
        BinaryNode n = BinaryNode.valueOf(data, 1, 1);
        data[2] = (byte) 3;
        BinaryNode n2 = BinaryNode.valueOf(data, 2, 1);
        assertTrue(n.equals(n2));
        assertEquals("\"Aw==\"", n.toString());

        assertEquals("AAMD", new BinaryNode(data).asText());

        // 1.6:
        assertNodeNumbersForNonNumeric(n);
    }

    public void testPOJO()
    {
        POJONode n = new POJONode("x"); // not really a pojo but that's ok
        assertStandardEquals(n);
        assertEquals(n, new POJONode("x"));
        assertEquals("x", n.asText());
        // not sure if this is what it'll remain as but:
        assertEquals("x", n.toString());

        assertEquals(new POJONode(null), new POJONode(null));

        // 1.6:
        // default; non-numeric
        assertNodeNumbersForNonNumeric(n);
        // but if wrapping actual number, use it
        assertNodeNumbers(new POJONode(Integer.valueOf(123)), 123, 123.0);
    }

    public void testMissing()
    {
        MissingNode n = MissingNode.getInstance();
        assertEquals(JsonToken.NOT_AVAILABLE, n.asToken());
        // as per [JACKSON-775]
        assertEquals("", n.asText());
        assertStandardEquals(n);
        assertEquals("", n.toString());

        // missing acts same as null, so:
        assertNodeNumbers(n, 0, 0.0);
    }
}
