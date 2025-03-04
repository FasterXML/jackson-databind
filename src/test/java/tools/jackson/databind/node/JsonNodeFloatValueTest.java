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
        final float ONE_D = 1.0f;

        // Then other integer types
        assertEquals(ONE_D, NODES.numberNode((byte) 1).floatValue());
        assertEquals((float)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).floatValue());
        assertEquals((float)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).floatValue());

        assertEquals(ONE_D, NODES.numberNode((short) 1).floatValue());
        assertEquals((float)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).floatValue());
        assertEquals((float)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).floatValue());

        assertEquals(ONE_D, NODES.numberNode(1).floatValue());
        assertEquals((float) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).floatValue());
        assertEquals((float) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).floatValue());
        
        assertEquals(ONE_D, NODES.numberNode(1L).floatValue());
        assertEquals((float) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE).floatValue());
        assertEquals((float) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE).floatValue());

        assertEquals(ONE_D, NODES.numberNode(BigInteger.valueOf(1)).floatValue());
        assertEquals((float) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)).floatValue());
        assertEquals((float) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)).floatValue());
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
        assertEquals(1.0f, NODES.numberNode(1.0f).floatValue());
        assertEquals(100_000.25f, NODES.numberNode(100_000.25f).floatValue());
        assertEquals(-100_000.25f, NODES.numberNode(-100_000.25f).floatValue());

        assertEquals(1.0f, NODES.numberNode(1.0d).floatValue());
        assertEquals(100_000.25f, NODES.numberNode(100_000.25d).floatValue());
        assertEquals(-100_000.25f, NODES.numberNode(-100_000.25d).floatValue());

        assertEquals(1.25f,
                NODES.numberNode(BigDecimal.valueOf(1.25d)).floatValue());
        assertEquals((float) Long.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MIN_VALUE)).floatValue());
        assertEquals((float) Long.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MAX_VALUE)).floatValue());
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

    // // // Shared helper methods

    private void _assertFailFloatForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.floatValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 32-bit `float` range");
    }

    private void _assertFailFloatForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.floatValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");
    }
}
