package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#4958], JsonNode.longValue() (and related) parts
 * over all types.
 */
public class JsonNodeLongValueTest
    extends NodeTestBase
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // longValue() + Numbers/Integers

    @Test
    public void longValueFromNumberIntOk()
    {
        // First safe from `long`
        _assertLongValue(1L, NODES.numberNode(1L));
        _assertLongValue(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertLongValue(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        // Then other integer types, byte/short/int
        _assertLongValue(1L, NODES.numberNode((byte) 1));
        _assertLongValue((long)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertLongValue((long)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertLongValue(1L, NODES.numberNode((short) 1));
        _assertLongValue((long)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertLongValue((long)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertLongValue(1L, NODES.numberNode(1));
        _assertLongValue((long) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertLongValue((long) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        _assertLongValue(1L, NODES.numberNode(BigInteger.valueOf(1)));
        _assertLongValue(Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertLongValue(Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void longValueFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final BigInteger underflow = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        final BigInteger overflow = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    
        _assertFailLongForValueRange(NODES.numberNode(underflow));
        _assertFailLongForValueRange(NODES.numberNode(overflow));
    }

    // longValue() + Numbers/FPs

    @Test
    public void longValueFromNumberFPOk()
    {
        _assertLongValue(1, NODES.numberNode(1.0f));
        _assertLongValue(100_000, NODES.numberNode(100_000.0f));
        _assertLongValue(-100_000, NODES.numberNode(-100_000.0f));

        _assertLongValue(1, NODES.numberNode(1.0d));
        _assertLongValue(100_000, NODES.numberNode(100_000.0d));
        _assertLongValue(-100_000, NODES.numberNode(-100_000.0d));
        _assertLongValue(Long.MIN_VALUE, NODES.numberNode((double) Long.MIN_VALUE));
        _assertLongValue(Long.MAX_VALUE, NODES.numberNode((double) Long.MAX_VALUE));

        _assertLongValue(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertLongValue(Long.MIN_VALUE,
                NODES.numberNode(new BigDecimal(Long.MIN_VALUE+".0")));
        _assertLongValue(Long.MAX_VALUE,
                NODES.numberNode(new BigDecimal(Long.MAX_VALUE+".0")));
    }

    @Test
    public void longValueFromNumberFPFailRange()
    {
        // For Float and Double both it's tricky to do too-big/too-small accurately so

        final double underflow_d = -Double.MAX_VALUE;
        final double overflow_d = Double.MAX_VALUE;

        _assertFailLongForValueRange(NODES.numberNode(underflow_d));
        _assertFailLongForValueRange(NODES.numberNode(overflow_d));

        _assertFailLongForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertFailLongForValueRange(NODES.numberNode(Float.MAX_VALUE));

        // But for BigDecimal can do exact check
        
        final BigDecimal underflow_big = BigDecimal.valueOf(Long.MIN_VALUE).subtract(BigDecimal.ONE);
        final BigDecimal overflow_big = BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE);

        _assertFailLongForValueRange(NODES.numberNode(underflow_big));
        _assertFailLongForValueRange(NODES.numberNode(overflow_big));
    }

    @Test
    public void longValueFromNumberFPFailFraction()
    {
        _assertFailLongValueForFraction(NODES.numberNode(100.5f));
        _assertFailLongValueForFraction(NODES.numberNode(-0.25f));

        _assertFailLongValueForFraction(NODES.numberNode(100.5d));
        _assertFailLongValueForFraction(NODES.numberNode(-0.25d));

        _assertFailLongValueForFraction(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertFailLongValueForFraction(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
    }

    @Test
    public void longValueFromNumberFPFailNaN()
    {
        _assertFailLongForNaN(NODES.numberNode(Float.NaN));
        _assertFailLongForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));
        _assertFailLongForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));

        _assertFailLongForNaN(NODES.numberNode(Double.NaN));
        _assertFailLongForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
        _assertFailLongForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
    }

    // longValue() + non-Numeric types

    @Test
    public void longValueFromNonNumberScalarFail()
    {
        _assertFailLongForNonNumber(NODES.booleanNode(true));
        _assertFailLongForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailLongForNonNumber(NODES.stringNode("123"));
        _assertFailLongForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailLongForNonNumber(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void longValueFromStructuralFail()
    {
        _assertFailLongForNonNumber(NODES.arrayNode(3));
        _assertFailLongForNonNumber(NODES.objectNode());
    }

    @Test
    public void longValueFromMiscOtherFail()
    {
        _assertFailLongForNonNumber(NODES.nullNode());
        _assertFailLongForNonNumber(NODES.missingNode());
    }

    // // // Shared helper methods

    private void _assertLongValue(long expected, JsonNode node)
    {
        assertEquals(expected, node.longValue());

        // But also fallbacks
        assertEquals(expected, node.longValue(999999L));
        assertEquals(expected, node.longValueOpt().getAsLong());
    }

    private void _assertFailLongForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.longValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("longValue()")
            .contains("cannot convert value")
            .contains("value not in 64-bit `long` range");

        // Verify default value handling
        assertEquals(1L, node.longValue(1L));
        assertFalse(node.longValueOpt().isPresent());
    }

    private void _assertFailLongValueForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.longValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("longValue()")
            .contains("cannot convert value")
            .contains("to `long`: value has fractional part");

        // Verify default value handling
        assertEquals(1L, node.longValue(1L));
        assertFalse(node.longValueOpt().isPresent());
    }

    private void _assertFailLongForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.longValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("longValue()")
            .contains("cannot convert value")
            .contains("value type not numeric");

        // Verify default value handling
        assertEquals(1L, node.longValue(1L));
        assertFalse(node.longValueOpt().isPresent());
    }

    private void _assertFailLongForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.longValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("longValue()")
            .contains("cannot convert value")
            .contains("value non-Finite");

        // Verify default value handling
        assertEquals(1L, node.longValue(1L));
        assertFalse(node.longValueOpt().isPresent());
    }
}
