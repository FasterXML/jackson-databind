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

    // // // doubleValue() + Numbers/Integers

    @Test
    public void doubleValueFromNumberIntOk()
    {
        final double ONE_D = (double) 1;
        
        // Then other integer types
        assertEquals(ONE_D, NODES.numberNode((byte) 1).doubleValue());
        assertEquals(ONE_D, NODES.numberNode((byte) 1).doubleValue(99));
        assertEquals(ONE_D, NODES.numberNode((byte) 1).doubleValueOpt().getAsDouble());
        assertEquals((double)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).doubleValue());
        assertEquals((double)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).doubleValue(99));
        assertEquals((double)Byte.MIN_VALUE, NODES.numberNode(Byte.MIN_VALUE).doubleValueOpt().getAsDouble());
        assertEquals((double)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).doubleValue());
        assertEquals((double)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).doubleValue(99));
        assertEquals((double)Byte.MAX_VALUE, NODES.numberNode(Byte.MAX_VALUE).doubleValueOpt().getAsDouble());

        assertEquals(ONE_D, NODES.numberNode((short) 1).doubleValue());
        assertEquals(ONE_D, NODES.numberNode((short) 1).doubleValue(99));
        assertEquals(ONE_D, NODES.numberNode((short) 1).doubleValueOpt().getAsDouble());
        assertEquals((double)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).doubleValue());
        assertEquals((double)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).doubleValue(99));
        assertEquals((double)Short.MIN_VALUE, NODES.numberNode(Short.MIN_VALUE).doubleValueOpt().getAsDouble());
        assertEquals((double)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).doubleValue());
        assertEquals((double)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).doubleValue(99));
        assertEquals((double)Short.MAX_VALUE, NODES.numberNode(Short.MAX_VALUE).doubleValueOpt().getAsDouble());

        assertEquals(ONE_D, NODES.numberNode(1).doubleValue());
        assertEquals(ONE_D, NODES.numberNode(1).doubleValue(99));
        assertEquals(ONE_D, NODES.numberNode(1).doubleValueOpt().getAsDouble());
        assertEquals((double) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).doubleValue());
        assertEquals((double) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).doubleValue(99));
        assertEquals((double) Integer.MIN_VALUE, NODES.numberNode(Integer.MIN_VALUE).doubleValueOpt().getAsDouble());
        assertEquals((double) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).doubleValue());
        assertEquals((double) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).doubleValue(99));
        assertEquals((double) Integer.MAX_VALUE, NODES.numberNode(Integer.MAX_VALUE).doubleValueOpt().getAsDouble());
        
        assertEquals(ONE_D, NODES.numberNode(1L).doubleValue());
        assertEquals(ONE_D, NODES.numberNode(1L).doubleValue(99));
        assertEquals(ONE_D, NODES.numberNode(1L).doubleValueOpt().getAsDouble());
        assertEquals((double) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE).doubleValue());
        assertEquals((double) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE).doubleValue(99));
        assertEquals((double) Long.MIN_VALUE, NODES.numberNode(Long.MIN_VALUE).doubleValueOpt().getAsDouble());
        assertEquals((double) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE).doubleValue());
        assertEquals((double) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE).doubleValue(99));
        assertEquals((double) Long.MAX_VALUE, NODES.numberNode(Long.MAX_VALUE).doubleValueOpt().getAsDouble());

        assertEquals(ONE_D, NODES.numberNode(BigInteger.valueOf(1)).doubleValue());
        assertEquals(ONE_D, NODES.numberNode(BigInteger.valueOf(1)).doubleValue(99));
        assertEquals(ONE_D, NODES.numberNode(BigInteger.valueOf(1)).doubleValueOpt().getAsDouble());
        assertEquals((double) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)).doubleValue());
        assertEquals((double) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)).doubleValue(99));
        assertEquals((double) Long.MIN_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)).doubleValueOpt().getAsDouble());
        assertEquals((double) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)).doubleValue());
        assertEquals((double) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)).doubleValue(99));
        assertEquals((double) Long.MAX_VALUE, NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)).doubleValueOpt().getAsDouble());
    }

    @Test
    public void failDoubleValueFromNumberIntRange() {
        // Can only fail for underflow/overflow: and that only for / BigInteger
        // (neither Integer nor Long is outside of range of even Float).

        final BigInteger tooBig = BigInteger.TEN.pow(310);      
        final BigInteger tooSmall = tooBig.negate();
        
        _assertFailDoubleForValueRange(NODES.numberNode(tooBig));
        _assertDefaultDoubleForOtherwiseFailing(NODES.numberNode(tooBig));
        _assertFailDoubleForValueRange(NODES.numberNode(tooSmall));
        _assertDefaultDoubleForOtherwiseFailing(NODES.numberNode(tooSmall));
    }

    // // // doubleValue() + Numbers/FPs

    @Test
    public void doubleValueFromNumberFPOk()
    {
        assertEquals(1.0, NODES.numberNode(1.0f).doubleValue());
        assertEquals(1.0, NODES.numberNode(1.0f).doubleValue(99));
        assertEquals(100_000.0, NODES.numberNode(100_000.0f).doubleValue());
        assertEquals(100_000.0, NODES.numberNode(100_000.0f).doubleValue(99));
        assertEquals(-100_000.0, NODES.numberNode(-100_000.0f).doubleValue());
        assertEquals(-100_000.0, NODES.numberNode(-100_000.0f).doubleValue(99));

        assertEquals(1.0, NODES.numberNode(1.0d).doubleValue());
        assertEquals(1.0, NODES.numberNode(1.0d).doubleValue(99));
        assertEquals(100_000.0, NODES.numberNode(100_000.0d).doubleValue());
        assertEquals(100_000.0, NODES.numberNode(100_000.0d).doubleValue(99));
        assertEquals(-100_000.0, NODES.numberNode(-100_000.0d).doubleValue());
        assertEquals(-100_000.0, NODES.numberNode(-100_000.0d).doubleValue(99));

        assertEquals(1.0,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).doubleValue());
        assertEquals(1.0,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).doubleValue(99));
        assertEquals((double) Long.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MIN_VALUE)).doubleValue());
        assertEquals((double) Long.MIN_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MIN_VALUE)).doubleValue(99));
        assertEquals((double) Long.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MAX_VALUE)).doubleValue());
        assertEquals((double) Long.MAX_VALUE,
                NODES.numberNode(BigDecimal.valueOf((double) Long.MAX_VALUE)).doubleValue(99));
    }

    @Test
    public void failDoubleValueFromNumberFPRange()
    {
        // Can only fail from BigDecimal (similar to ints vs BigInteger)

        final BigDecimal tooBig = new BigDecimal(BigInteger.TEN.pow(310))
                .add(BigDecimal.valueOf(0.125));
        final BigDecimal tooSmall = tooBig.negate();

        _assertFailDoubleForValueRange(NODES.numberNode(tooBig));
        _assertDefaultDoubleForOtherwiseFailing(NODES.numberNode(tooBig));
        _assertFailDoubleForValueRange(NODES.numberNode(tooSmall));
        _assertDefaultDoubleForOtherwiseFailing(NODES.numberNode(tooSmall));
    }

    // // // doubleValue() + non-Numeric types

    @Test
    public void failDoubleValueFromNonNumberScalar()
    {
        _assertFailDoubleForNonNumber(NODES.booleanNode(true));
        _assertDefaultDoubleForOtherwiseFailing(NODES.booleanNode(true));
        _assertFailDoubleForNonNumber(NODES.binaryNode(new byte[3]));
        _assertDefaultDoubleForOtherwiseFailing(NODES.binaryNode(new byte[3]));
        _assertFailDoubleForNonNumber(NODES.stringNode("123"));
        _assertDefaultDoubleForOtherwiseFailing(NODES.stringNode("123"));
        _assertFailDoubleForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertDefaultDoubleForOtherwiseFailing(NODES.rawValueNode(new RawValue("abc")));
        _assertFailDoubleForNonNumber(NODES.pojoNode(Boolean.TRUE));
        _assertDefaultDoubleForOtherwiseFailing(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void failDoubleValueFromStructural()
    {
        _assertFailDoubleForNonNumber(NODES.arrayNode(3));
        _assertDefaultDoubleForOtherwiseFailing(NODES.arrayNode(3));
        _assertFailDoubleForNonNumber(NODES.objectNode());
        _assertDefaultDoubleForOtherwiseFailing(NODES.objectNode());
    }

    @Test
    public void failDoubleValueFromMiscOther()
    {
        _assertFailDoubleForNonNumber(NODES.nullNode());
        _assertDefaultDoubleForOtherwiseFailing(NODES.nullNode());
        _assertFailDoubleForNonNumber(NODES.missingNode());
        _assertDefaultDoubleForOtherwiseFailing(NODES.missingNode());
    }

    // // // Shared helper methods

    private void _assertFailDoubleForValueRange(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.doubleValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value not in 64-bit `double` range");
    }

    private void _assertFailDoubleForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.doubleValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");
    }

    private void _assertDefaultDoubleForOtherwiseFailing(JsonNode node) {
        assertEquals(-2.25d, node.doubleValue(-2.25d));
        assertEquals(OptionalDouble.empty(), node.doubleValueOpt());
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
