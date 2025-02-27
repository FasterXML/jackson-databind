package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Tests for [databind#4958], JsonNode.intValue() (and related) parts
 * over all types
 */
public class JsonNodeIntValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // intValue() + Numbers/Integers

    @Test
    public void intValueFromNumberIntOk()
    {
        // First safe from `int`
        assertEquals(1, NODES.numberNode(1).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).intValue());

        // Then other integer types
        assertEquals(1, NODES.numberNode((byte) 1).intValue());
        assertEquals((int)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).intValue());
        assertEquals((int)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).intValue());

        assertEquals(1, NODES.numberNode((short) 1).intValue());
        assertEquals((int)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).intValue());
        assertEquals((int)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).intValue());

        assertEquals(1, NODES.numberNode(1L).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode((long) Integer.MIN_VALUE).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode((long) Integer.MAX_VALUE).intValue());

        assertEquals(1, NODES.numberNode(BigInteger.valueOf(1)).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MIN_VALUE)).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MAX_VALUE)).intValue());
    }
    
    @Test
    public void intValueFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = -1L + Integer.MIN_VALUE;
        final long overflow = +1L + Integer.MAX_VALUE;

        _assertFailIntForValueRange(NODES.numberNode(underflow));
        _assertFailIntForValueRange(NODES.numberNode(overflow));

        _assertFailIntForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertFailIntForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
    }

    // // // intValue() + Numbers/FPs

    @Test
    public void intValueFromNumberFPOk()
    {
        assertEquals(1, NODES.numberNode(1.0f).intValue());
        assertEquals(100_000, NODES.numberNode(100_000.0f).intValue());
        assertEquals(-100_000, NODES.numberNode(-100_000.0f).intValue());

        assertEquals(1, NODES.numberNode(1.0d).intValue());
        assertEquals(100_000, NODES.numberNode(100_000.0f).intValue());
        assertEquals(-100_000, NODES.numberNode(-100_000.0d).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode((double) Integer.MIN_VALUE).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode((double) Integer.MAX_VALUE).intValue());

        assertEquals(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).intValue());
        assertEquals(Integer.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MIN_VALUE)).intValue());
        assertEquals(Integer.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MAX_VALUE)).intValue());
    }

    @Test
    public void intValueFromNumberFPFailRange()
    {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = Integer.MIN_VALUE - 1L;
        final long overflow =  Integer.MAX_VALUE + 1L;

        _assertFailIntForValueRange(NODES.numberNode((double)underflow));
        _assertFailIntForValueRange(NODES.numberNode((double)overflow));

        // Float is too inexact for using same test as Double, so:
        
        _assertFailIntForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertFailIntForValueRange(NODES.numberNode(Float.MAX_VALUE));

        _assertFailIntForValueRange(NODES.numberNode(BigDecimal.valueOf(underflow)));
        _assertFailIntForValueRange(NODES.numberNode(BigDecimal.valueOf(overflow)));
    }

    @Test
    public void intValueFromNumberFPFailFraction()
    {
        _assertFailIntValueForFraction(NODES.numberNode(100.5f));
        _assertFailIntValueForFraction(NODES.numberNode(-0.25f));

        _assertFailIntValueForFraction(NODES.numberNode(100.5d));
        _assertFailIntValueForFraction(NODES.numberNode(-0.25d));
        
        _assertFailIntValueForFraction(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertFailIntValueForFraction(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
    }

    // // // intValue() + non-Numeric types

    @Test
    public void intValueFromNonNumberScalarFail()
    {
        _assertFailIntForNonNumber(NODES.booleanNode(true));
        _assertFailIntForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailIntForNonNumber(NODES.stringNode("false"));
        _assertFailIntForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailIntForNonNumber(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void intValueFromStructuralFail()
    {
        _assertFailIntForNonNumber(NODES.arrayNode(3));
        _assertFailIntForNonNumber(NODES.objectNode());
    }

    @Test
    public void intValueFromMiscOtherFail()
    {
        _assertFailIntForNonNumber(NODES.nullNode());
        _assertFailIntForNonNumber(NODES.missingNode());
    }

    // // // Shared helper methods

    private void _assertFailIntForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 32-bit `int` range");
    }

    private void _assertFailIntValueForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("to `int`: value has fractional part");
    }

    private void _assertFailIntForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");
    }
}
