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

        _assertIntValueFailForValueRange(NODES.numberNode(underflow));
        _assertIntValueFailForValueRange(NODES.numberNode(overflow));

        _assertIntValueFailForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertIntValueFailForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
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

        _assertIntValueFailForValueRange(NODES.numberNode((double)underflow));
        _assertIntValueFailForValueRange(NODES.numberNode((double)overflow));

        // Float is too inexact for using same test as Double, so:

        _assertIntValueFailForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertIntValueFailForValueRange(NODES.numberNode(Float.MAX_VALUE));

        _assertIntValueFailForValueRange(NODES.numberNode(BigDecimal.valueOf(underflow)));
        _assertIntValueFailForValueRange(NODES.numberNode(BigDecimal.valueOf(overflow)));
    }
    
    @Test
    public void intValueFromNumberFPFailFraction()
    {
        _assertIntValueFailForFraction(NODES.numberNode(100.5f));
        _assertIntValueFailForFraction(NODES.numberNode(-0.25f));

        _assertIntValueFailForFraction(NODES.numberNode(100.5d));
        _assertIntValueFailForFraction(NODES.numberNode(-0.25d));
        
        _assertIntValueFailForFraction(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertIntValueFailForFraction(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
    }

    @Test
    public void intValueFromNumberFPFailNaN()
    {
        _assertIntValueFailForNaN(NODES.numberNode(Float.NaN));
        _assertIntValueFailForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));
        _assertIntValueFailForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));

        _assertIntValueFailForNaN(NODES.numberNode(Double.NaN));
        _assertIntValueFailForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
        _assertIntValueFailForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
    }
    
    // // // intValue() + non-Numeric types

    @Test
    public void intValueFromNonNumberScalarFail()
    {
        _assertIntValueFailForNonNumber(NODES.booleanNode(true));
        _assertIntValueFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertIntValueFailForNonNumber(NODES.stringNode("123"));
        _assertIntValueFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertIntValueFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertIntValueFailForNonNumber(NODES.pojoNode(456));
    }

    @Test
    public void intValueFromStructuralFail()
    {
        _assertIntValueFailForNonNumber(NODES.arrayNode(3));
        _assertIntValueFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void intValueFromMiscOtherFail()
    {
        _assertIntValueFailForNonNumber(NODES.nullNode());
        _assertIntValueFailForNonNumber(NODES.missingNode());
    }

    // // // asInt()
    
    // Numbers/Integers

    @Test
    public void asIntFromNumberIntOk()
    {
        // First safe from `int`
        _assertAsInt(1, NODES.numberNode(1));
        _assertAsInt(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertAsInt(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        // Then other integer types
        _assertAsInt(1, NODES.numberNode((byte) 1));
        _assertAsInt((int)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertAsInt((int)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertAsInt(1, NODES.numberNode((short) 1));
        _assertAsInt((int)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertAsInt((int)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertAsInt(1, NODES.numberNode(1L));
        _assertAsInt(Integer.MIN_VALUE, NODES.numberNode((long) Integer.MIN_VALUE));
        _assertAsInt(Integer.MAX_VALUE, NODES.numberNode((long) Integer.MAX_VALUE));

        _assertAsInt(1, NODES.numberNode(BigInteger.valueOf(1)));
        _assertAsInt(Integer.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MIN_VALUE)));
        _assertAsInt(Integer.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    public void asIntFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = -1L + Integer.MIN_VALUE;
        final long overflow = +1L + Integer.MAX_VALUE;

        _assertAsIntFailForValueRange(NODES.numberNode(underflow));
        _assertAsIntFailForValueRange(NODES.numberNode(overflow));

        _assertAsIntFailForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertAsIntFailForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
    }
    
    //  Numbers/FPs

    @Test
    public void asIntFromNumberFPOk()
    {
        _assertAsInt(1, NODES.numberNode(1.0f));
        _assertAsInt(100_000, NODES.numberNode(100_000.0f));
        _assertAsInt(-100_000, NODES.numberNode(-100_000.0f));

        _assertAsInt(1, NODES.numberNode(1.0d));
        _assertAsInt(100_000, NODES.numberNode(100_000.0d));
        _assertAsInt(-100_000, NODES.numberNode(-100_000.0d));
        _assertAsInt(Integer.MIN_VALUE, NODES.numberNode((double) Integer.MIN_VALUE));
        _assertAsInt(Integer.MAX_VALUE, NODES.numberNode((double) Integer.MAX_VALUE));

        _assertAsInt(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertAsInt(Integer.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MIN_VALUE)));
        _assertAsInt(Integer.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Integer.MAX_VALUE)));
    }

    @Test
    public void asIntFromNumberFPFailRange()
    {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = Integer.MIN_VALUE - 1L;
        final long overflow =  Integer.MAX_VALUE + 1L;

        _assertAsIntFailForValueRange(NODES.numberNode((double)underflow));
        _assertAsIntFailForValueRange(NODES.numberNode((double)overflow));

        // Float is too inexact for using same test as Double, so:

        _assertAsIntFailForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertAsIntFailForValueRange(NODES.numberNode(Float.MAX_VALUE));

        _assertAsIntFailForValueRange(NODES.numberNode(BigDecimal.valueOf(underflow)));
        _assertAsIntFailForValueRange(NODES.numberNode(BigDecimal.valueOf(overflow)));
    }
    
    @Test
    public void asIntFromNumberFPWithFraction()
    {
        _assertAsInt(100, NODES.numberNode(100.75f));
        _assertAsInt(-1, NODES.numberNode(-1.25f));

        _assertAsInt(100, NODES.numberNode(100.75d));
        _assertAsInt(-1, NODES.numberNode(-1.25d));
        
        _assertAsInt(100, NODES.numberNode(BigDecimal.valueOf(100.75d)));
        _assertAsInt(-1, NODES.numberNode(BigDecimal.valueOf(-1.25d)));
    }

    @Test
    public void asIntFromNumberFPFailNaN()
    {
        _assertAsIntFailForNaN(NODES.numberNode(Float.NaN));
        _assertAsIntFailForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));
        _assertAsIntFailForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));

        _assertAsIntFailForNaN(NODES.numberNode(Double.NaN));
        _assertAsIntFailForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
        _assertAsIntFailForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
    }
    
    // non-Numeric types

    @Test
    public void asIntFromNonNumberScalar()
    {
        // Some fail:
        _assertAsIntFailForNonNumber(NODES.booleanNode(true));
        _assertAsIntFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertAsIntFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertAsIntFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertAsIntFailForNonNumber(NODES.stringNode("abc"),
                "value not a valid String representation of `int`");
        _assertAsIntFailForNonNumber(NODES.pojoNode(123_456_789_000L));

        // Some pass:
        _assertAsInt(123, NODES.stringNode("123"));
        _assertAsInt(456, NODES.pojoNode(456L));
        _assertAsInt(789, NODES.pojoNode(BigInteger.valueOf(789)));
    }

    @Test
    public void asIntFromStructuralFail()
    {
        _assertAsIntFailForNonNumber(NODES.arrayNode(3));
        _assertAsIntFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void asIntFromMiscOther()
    {
        // NullNode -> 0 but "missing" still fails
        _assertAsInt(0, NODES.nullNode());

        _assertAsIntFailForNonNumber(NODES.missingNode());
    }
    
    // // // Shared helper methods: intValue()

    private void _assertIntValue(int expected, JsonNode node) {
        assertEquals(expected, node.intValue());

        // and defaulting

        assertEquals(expected, node.intValue(999_999));
        assertEquals(expected, node.intValueOpt().getAsInt());
    }

    private void _assertIntValueFailForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("intValue()")
            .contains("cannot convert value")
            .contains("value not in 32-bit `int` range");

        // assert defaulting
        assertEquals(99, node.intValue(99));
        assertEquals(OptionalInt.empty(), node.intValueOpt());
    }

    private void _assertIntValueFailForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("intValue()")
            .contains("cannot convert value")
            .contains("to `int`: value has fractional part");

        // assert defaulting
        assertEquals(99, node.intValue(99));
        assertEquals(OptionalInt.empty(), node.intValueOpt());
    }

    private void _assertIntValueFailForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("intValue()")
            .contains("cannot convert value")
            .contains("value type not numeric");

        // assert defaulting
        assertEquals(99, node.intValue(99));
        assertEquals(OptionalInt.empty(), node.intValueOpt());
    }

    private void _assertIntValueFailForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("intValue()")
            .contains("cannot convert value")
            .contains("value non-Finite");

        // Verify default value handling
        assertEquals(1, node.intValue(1));
        assertFalse(node.intValueOpt().isPresent());
    }

    // // // Shared helper methods: asInt()

    private void _assertAsInt(int expected, JsonNode node) {
        assertEquals(expected, node.asInt());

        // and defaulting

        assertEquals(expected, node.asInt(999_999));
        assertEquals(expected, node.asIntOpt().getAsInt());
    }

    private void _assertAsIntFailForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asInt(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asInt()")
            .contains("cannot convert value")
            .contains("value not in 32-bit `int` range");

        // assert defaulting
        assertEquals(99, node.asInt(99));
        assertEquals(OptionalInt.empty(), node.asIntOpt());
    }

    private void _assertAsIntFailForNonNumber(JsonNode node) {
        _assertAsIntFailForNonNumber(node, "value type not numeric");
    }

    private void _assertAsIntFailForNonNumber(JsonNode node, String extraFailMsg) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asInt(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asInt()")
            .contains("cannot convert value")
            .contains(extraFailMsg);

        // assert defaulting
        assertEquals(99, node.asInt(99));
        assertEquals(OptionalInt.empty(), node.asIntOpt());
    }

    private void _assertAsIntFailForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asInt(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
        .contains("asInt()")
            .contains("cannot convert value")
            .contains("value non-Finite");

        // Verify default value handling
        assertEquals(1, node.asInt(1));
        assertFalse(node.asIntOpt().isPresent());
    }

}
