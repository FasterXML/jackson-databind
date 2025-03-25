package tools.jackson.databind.node;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;

abstract class NodeTestBase extends DatabindTestUtil
{
    protected void assertNodeNumbersForNonNumeric(JsonNode n)
    {
        assertFalse(n.isNumber());
        assertFalse(n.canConvertToInt());
        assertFalse(n.canConvertToLong());
        assertFalse(n.canConvertToExactIntegral());

        // As of 3.0, coercion rules vary by specific type so can no longer test these
        //assertEquals(-42, n.asInt(-42));
        //assertEquals(12345678901L, n.asLong(12345678901L));
        //assertEquals(-19.25, n.asDouble(-19.25));
    }

    // Test to check conversions, coercions
    protected void assertNodeNumbers(JsonNode n, int expInt, double expDouble)
    {
        assertEquals(expInt, n.asInt());
        assertEquals(expInt, n.asInt(-42));
        assertEquals((long) expInt, n.asLong());
        assertEquals((long) expInt, n.asLong(19L));
        assertEquals(expDouble, n.asDouble());
        assertEquals(expDouble, n.asDouble(-19.25));

        assertTrue(n.isEmpty());
    }

    // Testing for non-ContainerNode (ValueNode) stream method implementations
    //
    // @since 2.19
    protected void assertNonContainerStreamMethods(ValueNode n)
    {
        assertEquals(0, n.valueStream().count());
        assertEquals(0, n.propertyStream().count());

        // And then empty forEachEntry()
        n.forEachEntry((k, v) -> { throw new UnsupportedOperationException(); });
    }

    protected static BigDecimal bigDec(long l) { return new BigDecimal(l); }
    protected static BigDecimal bigDec(double d) { return new BigDecimal(d); }
    protected static BigDecimal bigDec(String str) { return new BigDecimal(str); }

    protected static BigInteger bigInt(long l) {
        return BigInteger.valueOf(l);
    }
}
