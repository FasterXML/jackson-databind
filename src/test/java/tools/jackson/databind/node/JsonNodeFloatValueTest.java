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
 * Tests for [databind#4958], JsonNode.floatValue() (and related) parts
 * over all types.
 */
public class JsonNodeFloatValueTest
    extends NodeTestBase
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // floatValue() + Numbers/Integers

    @Test
    public void floatValueFromNumberIntOk()
    {
        final float ONE_F = 1.0f;

        // Then other integer types
        _assertFloatValue(ONE_F, NODES.numberNode((byte) 1));
        _assertFloatValue((float)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertFloatValue((float)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertFloatValue(ONE_F, NODES.numberNode((short) 1));
        _assertFloatValue((float)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertFloatValue((float)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertFloatValue(ONE_F, NODES.numberNode(1));
        _assertFloatValue((float) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertFloatValue((float) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        _assertFloatValue(ONE_F, NODES.numberNode(1L));
        _assertFloatValue((float) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE));
        _assertFloatValue((float) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE));

        _assertFloatValue(ONE_F, NODES.numberNode(BigInteger.valueOf(1)));
        _assertFloatValue((float) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertFloatValue((float) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void failfloatValueFromNumberIntRange() {
        // Can only fail for underflow/overflow: and that only for / BigInteger
        // (neither Integer nor Long is outside of range Float).

        final BigInteger tooBig = BigInteger.TEN.pow(310);      
        final BigInteger tooSmall = tooBig.negate();
        
        _assertFailFloatForValueRange(NODES.numberNode(tooBig));
        _assertFailFloatForValueRange(NODES.numberNode(tooSmall));
    }

    // // // floatValue() + Numbers/FPs

    @Test
    public void floatValueFromNumberFPOk()
    {
        _assertFloatValue(1.0f, NODES.numberNode(1.0f));
        _assertFloatValue(100_000.25f, NODES.numberNode(100_000.25f));
        _assertFloatValue(-100_000.25f, NODES.numberNode(-100_000.25f));

        _assertFloatValue(1.0f, NODES.numberNode(1.0d));
        _assertFloatValue(100_000.25f, NODES.numberNode(100_000.25d));
        _assertFloatValue(-100_000.25f, NODES.numberNode(-100_000.25d));

        _assertFloatValue(1.25f,
                NODES.numberNode(BigDecimal.valueOf(1.25d)));
        _assertFloatValue((float) Long.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MIN_VALUE)));
        _assertFloatValue((float) Long.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MAX_VALUE)));
    }

    @Test
    public void failFloatValueFromNumberFPRange()
    {
        final double tooBigD = 1e40; // 10^40, larger than Float.MAX_VALUE
        final double tooSmallD = -tooBigD;
        
        _assertFailFloatForValueRange(NODES.numberNode(tooBigD));
        _assertFailFloatForValueRange(NODES.numberNode(tooSmallD));

        // and similarly for BigDecimal
        final BigDecimal tooBigDec = new BigDecimal(BigInteger.TEN.pow(50))
                .add(BigDecimal.valueOf(0.125));
        final BigDecimal tooSmallDec = tooBigDec.negate();

        _assertFailFloatForValueRange(NODES.numberNode(tooBigDec));
        _assertFailFloatForValueRange(NODES.numberNode(tooSmallDec));
    }

    // // // floatValue() + non-Numeric types

    @Test
    public void failFloatValueFromNonNumberScalar()
    {
        _assertFailFloatForNonNumber(NODES.booleanNode(true));
        _assertFailFloatForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailFloatForNonNumber(NODES.stringNode("123"));
        _assertFailFloatForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailFloatForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertFailFloatForNonNumber(NODES.pojoNode(3.8f));
    }

    @Test
    public void failFloatValueFromStructural()
    {
        _assertFailFloatForNonNumber(NODES.arrayNode(3));
        _assertFailFloatForNonNumber(NODES.objectNode());
    }

    @Test
    public void failFloatValueFromMiscOther()
    {
        _assertFailFloatForNonNumber(NODES.nullNode());
        _assertFailFloatForNonNumber(NODES.missingNode());
    }

    // // // asFloat()

    // from Integers

    @Test
    public void asFloatFromNumberIntOk()
    {
        final float ONE_F = (float) 1;

        _assertAsFloat(ONE_F, NODES.numberNode((byte) 1));
        _assertAsFloat((float)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertAsFloat((float)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertAsFloat(ONE_F, NODES.numberNode((short) 1));
        _assertAsFloat((float)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertAsFloat((float)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        _assertAsFloat(ONE_F, NODES.numberNode(1));
        _assertAsFloat((float) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE));
        _assertAsFloat((float) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE));

        _assertAsFloat(ONE_F, NODES.numberNode(1L));
        _assertAsFloat((float) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE));
        _assertAsFloat((float) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE));

        _assertAsFloat(ONE_F, NODES.numberNode(BigInteger.valueOf(1)));
        _assertAsFloat((float) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertAsFloat((float) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void asFloatFailFromNumberIntRange() {
        // Can only fail for underflow/overflow: and that only for / BigInteger
        // (neither Integer nor Long is outside of range of even Float).

        final BigInteger tooBig = BigInteger.TEN.pow(310);
        final BigInteger tooSmall = tooBig.negate();

        _assertAsFloatFailForValueRange(NODES.numberNode(tooBig));
        _assertAsFloatFailForValueRange(NODES.numberNode(tooSmall));
    }

    // Numbers/FPs

    @Test
    public void asFloatFromNumberFPOk()
    {
        _assertAsFloat(1.0f, NODES.numberNode(1.0f));
        _assertAsFloat(100_000.0f, NODES.numberNode(100_000.0f));
        _assertAsFloat(-100_000.0f, NODES.numberNode(-100_000.0f));

        _assertAsFloat(1.0f, NODES.numberNode(1.0d));
        _assertAsFloat(100_000.0f, NODES.numberNode(100_000.0d));
        _assertAsFloat(-100_000.0f, NODES.numberNode(-100_000.0d));

        _assertAsFloat(1.0f,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertAsFloat((float) Long.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((float) Long.MIN_VALUE)));
        _assertAsFloat((float) Long.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((float) Long.MAX_VALUE)));
    }

    @Test
    public void asFloatFromNumberFPRangeFail()
    {
        // Can only fail from BigDecimal (similar to ints vs BigInteger)

        final BigDecimal tooBig = new BigDecimal(BigInteger.TEN.pow(310))
                .add(BigDecimal.valueOf(0.125));
        final BigDecimal tooSmall = tooBig.negate();

        _assertAsFloatFailForValueRange(NODES.numberNode(tooBig));
        _assertAsFloatFailForValueRange(NODES.numberNode(tooSmall));
    }

    // from non-Numeric types

    @Test
    public void asFloatFromNonNumberScalar()
    {
        // First, failing cases:
        _assertAsFloatFailForNonNumber(NODES.booleanNode(true));
        _assertAsFloatFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertAsFloatFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertAsFloatFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertAsFloatFailForNonNumber(NODES.stringNode("abc"),
                "not a valid String representation of `float`");
        _assertAsFloatFailForValueRange(NODES.pojoNode(1e40));

        // Then passing ones:
        _assertAsFloat(2.5f, NODES.pojoNode(2.5f));
        _assertAsFloat(0.5f, NODES.stringNode("0.5"));
    }

    @Test
    public void asFloatFromStructuralFail()
    {
        _assertAsFloatFailForNonNumber(NODES.arrayNode(3));
        _assertAsFloatFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void asFloatFromMiscOther()
    {
        // Null node converts to 0.0f; missing fails
        _assertAsFloat((float) 0, NODES.nullNode());
        _assertAsFloatFailForNonNumber(NODES.missingNode());
    }


    // // // Shared helper methods

    private void _assertFloatValue(float expected, JsonNode node) {
        assertEquals(expected, node.floatValue());

        // and defaults
        assertEquals(expected, node.floatValue(-9999.5f));
    }

    private void _assertFailFloatForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.floatValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 32-bit `float` range");

        assertEquals(-2.25f, node.floatValue(-2.25f));
    }

    private void _assertFailFloatForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.floatValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");

        assertEquals(-2.25f, node.floatValue(-2.25f));
    }

    private void _assertAsFloat(float expected, JsonNode node) {
        assertEquals(expected, node.asFloat());

        // and defaults
        assertEquals(expected, node.asFloat(-9999.5f));
    }

    private void _assertAsFloatFailForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asFloat(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
                .contains("asFloat()")
                .contains("cannot convert value")
                .contains("value not in 32-bit `float` range");

        assertEquals(-2.25f, node.asFloat(-2.25f));
    }

    private void _assertAsFloatFailForNonNumber(JsonNode node) {
        _assertAsFloatFailForNonNumber(node, "value type not numeric");
    }

    private void _assertAsFloatFailForNonNumber(JsonNode node, String extraMatch) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asFloat(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
                .contains("asFloat()")
                .contains("cannot convert value")
                .contains(extraMatch);

        assertEquals(1.5f, node.asFloat(1.5f));
    }

}
