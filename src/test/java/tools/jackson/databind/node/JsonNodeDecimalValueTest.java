package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#4958], JsonNode.decimalValue() (and related) parts
 * over all types.
 */
public class JsonNodeDecimalValueTest
    extends NodeTestBase
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    private final BigDecimal BD_ONE = BigDecimal.ONE;
    private final BigDecimal BD_ONE_O = new BigDecimal("1.0");

    private final BigDecimal BD_DEFAULT = bigDec(12.125);

    // // // decimalValue() tests
    
    // decimalValue() + Numbers/Integers

    @Test
    public void decimalValueFromNumberIntOk()
    {
        _assertDecimalValue(BD_ONE, NODES.numberNode((byte) 1));
        _assertDecimalValue(bigDec(Byte.MIN_VALUE), NODES.numberNode(Byte.MIN_VALUE));
        _assertDecimalValue(bigDec(Byte.MAX_VALUE), NODES.numberNode(Byte.MAX_VALUE));

        _assertDecimalValue(BD_ONE, NODES.numberNode((short) 1));
        _assertDecimalValue(bigDec(Short.MIN_VALUE), NODES.numberNode(Short.MIN_VALUE));
        _assertDecimalValue(bigDec(Short.MAX_VALUE), NODES.numberNode(Short.MAX_VALUE));

        _assertDecimalValue(BD_ONE, NODES.numberNode(1));
        _assertDecimalValue(bigDec(Integer.MIN_VALUE), NODES.numberNode(Integer.MIN_VALUE));
        _assertDecimalValue(bigDec(Integer.MAX_VALUE), NODES.numberNode(Integer.MAX_VALUE));

        _assertDecimalValue(BD_ONE, NODES.numberNode(1L));
        _assertDecimalValue(bigDec(Long.MIN_VALUE), NODES.numberNode(Long.MIN_VALUE));
        _assertDecimalValue(bigDec(Long.MAX_VALUE), NODES.numberNode(Long.MAX_VALUE));

        _assertDecimalValue(BD_ONE, NODES.numberNode(BigInteger.valueOf(1)));
        _assertDecimalValue(bigDec(Long.MIN_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertDecimalValue(bigDec(Long.MAX_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    // Cannot fail for Over/Underflow from Integer values
    //@Test public void failBigDecimalValueFromNumberIntRange() { }

    // decimalValue() + Numbers/FPs

    @Test
    public void decimalValueFromNumberFPOk()
    {
        _assertDecimalValue(BD_ONE_O, NODES.numberNode(1.0f));

        _assertDecimalValue(BD_ONE_O, NODES.numberNode(1.0f));
        _assertDecimalValue(bigDec("100000.0"), NODES.numberNode(100_000.0f));
        _assertDecimalValue(bigDec("-100000.0"), NODES.numberNode(-100_000.0f));

        _assertDecimalValue(BD_ONE_O, NODES.numberNode(1.0d));
        _assertDecimalValue(bigDec("100000.0"), NODES.numberNode(100_000.0d));
        _assertDecimalValue(bigDec("-100000.0"), NODES.numberNode(-100_000.0d));

        _assertDecimalValue(new BigDecimal("100.001"),
                NODES.numberNode(new BigDecimal("100.001")));
    }

    // Cannot fail for Over/Underflow from FP values either
    //@Test public void failBigDecimalFromNumberFPRange() { }

    // But can fail for NaN

    @Test
    public void decimalValueFromNumberFPFail()
    {
        _assertFailDecimalValueForNaN(NODES.numberNode(Float.NaN));
        _assertFailDecimalValueForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));
        _assertFailDecimalValueForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));

        _assertFailDecimalValueForNaN(NODES.numberNode(Double.NaN));
        _assertFailDecimalValueForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
        _assertFailDecimalValueForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
    }

    // decimalValue() + non-Numeric types

    @Test
    public void failBigDecimalFromNonNumberScalar()
    {
        _assertFailDecimalValueForNonNumber(NODES.booleanNode(true));
        _assertFailDecimalValueForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailDecimalValueForNonNumber(NODES.stringNode("123"));
        _assertFailDecimalValueForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailDecimalValueForNonNumber(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void failBigDecimalValueFromMiscOther()
    {
        _assertFailDecimalValueForNonNumber(NODES.nullNode());
        _assertFailDecimalValueForNonNumber(NODES.missingNode());
    }

    @Test
    public void failBigDecimalValueFromStructural()
    {
        _assertFailDecimalValueForNonNumber(NODES.arrayNode(3));
        _assertFailDecimalValueForNonNumber(NODES.objectNode());
    }

    // // // asDecimal() tests

    @Test
    public void asDecimalFromNumberIntOk()
    {
        _assertAsDecimal(BD_ONE, NODES.numberNode((byte) 1));
        _assertAsDecimal(bigDec(Byte.MIN_VALUE), NODES.numberNode(Byte.MIN_VALUE));
        _assertAsDecimal(bigDec(Byte.MAX_VALUE), NODES.numberNode(Byte.MAX_VALUE));

        _assertAsDecimal(BD_ONE, NODES.numberNode((short) 1));
        _assertAsDecimal(bigDec(Short.MIN_VALUE), NODES.numberNode(Short.MIN_VALUE));
        _assertAsDecimal(bigDec(Short.MAX_VALUE), NODES.numberNode(Short.MAX_VALUE));

        _assertAsDecimal(BD_ONE, NODES.numberNode(1));
        _assertAsDecimal(bigDec(Integer.MIN_VALUE), NODES.numberNode(Integer.MIN_VALUE));
        _assertAsDecimal(bigDec(Integer.MAX_VALUE), NODES.numberNode(Integer.MAX_VALUE));

        _assertAsDecimal(BD_ONE, NODES.numberNode(1L));
        _assertAsDecimal(bigDec(Long.MIN_VALUE), NODES.numberNode(Long.MIN_VALUE));
        _assertAsDecimal(bigDec(Long.MAX_VALUE), NODES.numberNode(Long.MAX_VALUE));

        _assertAsDecimal(BD_ONE, NODES.numberNode(BigInteger.valueOf(1)));
        _assertAsDecimal(bigDec(Long.MIN_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)));
        _assertAsDecimal(bigDec(Long.MAX_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void asDecimalFromNumberFPOk()
    {
        _assertAsDecimal(BD_ONE_O, NODES.numberNode(1.0f));

        _assertAsDecimal(BD_ONE_O, NODES.numberNode(1.0f));
        _assertAsDecimal(bigDec("100000.0"), NODES.numberNode(100_000.0f));
        _assertAsDecimal(bigDec("-100000.0"), NODES.numberNode(-100_000.0f));

        _assertAsDecimal(BD_ONE_O, NODES.numberNode(1.0d));
        _assertAsDecimal(bigDec("100000.0"), NODES.numberNode(100_000.0d));
        _assertAsDecimal(bigDec("-100000.0"), NODES.numberNode(-100_000.0d));

        _assertAsDecimal(new BigDecimal("100.001"),
                NODES.numberNode(new BigDecimal("100.001")));
    }

    // Cannot fail for Over/Underflow from FP values either

    // But can fail for NaN

    @Test
    public void asDecimalFromNumberFPFail()
    {
        _assertFailAsDecimalForNaN(NODES.numberNode(Float.NaN));
        _assertFailAsDecimalForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));
        _assertFailAsDecimalForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));

        _assertFailAsDecimalForNaN(NODES.numberNode(Double.NaN));
        _assertFailAsDecimalForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
        _assertFailAsDecimalForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
    }

    // asDecimal() + non-Numeric types

    @Test
    public void asDecimalFromNonNumberScalar()
    {
        // Regular failing cases
        _assertFailAsDecimalForNonNumber(NODES.booleanNode(true));
        _assertFailAsDecimalForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailAsDecimalForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailAsDecimalForNonNumber(NODES.pojoNode(Boolean.TRUE));

        // Special failing cases:
        _assertFailAsDecimal(NODES.stringNode("abc"),
                "value not a valid String representation of `BigDecimal`");


        // Passing cases
        _assertAsDecimal(BigDecimal.valueOf(2), NODES.stringNode("2"));
        _assertAsDecimal(BigDecimal.TEN, NODES.pojoNode(10));
    }

    @Test
    public void asDecimalFailFromStructural()
    {
        _assertFailAsDecimalForNonNumber(NODES.arrayNode(3));
        _assertFailAsDecimalForNonNumber(NODES.objectNode());
    }

    @Test
    public void asDecimalFromMiscOther()
    {
        // "null" becomes "0.0"
        _assertAsDecimal(BigDecimal.ZERO, NODES.nullNode());

        // but "missing" still fails
        _assertFailAsDecimalForNonNumber(NODES.missingNode());
    }
    
    // // // Shared helper methods

    private void _assertDecimalValue(BigDecimal expected, JsonNode fromNode)
    {
        // main accessor
        assertEquals(expected, fromNode.decimalValue());

        // but also defaulting
        assertEquals(expected, fromNode.decimalValue(BD_DEFAULT));
        assertEquals(expected, fromNode.decimalValueOpt().get());
    }

    private void _assertAsDecimal(BigDecimal expected, JsonNode fromNode)
    {
        // main accessor
        assertEquals(expected, fromNode.asDecimal());

        // but also defaulting
        assertEquals(expected, fromNode.asDecimal(BD_DEFAULT));
        assertEquals(expected, fromNode.asDecimalOpt().get());
    }

    private void _assertFailDecimalValueForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.decimalValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("decimalValue()")
            .contains("cannot convert value")
            .contains("value type not numeric");

        // Verify default value handling
        assertEquals(BD_DEFAULT, node.decimalValue(BD_DEFAULT));
        assertEquals(Optional.empty(), node.decimalValueOpt());
    }

    private void _assertFailDecimalValueForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.decimalValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("decimalValue()")
            .contains("cannot convert value")
            .contains("value non-Finite ('NaN')");

        // Verify default value handling
        assertEquals(BD_DEFAULT, node.decimalValue(BD_DEFAULT));
        assertEquals(Optional.empty(), node.decimalValueOpt());
    }

    private void _assertFailAsDecimalForNonNumber(JsonNode node) {
        _assertFailAsDecimal(node, "value type not coercible to `BigDecimal`");
    }

    private void _assertFailAsDecimal(JsonNode node, String extraFailMsg) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asDecimal(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("asDecimal()")
            .contains("cannot convert value")
            .contains(extraFailMsg);

        // Verify default value handling
        assertEquals(BD_DEFAULT, node.asDecimal(BD_DEFAULT));
        assertEquals(Optional.empty(), node.asDecimalOpt());
    }

    private void _assertFailAsDecimalForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asDecimal(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
        .contains("asDecimal()")
            .contains("cannot convert value")
            .contains("value non-Finite ('NaN')");

        // Verify default value handling
        assertEquals(BD_DEFAULT, node.asDecimal(BD_DEFAULT));
        assertEquals(Optional.empty(), node.asDecimalOpt());
    }
    
    protected static Optional<BigDecimal> bigDecOpt(BigDecimal bigDec) {
        return Optional.of(bigDec);
    }
}
