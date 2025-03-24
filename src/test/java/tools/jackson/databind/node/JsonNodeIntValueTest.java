package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
        _assertIntValue(1, NODES.numberNode(1));
        _assertIntValue(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertIntValue(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        // Then other integer types
        _assertIntValue(1, NODES.numberNode((byte) 1));
        _assertIntValue((int)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertIntValue((int)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertIntValue(1, NODES.numberNode((short) 1));
        _assertIntValue((int)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertIntValue((int)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertIntValue(1, NODES.numberNode(1L));
        _assertIntValue(Integer.MIN_VALUE, NODES.numberNode((long) Integer.MIN_VALUE));
        _assertIntValue(Integer.MAX_VALUE, NODES.numberNode((long) Integer.MAX_VALUE));

        _assertIntValue(1, NODES.numberNode(BigInteger.valueOf(1)));
        _assertIntValue(Integer.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MIN_VALUE)));
        _assertIntValue(Integer.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MAX_VALUE)));
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
        _assertIntValue(1, NODES.numberNode(1.0f));
        _assertIntValue(100_000, NODES.numberNode(100_000.0f));
        _assertIntValue(-100_000, NODES.numberNode(-100_000.0f));

        _assertIntValue(1, NODES.numberNode(1.0d));
        _assertIntValue(100_000, NODES.numberNode(100_000.0d));
        _assertIntValue(-100_000, NODES.numberNode(-100_000.0d));
        _assertIntValue(Integer.MIN_VALUE, NODES.numberNode((double) Integer.MIN_VALUE));
        _assertIntValue(Integer.MAX_VALUE, NODES.numberNode((double) Integer.MAX_VALUE));

        _assertIntValue(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertIntValue(Integer.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MIN_VALUE)));
        _assertIntValue(Integer.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MAX_VALUE)));
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
        _assertFailIntForNonNumber(NODES.stringNode("123"));
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
    
    // // // shortValue()

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
    public void shortValueFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final int underflow = -1 + Short.MIN_VALUE;
        final int overflow = +1 + Short.MAX_VALUE;

        _assertFailShortForValueRange(NODES.numberNode(underflow));
        _assertFailShortForValueRange(NODES.numberNode(overflow));

        _assertFailShortForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertFailShortForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
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
    public void shortValueFromNumberFPFailRange()
    {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = Short.MIN_VALUE - 1L;
        final long overflow =  Short.MAX_VALUE + 1L;

        _assertFailShortForValueRange(NODES.numberNode((double)underflow));
        _assertFailShortForValueRange(NODES.numberNode((double)overflow));

        // Float is too inexact for using same test as Double, so:

        _assertFailShortForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertFailShortForValueRange(NODES.numberNode(Float.MAX_VALUE));

        _assertFailShortForValueRange(NODES.numberNode(BigDecimal.valueOf(underflow)));
        _assertFailShortForValueRange(NODES.numberNode(BigDecimal.valueOf(overflow)));
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

    @Test
    public void shortValueFromNonNumberFail()
    {
        _assertFailShortForNonNumber(NODES.booleanNode(true));
        _assertFailShortForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailShortForNonNumber(NODES.stringNode("123"));
        _assertFailShortForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailShortForNonNumber(NODES.pojoNode(Boolean.TRUE));

        _assertFailShortForNonNumber(NODES.arrayNode(3));
        _assertFailShortForNonNumber(NODES.objectNode());
        
        _assertFailShortForNonNumber(NODES.nullNode());
        _assertFailShortForNonNumber(NODES.missingNode());
    }

    // // // Shared helper methods

    private void _assertIntValue(int expected, JsonNode node) {
        assertEquals(expected, node.intValue());

        // and defaulting

        assertEquals(expected, node.intValue(999_999));
        assertEquals(expected, node.intValueOpt().getAsInt());
    }

    private void _assertFailIntForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 32-bit `int` range");

        // assert defaulting
        assertEquals(99, node.intValue(99));
        assertEquals(OptionalInt.empty(), node.intValueOpt());
    }

    private void _assertFailShortForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.shortValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 16-bit `short` range");

    }

    private void _assertFailIntValueForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("to `int`: value has fractional part");

        // assert defaulting
        assertEquals(99, node.intValue(99));
        assertEquals(OptionalInt.empty(), node.intValueOpt());
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

        // assert defaulting
        assertEquals(99, node.intValue(99));
        assertEquals(OptionalInt.empty(), node.intValueOpt());
    }

    private void _assertFailShortForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.shortValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");
    }
}
