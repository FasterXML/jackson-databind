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
import java.util.OptionalInt;

/**
 * Tests for [databind#4958], JsonNode.intValue() (and related) parts
 * over all types.
 *<p>
 * Also contains tests for {@code JsonNode.shortValue()}.
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
        assertEquals(1, NODES.numberNode(1).intValue(99));
        assertEquals(1, NODES.numberNode(1).intValueOpt().getAsInt());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).intValue(99));
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).intValueOpt().getAsInt());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).intValue(99));
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).intValueOpt().getAsInt());

        // Then other integer types
        assertEquals(1, NODES.numberNode((byte) 1).intValue());
        assertEquals(1, NODES.numberNode((byte) 1).intValue(99));
        assertEquals(1, NODES.numberNode((byte) 1).intValue(99));
        assertEquals((int)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).intValue());
        assertEquals((int)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).intValue(99));
        assertEquals((int)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).intValueOpt().getAsInt());
        assertEquals((int)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).intValue());
        assertEquals((int)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).intValue(99));
        assertEquals((int)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).intValueOpt().getAsInt());

        assertEquals(1, NODES.numberNode((short) 1).intValue());
        assertEquals(1, NODES.numberNode((short) 1).intValue(99));
        assertEquals(1, NODES.numberNode((short) 1).intValueOpt().getAsInt());
        assertEquals((int)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).intValue());
        assertEquals((int)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).intValue(99));
        assertEquals((int)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).intValueOpt().getAsInt());
        assertEquals((int)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).intValue());
        assertEquals((int)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).intValue(99));
        assertEquals((int)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).intValueOpt().getAsInt());

        assertEquals(1, NODES.numberNode(1L).intValue());
        assertEquals(1, NODES.numberNode(1L).intValue(99));
        assertEquals(1, NODES.numberNode(1L).intValueOpt().getAsInt());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode((long) Integer.MIN_VALUE).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode((long) Integer.MIN_VALUE).intValue(99));
        assertEquals(Integer.MIN_VALUE, NODES.numberNode((long) Integer.MIN_VALUE).intValueOpt().getAsInt());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode((long) Integer.MAX_VALUE).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode((long) Integer.MAX_VALUE).intValue(99));
        assertEquals(Integer.MAX_VALUE, NODES.numberNode((long) Integer.MAX_VALUE).intValueOpt().getAsInt());

        assertEquals(1, NODES.numberNode(BigInteger.valueOf(1)).intValue());
        assertEquals(1, NODES.numberNode(BigInteger.valueOf(1)).intValue(99));
        assertEquals(1, NODES.numberNode(BigInteger.valueOf(1)).intValueOpt().getAsInt());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MIN_VALUE)).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MIN_VALUE)).intValue(99));
        assertEquals(Integer.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MIN_VALUE)).intValueOpt().getAsInt());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MAX_VALUE)).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MAX_VALUE)).intValue(99));
        assertEquals(Integer.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MAX_VALUE)).intValueOpt().getAsInt());
    }

    @Test
    public void shortValueFromNumberIntOk()
    {
        final short SHORT_1 = (short) 1;
        final short MIN_SHORT = Short.MIN_VALUE;
        final short MAX_SHORT = Short.MAX_VALUE;
        
        // First safe from `short`
        assertEquals(SHORT_1, NODES.numberNode((short) 1).shortValue());
        assertEquals((int)Short.MIN_VALUE, NODES.numberNode(MIN_SHORT).shortValue());
        assertEquals((int)Short.MAX_VALUE, NODES.numberNode(MAX_SHORT).shortValue());

        // Then other integer types

        assertEquals(SHORT_1, NODES.numberNode((byte) 1).shortValue());
        assertEquals((short) Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).shortValue());
        assertEquals((short) Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).shortValue());

        assertEquals(SHORT_1, NODES.numberNode(1).shortValue());
        assertEquals(MIN_SHORT, NODES.numberNode((int) MIN_SHORT).shortValue());
        assertEquals(MAX_SHORT, NODES.numberNode((int) MAX_SHORT).shortValue());

        assertEquals(SHORT_1, NODES.numberNode(1L).shortValue());
        assertEquals(MIN_SHORT, NODES.numberNode((long) MIN_SHORT).shortValue());
        assertEquals(MAX_SHORT, NODES.numberNode((long) MAX_SHORT).shortValue());

        assertEquals(SHORT_1, NODES.numberNode(BigInteger.valueOf(1)).shortValue());
        assertEquals(MIN_SHORT, NODES.numberNode(BigInteger.valueOf(MIN_SHORT)).shortValue());
        assertEquals(MAX_SHORT, NODES.numberNode(BigInteger.valueOf(MAX_SHORT)).shortValue());
    }

    @Test
    public void intValueFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = -1L + Integer.MIN_VALUE;
        final long overflow = +1L + Integer.MAX_VALUE;

        _assertFailIntForValueRange(NODES.numberNode(underflow));
        _assertDefaultIntForValueRange(NODES.numberNode(underflow));
        _assertFailIntForValueRange(NODES.numberNode(overflow));
        _assertDefaultIntForValueRange(NODES.numberNode(overflow));

        _assertFailIntForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertDefaultIntForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertFailIntForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
        _assertDefaultIntForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
    }

    @Test
    public void shortValueFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final int underflow = -1 + Short.MIN_VALUE;
        final int overflow = +1 + Short.MAX_VALUE;

        _assertFailShortForValueRange(NODES.numberNode(underflow));
        _assertFailShortForValueRange(NODES.numberNode(overflow));

        _assertFailShortForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertFailShortForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
    }

    // // // intValue() + Numbers/FPs

    @Test
    public void intValueFromNumberFPOk()
    {
        assertEquals(1, NODES.numberNode(1.0f).intValue());
        assertEquals(1, NODES.numberNode(1.0f).intValue(99));
        assertEquals(100_000, NODES.numberNode(100_000.0f).intValue());
        assertEquals(100_000, NODES.numberNode(100_000.0f).intValue(99));
        assertEquals(-100_000, NODES.numberNode(-100_000.0f).intValue());
        assertEquals(-100_000, NODES.numberNode(-100_000.0f).intValue(99));

        assertEquals(1, NODES.numberNode(1.0d).intValue());
        assertEquals(1, NODES.numberNode(1.0d).intValue(99));
        assertEquals(100_000, NODES.numberNode(100_000.0d).intValue());
        assertEquals(100_000, NODES.numberNode(100_000.0d).intValue(99));
        assertEquals(-100_000, NODES.numberNode(-100_000.0d).intValue());
        assertEquals(-100_000, NODES.numberNode(-100_000.0d).intValue(99));
        assertEquals(Integer.MIN_VALUE, NODES.numberNode((double) Integer.MIN_VALUE).intValue());
        assertEquals(Integer.MIN_VALUE, NODES.numberNode((double) Integer.MIN_VALUE).intValue(99));
        assertEquals(Integer.MAX_VALUE, NODES.numberNode((double) Integer.MAX_VALUE).intValue());
        assertEquals(Integer.MAX_VALUE, NODES.numberNode((double) Integer.MAX_VALUE).intValue(99));

        assertEquals(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).intValue());
        assertEquals(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).intValue(99));
        assertEquals(Integer.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MIN_VALUE)).intValue());
        assertEquals(Integer.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MIN_VALUE)).intValue(99));
        assertEquals(Integer.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MAX_VALUE)).intValue());
        assertEquals(Integer.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MAX_VALUE)).intValue(99));
    }

    @Test
    public void shortValueFromNumberFPOk()
    {
        assertEquals(1, NODES.numberNode(1.0f).shortValue());
        assertEquals(10_000, NODES.numberNode(10_000.0f).shortValue());
        assertEquals(-10_000, NODES.numberNode(-10_000.0f).shortValue());

        assertEquals(1, NODES.numberNode(1.0d).shortValue());
        assertEquals(10_000, NODES.numberNode(10_000.0d).shortValue());
        assertEquals(-10_000, NODES.numberNode(-10_000.0d).shortValue());
        assertEquals(Short.MIN_VALUE, NODES.numberNode((double) Short.MIN_VALUE).shortValue());
        assertEquals(Short.MAX_VALUE, NODES.numberNode((double) Short.MAX_VALUE).shortValue());

        assertEquals(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).shortValue());
        assertEquals(Short.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Short.MIN_VALUE)).shortValue());
        assertEquals(Short.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Short.MAX_VALUE)).shortValue());
    }

    @Test
    public void intValueFromNumberFPFailRange()
    {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = Integer.MIN_VALUE - 1L;
        final long overflow =  Integer.MAX_VALUE + 1L;

        _assertFailIntForValueRange(NODES.numberNode((double)underflow));
        _assertDefaultIntForValueRange(NODES.numberNode((double)underflow));
        _assertFailIntForValueRange(NODES.numberNode((double)overflow));
        _assertDefaultIntForValueRange(NODES.numberNode((double)overflow));

        // Float is too inexact for using same test as Double, so:

        _assertFailIntForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertDefaultIntForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertFailIntForValueRange(NODES.numberNode(Float.MAX_VALUE));
        _assertDefaultIntForValueRange(NODES.numberNode(Float.MAX_VALUE));

        _assertFailIntForValueRange(NODES.numberNode(BigDecimal.valueOf(underflow)));
        _assertDefaultIntForValueRange(NODES.numberNode(BigDecimal.valueOf(underflow)));
        _assertFailIntForValueRange(NODES.numberNode(BigDecimal.valueOf(overflow)));
        _assertDefaultIntForValueRange(NODES.numberNode(BigDecimal.valueOf(overflow)));
    }

    @Test
    public void intValueFromNumberFPFailFraction()
    {
        _assertFailIntValueForFraction(NODES.numberNode(100.5f));
        _assertDefaultIntForValueRange(NODES.numberNode(100.5f));
        _assertFailIntValueForFraction(NODES.numberNode(-0.25f));
        _assertDefaultIntForValueRange(NODES.numberNode(-0.25f));

        _assertFailIntValueForFraction(NODES.numberNode(100.5d));
        _assertDefaultIntForValueRange(NODES.numberNode(100.5d));
        _assertFailIntValueForFraction(NODES.numberNode(-0.25d));
        _assertDefaultIntForValueRange(NODES.numberNode(-0.25d));
        
        _assertFailIntValueForFraction(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertDefaultIntForValueRange(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertFailIntValueForFraction(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
        _assertDefaultIntForValueRange(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
    }

    @Test
    public void shortValueFromNumberFPFailFraction()
    {
        _assertFailShortValueForFraction(NODES.numberNode(100.5f));
        _assertFailShortValueForFraction(NODES.numberNode(-0.25f));

        _assertFailShortValueForFraction(NODES.numberNode(100.5d));
        _assertFailShortValueForFraction(NODES.numberNode(-0.25d));
        
        _assertFailShortValueForFraction(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertFailShortValueForFraction(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
    }

    // // // intValue() + non-Numeric types

    @Test
    public void intValueFromNonNumberScalarFail()
    {
        _assertFailIntForNonNumber(NODES.booleanNode(true));
        _assertDefaultIntForValueRange(NODES.booleanNode(true));
        _assertFailIntForNonNumber(NODES.binaryNode(new byte[3]));
        _assertDefaultIntForValueRange(NODES.binaryNode(new byte[3]));
        _assertFailIntForNonNumber(NODES.stringNode("123"));
        _assertDefaultIntForValueRange(NODES.stringNode("123"));
        _assertFailIntForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertDefaultIntForValueRange(NODES.rawValueNode(new RawValue("abc")));
        _assertFailIntForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertDefaultIntForValueRange(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void intValueFromStructuralFail()
    {
        _assertFailIntForNonNumber(NODES.arrayNode(3));
        _assertDefaultIntForValueRange(NODES.arrayNode(3));
        _assertFailIntForNonNumber(NODES.objectNode());
        _assertDefaultIntForValueRange(NODES.objectNode());
    }

    @Test
    public void intValueFromMiscOtherFail()
    {
        _assertFailIntForNonNumber(NODES.nullNode());
        _assertDefaultIntForValueRange(NODES.nullNode());
        _assertFailIntForNonNumber(NODES.missingNode());
        _assertDefaultIntForValueRange(NODES.missingNode());
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

    private void _assertFailShortForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.shortValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 16-bit `short` range");
    }

    private void _assertDefaultIntForValueRange(JsonNode node) {
        assertEquals(99, node.intValue(99));
        assertEquals(OptionalInt.empty(), node.intValueOpt());
    }

    private void _assertFailIntValueForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("to `int`: value has fractional part");
    }

    private void _assertFailShortValueForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.shortValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("to `short`: value has fractional part");
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
