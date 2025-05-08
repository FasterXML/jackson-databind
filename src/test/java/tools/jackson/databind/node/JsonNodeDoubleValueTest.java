package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#4958], JsonNode.doubleValue() (and related) parts
 * over all types.
 */
public class JsonNodeDoubleValueTest
    extends NodeTestBase
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // doubleValue()
    
    // from Integers

    @Test
    public void doubleValueFromNumberIntOk()
    {
        final double ONE_D = (double) 1;

        _assertDoubleValue(ONE_D, NODES.numberNode((byte) 1));
        _assertDoubleValue((double)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertDoubleValue((double)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertDoubleValue(ONE_D, NODES.numberNode((short) 1));
        _assertDoubleValue((double)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertDoubleValue((double)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertDoubleValue(ONE_D, NODES.numberNode(1));
        _assertDoubleValue((double) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertDoubleValue((double) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));
        
        _assertDoubleValue(ONE_D, NODES.numberNode(1L));
        _assertDoubleValue((double) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE));
        _assertDoubleValue((double) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE));

        _assertDoubleValue(ONE_D, NODES.numberNode(BigInteger.valueOf(1)));
        _assertDoubleValue((double) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertDoubleValue((double) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void failDoubleValueFromNumberIntRange() {
        // Can only fail for underflow/overflow: and that only for / BigInteger
        // (neither Integer nor Long is outside of range of even Float).

        final BigInteger tooBig = BigInteger.TEN.pow(310);      
        final BigInteger tooSmall = tooBig.negate();

        _assertDoubleValueFailForValueRange(NODES.numberNode(tooBig));
        _assertDoubleValueFailForValueRange(NODES.numberNode(tooSmall));
    }

    // From FPs

    @Test
    public void doubleValueFromNumberFPOk()
    {
        _assertDoubleValue(1.0, NODES.numberNode(1.0f));
        _assertDoubleValue(100_000.0, NODES.numberNode(100_000.0f));
        _assertDoubleValue(-100_000.0, NODES.numberNode(-100_000.0f));

        _assertDoubleValue(1.0, NODES.numberNode(1.0d));
        _assertDoubleValue(100_000.0, NODES.numberNode(100_000.0d));
        _assertDoubleValue(-100_000.0, NODES.numberNode(-100_000.0d));

        _assertDoubleValue(1.0,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertDoubleValue((double) Long.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MIN_VALUE)));
        _assertDoubleValue((double) Long.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MAX_VALUE)));
    }

    @Test
    public void failDoubleValueFromNumberFPRange()
    {
        // Can only fail from BigDecimal (similar to ints vs BigInteger)

        final BigDecimal tooBig = new BigDecimal(BigInteger.TEN.pow(310))
                .add(BigDecimal.valueOf(0.125));
        final BigDecimal tooSmall = tooBig.negate();

        _assertDoubleValueFailForValueRange(NODES.numberNode(tooBig));
        _assertDoubleValueFailForValueRange(NODES.numberNode(tooSmall));
    }

    // from non-Numeric types

    @Test
    public void failDoubleValueFromNonNumberScalar()
    {
        _assertDoubleValueFailForNonNumber(NODES.booleanNode(true));
        _assertDoubleValueFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertDoubleValueFailForNonNumber(NODES.stringNode("123"));
        _assertDoubleValueFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertDoubleValueFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertDoubleValueFailForNonNumber(NODES.pojoNode(3.8d));
    }

    @Test
    public void failDoubleValueFromStructural()
    {
        _assertDoubleValueFailForNonNumber(NODES.arrayNode(3));
        _assertDoubleValueFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void failDoubleValueFromMiscOther()
    {
        _assertDoubleValueFailForNonNumber(NODES.nullNode());
        _assertDoubleValueFailForNonNumber(NODES.missingNode());
    }

    // // // asDouble()
    
    // from Integers

    @Test
    public void asDoubleFromNumberIntOk()
    {
        final double ONE_D = (double) 1;

        _assertAsDouble(ONE_D, NODES.numberNode((byte) 1));
        _assertAsDouble((double)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertAsDouble((double)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertAsDouble(ONE_D, NODES.numberNode((short) 1));
        _assertAsDouble((double)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertAsDouble((double)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertAsDouble(ONE_D, NODES.numberNode(1));
        _assertAsDouble((double) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertAsDouble((double) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));
        
        _assertAsDouble(ONE_D, NODES.numberNode(1L));
        _assertAsDouble((double) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE));
        _assertAsDouble((double) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE));

        _assertAsDouble(ONE_D, NODES.numberNode(BigInteger.valueOf(1)));
        _assertAsDouble((double) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertAsDouble((double) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void asDoubleFailFromNumberIntRange() {
        // Can only fail for underflow/overflow: and that only for / BigInteger
        // (neither Integer nor Long is outside of range of even Float).

        final BigInteger tooBig = BigInteger.TEN.pow(310);      
        final BigInteger tooSmall = tooBig.negate();

        _assertAsDoubleFailForValueRange(NODES.numberNode(tooBig));
        _assertAsDoubleFailForValueRange(NODES.numberNode(tooSmall));
    }

    // Numbers/FPs

    @Test
    public void asDoubleFromNumberFPOk()
    {
        _assertAsDouble(1.0, NODES.numberNode(1.0f));
        _assertAsDouble(100_000.0, NODES.numberNode(100_000.0f));
        _assertAsDouble(-100_000.0, NODES.numberNode(-100_000.0f));

        _assertAsDouble(1.0, NODES.numberNode(1.0d));
        _assertAsDouble(100_000.0, NODES.numberNode(100_000.0d));
        _assertAsDouble(-100_000.0, NODES.numberNode(-100_000.0d));

        _assertAsDouble(1.0,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertAsDouble((double) Long.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MIN_VALUE)));
        _assertAsDouble((double) Long.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MAX_VALUE)));
    }

    @Test
    public void asDoubleFromNumberFPRangeFail()
    {
        // Can only fail from BigDecimal (similar to ints vs BigInteger)

        final BigDecimal tooBig = new BigDecimal(BigInteger.TEN.pow(310))
                .add(BigDecimal.valueOf(0.125));
        final BigDecimal tooSmall = tooBig.negate();

        _assertAsDoubleFailForValueRange(NODES.numberNode(tooBig));
        _assertAsDoubleFailForValueRange(NODES.numberNode(tooSmall));
    }

    // from non-Numeric types

    @Test
    public void asDoubleFromNonNumberScalar()
    {
        // First, failing cases:

        _assertAsDoubleFailForNonNumber(NODES.booleanNode(true));
        _assertAsDoubleFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertAsDoubleFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertAsDoubleFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertAsDoubleFailForNonNumber(NODES.stringNode("abc"),
                "not a valid String representation of `double`");
        _assertAsDoubleFailForNonNumber(NODES.pojoNode(new BigDecimal(BigInteger.TEN.pow(310))));

        // Then passing ones:
        _assertAsDouble(0.5d, NODES.stringNode("0.5"));
        _assertAsDouble(2.5d, NODES.pojoNode(2.5d));
        _assertAsDouble(1e40, NODES.pojoNode(1e40));
    }

    @Test
    public void asDoubleFromStructuralFail()
    {
        _assertAsDoubleFailForNonNumber(NODES.arrayNode(3));
        _assertAsDoubleFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void asDoubleFromMiscOther()
    {
        // Null node converts to 0.0d; missing fails
        _assertAsDouble((double) 0, NODES.nullNode());

        _assertAsDoubleFailForNonNumber(NODES.missingNode());
    }

    // // // Shared helper methods

    private void _assertDoubleValue(double expected, JsonNode node) {
        assertEquals(expected, node.doubleValue());

        // and defaults
        assertEquals(expected, node.doubleValue(-9999.5));
        assertEquals(expected, node.doubleValueOpt().getAsDouble());
    }

    private void _assertDoubleValueFailForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.doubleValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 64-bit `double` range");

        assertEquals(-2.25d, node.doubleValue(-2.25d));
        assertEquals(OptionalDouble.empty(), node.doubleValueOpt());
    }

    private void _assertDoubleValueFailForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.doubleValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");

        assertEquals(1.5d, node.doubleValue(1.5d));
        assertEquals(OptionalDouble.empty(), node.doubleValueOpt());
    }

    private void _assertAsDouble(double expected, JsonNode node) {
        assertEquals(expected, node.asDouble());

        // and defaults
        assertEquals(expected, node.asDouble(-9999.5));
        assertEquals(expected, node.asDoubleOpt().getAsDouble());
    }

    private void _assertAsDoubleFailForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asDouble(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asDouble()")
            .contains("cannot convert value")
            .contains("value not in 64-bit `double` range");

        assertEquals(-2.25d, node.asDouble(-2.25d));
        assertEquals(OptionalDouble.empty(), node.asDoubleOpt());
    }

    private void _assertAsDoubleFailForNonNumber(JsonNode node) {
        _assertAsDoubleFailForNonNumber(node, "value type not numeric");
    }

    private void _assertAsDoubleFailForNonNumber(JsonNode node, String extraMatch) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asDouble(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asDouble()")
            .contains("cannot convert value")
            .contains(extraMatch);

        assertEquals(1.5d, node.asDouble(1.5d));
        assertEquals(OptionalDouble.empty(), node.asDoubleOpt());
    }

    // Just for manual verification
    /*
    public static void main(String[] args) {
        System.out.println("Long.MIN == "+Long.MIN_VALUE);
        System.out.println("Long.MAX == "+Long.MAX_VALUE);

        System.out.println("Long.MIN -> double == "+(double) Long.MIN_VALUE);
        System.out.println("Long.MAX -> double == "+(double) Long.MAX_VALUE);

        System.out.println("Long.MIN -> float == "+(float) Long.MIN_VALUE);
        System.out.println("Long.MAX -> float == "+(float) Long.MAX_VALUE);
    }
    */
}
