package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StringNodeTest extends NodeTestBase
{
    @Test
    public void testBasics()
    {
        assertNull(StringNode.valueOf(null));
        StringNode empty = StringNode.valueOf("");
        assertStandardEquals(empty);
        assertSame(StringNode.EMPTY_STRING_NODE, empty);

        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());

        assertNodeNumbers(StringNode.valueOf("-3"), -3, -3.0);

        long value = 127353264013893L;
        StringNode n = StringNode.valueOf(String.valueOf(value));
        assertEquals(value, n.asLong());

        assertFalse(n.isNumber());
        assertFalse(n.canConvertToInt());
        assertFalse(n.canConvertToLong());
        assertFalse(n.canConvertToExactIntegral());

        // and then with non-numeric input
        n = StringNode.valueOf("foobar");
        assertNodeNumbersForNonNumeric(n);

        assertEquals("foobar", n.asString());
        assertEquals("", empty.asString());

        assertTrue(StringNode.valueOf("true").asBoolean(true));
        assertTrue(StringNode.valueOf("true").asBoolean(false));
        assertFalse(StringNode.valueOf("false").asBoolean(true));
        assertFalse(StringNode.valueOf("false").asBoolean(false));

        assertNonContainerStreamMethods(n);
    }

    @Test
    public void testEquals()
    {
        assertEquals(new StringNode("abc"), new StringNode("abc"));
        assertNotEquals(new StringNode("abc"), new StringNode("def"));
    }

    @Test
    public void testHashCode()
    {
        assertEquals("abc".hashCode(), new StringNode("abc").hashCode());
    }
}
