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
    
        _assertLongValueFailForValueRange(NODES.numberNode(underflow));
        _assertLongValueFailForValueRange(NODES.numberNode(overflow));
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

        _assertLongValueFailForValueRange(NODES.numberNode(underflow_d));
        _assertLongValueFailForValueRange(NODES.numberNode(overflow_d));

        _assertLongValueFailForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertLongValueFailForValueRange(NODES.numberNode(Float.MAX_VALUE));

        // But for BigDecimal can do exact check
        
        final BigDecimal underflow_big = BigDecimal.valueOf(Long.MIN_VALUE).subtract(BigDecimal.ONE);
        final BigDecimal overflow_big = BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE);

        _assertLongValueFailForValueRange(NODES.numberNode(underflow_big));
        _assertLongValueFailForValueRange(NODES.numberNode(overflow_big));
    }

    @Test
    public void longValueFromNumberFPFailFraction()
    {
        _assertLongValueFailForFraction(NODES.numberNode(100.5f));
        _assertLongValueFailForFraction(NODES.numberNode(-0.25f));

        _assertLongValueFailForFraction(NODES.numberNode(100.5d));
        _assertLongValueFailForFraction(NODES.numberNode(-0.25d));

        _assertLongValueFailForFraction(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertLongValueFailForFraction(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
    }

    @Test
    public void longValueFromNumberFPFailNaN()
    {
        _assertLongValueFailForNaN(NODES.numberNode(Float.NaN));
        _assertLongValueFailForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));
        _assertLongValueFailForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));

        _assertLongValueFailForNaN(NODES.numberNode(Double.NaN));
        _assertLongValueFailForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
        _assertLongValueFailForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
    }

    // longValue() + non-Numeric types

    @Test
    public void longValueFromNonNumberScalarFail()
    {
        _assertLongValueFailForNonNumber(NODES.booleanNode(true));
        _assertLongValueFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertLongValueFailForNonNumber(NODES.stringNode("123"));
        _assertLongValueFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertLongValueFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertLongValueFailForNonNumber(NODES.pojoNode(456L));
    }

    @Test
    public void longValueFromStructuralFail()
    {
        _assertLongValueFailForNonNumber(NODES.arrayNode(3));
        _assertLongValueFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void longValueFromMiscOtherFail()
    {
        _assertLongValueFailForNonNumber(NODES.nullNode());
        _assertLongValueFailForNonNumber(NODES.missingNode());
    }

    // // // asLong() tests

    @Test
    public void asLongFromNumberIntOk()
    {
        // First safe from `long`
        _assertAsLong(1L, NODES.numberNode(1L));
        _assertAsLong(Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertAsLong(Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        // Then other integer types, byte/short/int
        _assertAsLong(1L, NODES.numberNode((byte) 1));
        _assertAsLong((long)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertAsLong((long)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertAsLong(1L, NODES.numberNode((short) 1));
        _assertAsLong((long)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertAsLong((long)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertAsLong(1L, NODES.numberNode(1));
        _assertAsLong((long) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertAsLong((long) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        _assertAsLong(1L, NODES.numberNode(BigInteger.valueOf(1)));
        _assertAsLong(Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertAsLong(Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void asLongFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final BigInteger underflow = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        final BigInteger overflow = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    
        _assertAsLongFailForValueRange(NODES.numberNode(underflow));
        _assertAsLongFailForValueRange(NODES.numberNode(overflow));
    }

    // longValue() + Numbers/FPs

    @Test
    public void asLongFromNumberFPOk()
    {
        _assertAsLong(1, NODES.numberNode(1.0f));
        _assertAsLong(100_000, NODES.numberNode(100_000.0f));
        _assertAsLong(-100_000, NODES.numberNode(-100_000.0f));

        _assertAsLong(1, NODES.numberNode(1.0d));
        _assertAsLong(100_000, NODES.numberNode(100_000.0d));
        _assertAsLong(-100_000, NODES.numberNode(-100_000.0d));
        _assertAsLong(Long.MIN_VALUE, NODES.numberNode((double) Long.MIN_VALUE));
        _assertAsLong(Long.MAX_VALUE, NODES.numberNode((double) Long.MAX_VALUE));

        _assertAsLong(1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertAsLong(Long.MIN_VALUE,
                NODES.numberNode(new BigDecimal(Long.MIN_VALUE+".0")));
        _assertAsLong(Long.MAX_VALUE,
                NODES.numberNode(new BigDecimal(Long.MAX_VALUE+".0")));
    }

    @Test
    public void asLongFromNumberFPFailRange()
    {
        // For Float and Double both it's tricky to do too-big/too-small accurately so

        final double underflow_d = -Double.MAX_VALUE;
        final double overflow_d = Double.MAX_VALUE;

        _assertAsLongFailForValueRange(NODES.numberNode(underflow_d));
        _assertAsLongFailForValueRange(NODES.numberNode(overflow_d));

        _assertAsLongFailForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertAsLongFailForValueRange(NODES.numberNode(Float.MAX_VALUE));

        // But for BigDecimal can do exact check
        
        final BigDecimal underflow_big = BigDecimal.valueOf(Long.MIN_VALUE).subtract(BigDecimal.ONE);
        final BigDecimal overflow_big = BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE);

        _assertAsLongFailForValueRange(NODES.numberNode(underflow_big));
        _assertAsLongFailForValueRange(NODES.numberNode(overflow_big));
    }

    @Test
    public void asLongFromNumberFPWithFraction()
    {
        _assertAsLong(100L, NODES.numberNode(100.75f));
        _assertAsLong(-1L, NODES.numberNode(-1.25f));

        _assertAsLong(100L, NODES.numberNode(100.75d));
        _assertAsLong(-1L, NODES.numberNode(-1.25d));

        _assertAsLong(100L, NODES.numberNode(BigDecimal.valueOf(100.75d)));
        _assertAsLong(-1L, NODES.numberNode(BigDecimal.valueOf(-1.25d)));
    }

    @Test
    public void asLongFromNumberFPFailNaN()
    {
        _assertAsLongFailForNaN(NODES.numberNode(Float.NaN));
        _assertAsLongFailForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));
        _assertAsLongFailForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));

        _assertAsLongFailForNaN(NODES.numberNode(Double.NaN));
        _assertAsLongFailForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
        _assertAsLongFailForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
    }

    // longValue() + non-Numeric types

    @Test
    public void asLongFromNonNumberScalarFail()
    {
        // Some fail;
        _assertAsLongFailForNonNumber(NODES.booleanNode(true));
        _assertAsLongFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertAsLongFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertAsLongFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertAsLongFailForNonNumber(NODES.stringNode("abcdef"), "not a valid String representation of `long`");
        _assertAsLongFailForNonNumber(NODES.pojoNode(1e40));

        // Some pass
        _assertAsLong(1234L, NODES.stringNode("1234"));
        _assertAsLong(123456L, NODES.pojoNode(123456L));
        _assertAsLong(789L, NODES.pojoNode(BigInteger.valueOf(789)));
    }

    @Test
    public void asLongFromStructuralFail()
    {
        _assertAsLongFailForNonNumber(NODES.arrayNode(3));
        _assertAsLongFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void asLongFromMiscOther()
    {
        // NullNode works, Missing fails
        _assertAsLong(0L, NODES.nullNode());

        _assertAsLongFailForNonNumber(NODES.missingNode());
    }
    
    // // // Shared helper methods, longValue()

    private void _assertLongValue(long expected, JsonNode node)
    {
        assertEquals(expected, node.longValue());

        // But also fallbacks
        assertEquals(expected, node.longValue(999999L));
        assertEquals(expected, node.longValueOpt().getAsLong());
    }

    private void _assertLongValueFailForValueRange(JsonNode node) {
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

    private void _assertLongValueFailForFraction(JsonNode node) {
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

    private void _assertLongValueFailForNonNumber(JsonNode node) {
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

    private void _assertLongValueFailForNaN(JsonNode node) {
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

    // // // Shared helper methods, asLong()

    private void _assertAsLong(long expected, JsonNode node)
    {
        assertEquals(expected, node.asLong());

        // But also fallbacks
        assertEquals(expected, node.asLong(999999L));
        assertEquals(expected, node.asLongOpt().getAsLong());
    }

    private void _assertAsLongFailForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asLong(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asLong()")
            .contains("cannot convert value")
            .contains("value not in 64-bit `long` range");

        // Verify default value handling
        assertEquals(1L, node.asLong(1L));
        assertFalse(node.asLongOpt().isPresent());
    }

    private void _assertAsLongFailForNonNumber(JsonNode node) {
        _assertAsLongFailForNonNumber(node, "value type not numeric");
    }

    private void _assertAsLongFailForNonNumber(JsonNode node, String extraMsg) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asLong(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asLong()")
            .contains("cannot convert value")
            .contains(extraMsg);

        // Verify default value handling
        assertEquals(1L, node.asLong(1L));
        assertFalse(node.asLongOpt().isPresent());
    }

    private void _assertAsLongFailForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asLong(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asLong()")
            .contains("cannot convert value")
            .contains("value non-Finite");

        // Verify default value handling
        assertEquals(1L, node.asLong(1L));
        assertFalse(node.asLongOpt().isPresent());
    }
    
}
