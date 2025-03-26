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
 * Tests for [databind#5003], JsonNode.bigIntegerValue() (and related) parts
 * over all types.
 */
public class JsonNodeBigIntegerValueTest
    extends NodeTestBase
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // bigIntegerValue() tests

    @Test
    public void bigIntegerValueFromNumberIntOk()
    {
        // Integer types, byte/short/int/long/BigInteger
        _assertBigIntegerValue(BigInteger.ONE, NODES.numberNode((byte) 1));
        _assertBigIntegerValue(BigInteger.ONE, NODES.numberNode((short) 1));
        _assertBigIntegerValue(BigInteger.ONE, NODES.numberNode(1));
        _assertBigIntegerValue(BigInteger.ONE, NODES.numberNode(1L));
        _assertBigIntegerValue(BigInteger.ONE, NODES.numberNode(BigInteger.ONE));
    }

    @Test
    public void bigIntegerValueFromNumberFPOk()
    {
        _assertBigIntegerValue(BigInteger.ONE, NODES.numberNode(1.0f));
        _assertBigIntegerValue(bigInt(100_000), NODES.numberNode(100_000.0f));
        _assertBigIntegerValue(bigInt(-100_000), NODES.numberNode(-100_000.0f));
        
        _assertBigIntegerValue(BigInteger.ONE, NODES.numberNode(1.0d));
        _assertBigIntegerValue(bigInt(100_000_000), NODES.numberNode(100_000_000.0d));
        _assertBigIntegerValue(bigInt(-100_000_000), NODES.numberNode(-100_000_000.0d));
    
        _assertBigIntegerValue(BigInteger.ONE,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertBigIntegerValue(bigInt(Long.MIN_VALUE),
                NODES.numberNode(new BigDecimal(Long.MIN_VALUE+".0")));
        _assertBigIntegerValue(bigInt(Long.MAX_VALUE),
                NODES.numberNode(new BigDecimal(Long.MAX_VALUE+".0")));
    }

    // NOTE: BigInteger has unlimited range so cannot fail for Under-/Overflow (hence no tests)
    // ... but there are NaNs:

    @Test
    public void bigIntegerValueFromNumberFPFailForNaN()
    {
        _assertFailBigIntegerForNaN(NODES.numberNode(Float.NaN));
        _assertFailBigIntegerForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));
        _assertFailBigIntegerForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));
        _assertFailBigIntegerForNaN(NODES.numberNode(Double.NaN));
        _assertFailBigIntegerForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
        _assertFailBigIntegerForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
    }
    
    @Test
    public void bigIntegerValueFromNumberFPFailFraction()
    {
        _assertFailBigIntegerValueForFraction(NODES.numberNode(100.5f));
        _assertFailBigIntegerValueForFraction(NODES.numberNode(-0.25f));

        _assertFailBigIntegerValueForFraction(NODES.numberNode(100.5d));
        _assertFailBigIntegerValueForFraction(NODES.numberNode(-0.25d));
        
        _assertFailBigIntegerValueForFraction(NODES.numberNode(BigDecimal.valueOf(100.5d)));
        _assertFailBigIntegerValueForFraction(NODES.numberNode(BigDecimal.valueOf(-0.25d)));
    }

    @Test
    public void bigIntegerValueFromNonNumberScalarFail()
    {
        _assertFailBigIntegerForNonNumber(NODES.booleanNode(true));
        _assertFailBigIntegerForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailBigIntegerForNonNumber(NODES.stringNode("123"));
        _assertFailBigIntegerForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailBigIntegerForNonNumber(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void bigIntegerValueFromStructuralFail()
    {
        _assertFailBigIntegerForNonNumber(NODES.arrayNode(3));
        _assertFailBigIntegerForNonNumber(NODES.objectNode());
    }

    @Test
    public void bigIntegerValueFromMiscOtherFail()
    {
        _assertFailBigIntegerForNonNumber(NODES.nullNode());
        _assertFailBigIntegerForNonNumber(NODES.missingNode());
    }

    // // // asBigInteger()

    // // // BigInteger + bigIntegerValue()

    @Test
    public void asBigIntegerFromNumberIntOk()
    {
        // Integer types, byte/short/int/long/BigInteger
        _assertAsBigInteger(BigInteger.ONE, NODES.numberNode((byte) 1));
        _assertAsBigInteger(BigInteger.ONE, NODES.numberNode((short) 1));
        _assertAsBigInteger(BigInteger.ONE, NODES.numberNode(1));
        _assertAsBigInteger(BigInteger.ONE, NODES.numberNode(1L));
        _assertAsBigInteger(BigInteger.ONE, NODES.numberNode(BigInteger.ONE));
    }

    // NOTE: BigInteger has unlimited range so cannot fail for Under-/Overflow (hence no tests)

    @Test
    public void asBigIntegerFromNumberFPOk()
    {
        _assertAsBigInteger(BigInteger.ONE, NODES.numberNode(1.0f));
        _assertAsBigInteger(bigInt(100_000), NODES.numberNode(100_000.0f));
        _assertAsBigInteger(bigInt(-100_000), NODES.numberNode(-100_000.0f));
        
        _assertAsBigInteger(BigInteger.ONE, NODES.numberNode(1.0d));
        _assertAsBigInteger(bigInt(100_000_000), NODES.numberNode(100_000_000.0d));
        _assertAsBigInteger(bigInt(-100_000_000), NODES.numberNode(-100_000_000.0d));
    
        _assertAsBigInteger(BigInteger.ONE,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertAsBigInteger(bigInt(Long.MIN_VALUE),
                NODES.numberNode(new BigDecimal(Long.MIN_VALUE+".0")));
        _assertAsBigInteger(bigInt(Long.MAX_VALUE),
                NODES.numberNode(new BigDecimal(Long.MAX_VALUE+".0")));
    }

    // NOTE: unlike with "bigIntegerValue()", fractions ok: will be rounded
    @Test
    public void asBigIntegerFromNumberFPFraction()
    {
        final BigInteger B100 = bigInt(100);
        final BigInteger B_MINUS_1 = bigInt(-1);
        
        _assertAsBigInteger(B100, NODES.numberNode(100.75f));
        _assertAsBigInteger(B_MINUS_1, NODES.numberNode(-1.25f));

        _assertAsBigInteger(B100, NODES.numberNode(100.75d));
        _assertAsBigInteger(B_MINUS_1, NODES.numberNode(-1.25d));
        
        _assertAsBigInteger(B100, NODES.numberNode(BigDecimal.valueOf(100.75d)));
        _assertAsBigInteger(B_MINUS_1, NODES.numberNode(BigDecimal.valueOf(-1.25d)));
    }

    @Test
    public void asBigIntegerFromNonNumberScalar()
    {
        // First failing cases
        _assertAsBigIntegerFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertAsBigIntegerFailForNonNumber(NODES.booleanNode(true));
        _assertAsBigIntegerFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertAsBigIntegerFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertAsBigIntegerFailForNonNumber(NODES.stringNode("E000"),
                "not valid String representation of `BigInteger`");

        // Then passing
        _assertAsBigInteger(BigInteger.TEN, NODES.pojoNode(BigInteger.TEN));
        _assertAsBigInteger(BigInteger.TEN, NODES.pojoNode(Integer.valueOf(10)));
        _assertAsBigInteger(BigInteger.TEN, NODES.pojoNode(Long.valueOf(10)));

        _assertAsBigInteger(BigInteger.TEN, NODES.stringNode("10"));
        _assertAsBigInteger(BigInteger.valueOf(-99), NODES.stringNode("-99"));
    }

    @Test
    public void asBigIntegerFromStructuralFail()
    {
        _assertAsBigIntegerFailForNonNumber(NODES.arrayNode(3));
        _assertAsBigIntegerFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void asBigIntegerFromMiscOther()
    {
        // NullNode becomes 0, not fail
        _assertAsBigInteger(BigInteger.ZERO, NODES.nullNode());

        // But MissingNode still fails
        _assertAsBigIntegerFailForNonNumber(NODES.missingNode());
    }
    
    // // // Shared helper methods

    private void _assertBigIntegerValue(BigInteger expected, JsonNode node) {
        assertEquals(expected, node.bigIntegerValue());

        // and then defaulting
        assertEquals(expected, node.bigIntegerValue(BigInteger.valueOf(9999999L)));
        assertEquals(expected, node.bigIntegerValueOpt().get());
    }

    private void _assertFailBigIntegerValueForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.bigIntegerValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("to `java.math.BigInteger`: value has fractional part");

        // Verify default value handling
        assertEquals(BigInteger.ONE, node.bigIntegerValue(BigInteger.ONE));
        assertFalse(node.bigIntegerValueOpt().isPresent());

    }

    private void _assertFailBigIntegerForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.bigIntegerValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");

        // Verify default value handling
        assertEquals(BigInteger.ONE, node.bigIntegerValue(BigInteger.ONE));
        assertFalse(node.bigIntegerValueOpt().isPresent());
    }

    private void _assertFailBigIntegerForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.bigIntegerValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value non-Finite ('NaN')");

        // Verify default value handling
        assertEquals(BigInteger.ONE, node.bigIntegerValue(BigInteger.ONE));
        assertFalse(node.bigIntegerValueOpt().isPresent());
    }

    private void _assertAsBigInteger(BigInteger expected, JsonNode node) {
        assertEquals(expected, node.asBigInteger());

        // and then defaulting
        assertEquals(expected, node.asBigInteger(BigInteger.valueOf(9999999L)));
        assertEquals(expected, node.asBigIntegerOpt().get());
    }

    private void _assertAsBigIntegerFailForNonNumber(JsonNode node) {
        _assertAsBigIntegerFailForNonNumber(node, "value type not numeric");
    }

    private void _assertAsBigIntegerFailForNonNumber(JsonNode node, String extraMsg) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asBigInteger(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value");

        // Verify default value handling
        assertEquals(BigInteger.ONE, node.asBigInteger(BigInteger.ONE));
        assertFalse(node.asBigIntegerOpt().isPresent());
    }
}
