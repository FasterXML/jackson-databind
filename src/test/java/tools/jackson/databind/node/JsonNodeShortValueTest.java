package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

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
public class JsonNodeShortValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // shortValue()

    @Test
    public void shortValueFromNumberIntOk()
    {
        final short SHORT_1 = (short) 1;
        final short MIN_SHORT = Short.MIN_VALUE;
        final short MAX_SHORT = Short.MAX_VALUE;
        
        // First safe from `short`
        _assertShortValue(SHORT_1, NODES.numberNode((short) 1));
        _assertShortValue(Short.MIN_VALUE, NODES.numberNode(MIN_SHORT));
        _assertShortValue(Short.MAX_VALUE, NODES.numberNode(MAX_SHORT));

        // Then other integer types

        _assertShortValue(SHORT_1, NODES.numberNode((byte) 1));
        _assertShortValue(Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertShortValue(Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertShortValue(SHORT_1, NODES.numberNode(1));
        _assertShortValue(MIN_SHORT, NODES.numberNode((int) MIN_SHORT));
        _assertShortValue(MAX_SHORT, NODES.numberNode((int) MAX_SHORT));

        _assertShortValue(SHORT_1, NODES.numberNode(1L));
        _assertShortValue(MIN_SHORT, NODES.numberNode((long) MIN_SHORT));
        _assertShortValue(MAX_SHORT, NODES.numberNode((long) MAX_SHORT));

        _assertShortValue(SHORT_1, NODES.numberNode(BigInteger.valueOf(1)));
        _assertShortValue(MIN_SHORT, NODES.numberNode(BigInteger.valueOf(MIN_SHORT)));
        _assertShortValue(MAX_SHORT, NODES.numberNode(BigInteger.valueOf(MAX_SHORT)));
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
        _assertShortValue((short) 1, NODES.numberNode(1.0f));
        _assertShortValue((short) 10_000, NODES.numberNode(10_000.0f));
        _assertShortValue((short) -10_000, NODES.numberNode(-10_000.0f));

        _assertShortValue((short) 1, NODES.numberNode(1.0d));
        _assertShortValue((short) 10_000, NODES.numberNode(10_000.0d));
        _assertShortValue((short) -10_000, NODES.numberNode(-10_000.0d));
        _assertShortValue(Short.MIN_VALUE, NODES.numberNode((double) Short.MIN_VALUE));
        _assertShortValue(Short.MAX_VALUE, NODES.numberNode((double) Short.MAX_VALUE));

        _assertShortValue((short) 1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertShortValue(Short.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Short.MIN_VALUE)));
        _assertShortValue(Short.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Short.MAX_VALUE)));
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
        _assertFailShortForNonNumber(NODES.pojoNode((short) 456));
    }

    // // // asShort()

    // Numbers/Integers

    @Test
    public void asShortFromNumberIntOk()
    {
        // First safe from `short`
        _assertAsShort((short) 1, NODES.numberNode((short) 1));
        _assertAsShort(Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE));
        _assertAsShort(Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE));

        // Then other integer types
        _assertAsShort((short) 1, NODES.numberNode((byte) 1));
        _assertAsShort(Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE));
        _assertAsShort(Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE));

        _assertAsShort((short) 1, NODES.numberNode(1));
        _assertAsShort(Short.MIN_VALUE, NODES.numberNode((int) Short.MIN_VALUE));
        _assertAsShort(Short.MAX_VALUE, NODES.numberNode((int) Short.MAX_VALUE));

        _assertAsShort((short) 1, NODES.numberNode(1L));
        _assertAsShort(Short.MIN_VALUE, NODES.numberNode((long) Short.MIN_VALUE));
        _assertAsShort(Short.MAX_VALUE, NODES.numberNode((long) Short.MAX_VALUE));

        _assertAsShort((short) 1, NODES.numberNode(BigInteger.valueOf(1)));
        _assertAsShort(Short.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Short.MIN_VALUE)));
        _assertAsShort(Short.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Short.MAX_VALUE)));
    }

    @Test
    public void asShortFromNumberIntFailRange() {
        // Can only fail for underflow/overflow: and that only for Integer / Long / BigInteger
        final int underflow = -1 + Short.MIN_VALUE;
        final long overflow = +1L + Short.MAX_VALUE;

        _assertAsShortFailForValueRange(NODES.numberNode(underflow));
        _assertAsShortFailForValueRange(NODES.numberNode(overflow));

        _assertAsShortFailForValueRange(NODES.numberNode(BigInteger.valueOf(underflow)));
        _assertAsShortFailForValueRange(NODES.numberNode(BigInteger.valueOf(overflow)));
    }

    //  Numbers/FPs

    @Test
    public void asShortFromNumberFPOk()
    {
        _assertAsShort((short) 1, NODES.numberNode(1.0f));
        _assertAsShort((short) 100, NODES.numberNode(100.0f));
        _assertAsShort((short) -100, NODES.numberNode(-100.0f));

        _assertAsShort((short) 1, NODES.numberNode(1.0d));
        _assertAsShort((short) 100, NODES.numberNode(100.0d));
        _assertAsShort((short) -100, NODES.numberNode(-100.0d));
        _assertAsShort(Short.MIN_VALUE, NODES.numberNode((double) Short.MIN_VALUE));
        _assertAsShort(Short.MAX_VALUE, NODES.numberNode((double) Short.MAX_VALUE));

        _assertAsShort((short) 1,
                NODES.numberNode(BigDecimal.valueOf(1.0d)));
        _assertAsShort(Short.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Short.MIN_VALUE)));
        _assertAsShort(Short.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Short.MAX_VALUE)));
    }

    @Test
    public void asShortFromNumberFPFailRange()
    {
        // Can only fail for underflow/overflow: and that only for Long / BigInteger
        final long underflow = Short.MIN_VALUE - 1L;
        final long overflow =  Short.MAX_VALUE + 1L;

        _assertAsShortFailForValueRange(NODES.numberNode((float)underflow));
        _assertAsShortFailForValueRange(NODES.numberNode((double)overflow));

        // Float is too inexact for using same test as Double, so:

        _assertAsShortFailForValueRange(NODES.numberNode(-Float.MAX_VALUE));
        _assertAsShortFailForValueRange(NODES.numberNode(Float.MAX_VALUE));

        _assertAsShortFailForValueRange(NODES.numberNode(BigDecimal.valueOf(underflow)));
        _assertAsShortFailForValueRange(NODES.numberNode(BigDecimal.valueOf(overflow)));
    }

    @Test
    public void asShortFromNumberFPWithFraction()
    {
        _assertAsShort((short) 100, NODES.numberNode(100.75f));
        _assertAsShort((short) -1, NODES.numberNode(-1.25f));

        _assertAsShort((short) 100, NODES.numberNode(100.75d));
        _assertAsShort((short) -1, NODES.numberNode(-1.25d));

        _assertAsShort((short) 100, NODES.numberNode(BigDecimal.valueOf(100.75d)));
        _assertAsShort((short) -1, NODES.numberNode(BigDecimal.valueOf(-1.25d)));
    }

    @Test
    public void asIntFromNumberFPFailNaN()
    {
        _assertAsShortFailForNaN(NODES.numberNode(Float.NaN));
        _assertAsShortFailForNaN(NODES.numberNode(Float.NEGATIVE_INFINITY));
        _assertAsShortFailForNaN(NODES.numberNode(Float.POSITIVE_INFINITY));

        _assertAsShortFailForNaN(NODES.numberNode(Double.NaN));
        _assertAsShortFailForNaN(NODES.numberNode(Double.NEGATIVE_INFINITY));
        _assertAsShortFailForNaN(NODES.numberNode(Double.POSITIVE_INFINITY));
    }

    // non-Numeric types

    @Test
    public void asShortFromNonNumberScalar()
    {
        // Some fail:
        _assertAsShortFailForNonNumber(NODES.booleanNode(true));
        _assertAsShortFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertAsShortFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertAsShortFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertAsShortFailForNonNumber(NODES.stringNode("abc"),
                "value not a valid String representation of `short`");
        _assertAsShortFailForNonNumber(NODES.pojoNode("123456"));

        // Some pass:
        _assertAsShort((short) 123, NODES.stringNode("123"));
        _assertAsShort((short) 456, NODES.pojoNode(456));
        _assertAsShort((short) 789, NODES.pojoNode(BigInteger.valueOf(789)));
    }

    @Test
    public void asIntFromStructuralFail()
    {
        _assertAsShortFailForNonNumber(NODES.arrayNode(3));
        _assertAsShortFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void asIntFromMiscOther()
    {
        // NullNode -> 0 but "missing" still fails
        _assertAsShort((short) 0, NODES.nullNode());

        _assertAsShortFailForNonNumber(NODES.missingNode());
    }


    // // // Shared helper methods: shortValue()

    private void _assertShortValue(short expected, JsonNode node) {
        assertEquals(expected, node.shortValue());

        // and defaulting
        assertEquals(expected, node.shortValue((short) 99));
    }

    // // // Shared helper methods

    private void _assertFailShortForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.shortValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 16-bit `short` range");

        // assert defaulting
        assertEquals(99, node.shortValue((short) 99));
    }

    private void _assertFailShortValueForFraction(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.shortValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("to `short`: value has fractional part");

        // assert defaulting
        assertEquals(99, node.shortValue((short) 99));
    }

    private void _assertFailShortForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.shortValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");

        // assert defaulting
        assertEquals(99, node.shortValue((short) 99));
    }

    // // // Shared helper methods: asShort()

    private void _assertAsShort(short expected, JsonNode node) {
        assertEquals(expected, node.asShort());

        // and defaulting
        assertEquals(expected, node.asShort((short) 99));
    }

    private void _assertAsShortFailForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asShort(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
                .contains("asShort()")
                .contains("cannot convert value")
                .contains("value not in 16-bit `short` range");

        // assert defaulting
        assertEquals(99, node.asShort((short) 99));
    }

    private void _assertAsShortFailForNonNumber(JsonNode node) {
        _assertAsShortFailForNonNumber(node, "value type not numeric");
    }

    private void _assertAsShortFailForNonNumber(JsonNode node, String extraFailMsg) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asShort(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
                .contains("asShort()")
                .contains("cannot convert value")
                .contains(extraFailMsg);

        // assert defaulting
        assertEquals(99, node.asShort((short) 99));
    }

    private void _assertAsShortFailForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asShort(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
                .contains("asShort()")
                .contains("cannot convert value")
                .contains("value non-Finite");

        // Verify default value handling
        assertEquals(99, node.asShort((short) 99));
    }


}
