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

    private final Optional<BigDecimal> BD_ONE_OPT = Optional.of(BD_ONE);

    private final BigDecimal BD_DEFAULT = bigDec(12.125);
    
    // // // decimalValue() + Numbers/Integers

    @Test
    public void decimalValueFromNumberIntOk()
    {
        // Then other integer types
        assertEquals(BD_ONE, NODES.numberNode((byte) 1).decimalValue());
        assertEquals(BD_ONE, NODES.numberNode((byte) 1).decimalValue(BD_DEFAULT));
        assertEquals(BD_ONE_OPT, NODES.numberNode((byte) 1).decimalValueOpt());
        assertEquals(bigDec(Byte.MIN_VALUE), NODES.numberNode(Byte.MIN_VALUE).decimalValue());
        assertEquals(bigDec(Byte.MIN_VALUE), NODES.numberNode(Byte.MIN_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Byte.MIN_VALUE), NODES.numberNode(Byte.MIN_VALUE).decimalValueOpt().get());
        assertEquals(bigDec(Byte.MAX_VALUE), NODES.numberNode(Byte.MAX_VALUE).decimalValue());
        assertEquals(bigDec(Byte.MAX_VALUE), NODES.numberNode(Byte.MAX_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Byte.MAX_VALUE), NODES.numberNode(Byte.MAX_VALUE).decimalValueOpt().get());

        assertEquals(BD_ONE, NODES.numberNode((short) 1).decimalValue());
        assertEquals(BD_ONE, NODES.numberNode((short) 1).decimalValue(BD_DEFAULT));
        assertEquals(BD_ONE_OPT, NODES.numberNode((short) 1).decimalValueOpt());
        assertEquals(bigDec(Short.MIN_VALUE), NODES.numberNode(Short.MIN_VALUE).decimalValue());
        assertEquals(bigDec(Short.MIN_VALUE), NODES.numberNode(Short.MIN_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Short.MIN_VALUE), NODES.numberNode(Short.MIN_VALUE).decimalValueOpt().get());
        assertEquals(bigDec(Short.MAX_VALUE), NODES.numberNode(Short.MAX_VALUE).decimalValue());
        assertEquals(bigDec(Short.MAX_VALUE), NODES.numberNode(Short.MAX_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Short.MAX_VALUE), NODES.numberNode(Short.MAX_VALUE).decimalValueOpt().get());

        assertEquals(BD_ONE, NODES.numberNode(1).decimalValue());
        assertEquals(BD_ONE, NODES.numberNode(1).decimalValue(BD_DEFAULT));
        assertEquals(BD_ONE_OPT, NODES.numberNode(1).decimalValueOpt());
        assertEquals(bigDec(Integer.MIN_VALUE), NODES.numberNode(Integer.MIN_VALUE).decimalValue());
        assertEquals(bigDec(Integer.MIN_VALUE), NODES.numberNode(Integer.MIN_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Integer.MIN_VALUE), NODES.numberNode(Integer.MIN_VALUE).decimalValueOpt().get());
        assertEquals(bigDec(Integer.MAX_VALUE), NODES.numberNode(Integer.MAX_VALUE).decimalValue());
        assertEquals(bigDec(Integer.MAX_VALUE), NODES.numberNode(Integer.MAX_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Integer.MAX_VALUE), NODES.numberNode(Integer.MAX_VALUE).decimalValueOpt().get());
        
        assertEquals(BD_ONE, NODES.numberNode(1L).decimalValue());
        assertEquals(BD_ONE, NODES.numberNode(1L).decimalValue(BD_DEFAULT));
        assertEquals(BD_ONE_OPT, NODES.numberNode(1L).decimalValueOpt());
        assertEquals(bigDec(Long.MIN_VALUE), NODES.numberNode(Long.MIN_VALUE).decimalValue());
        assertEquals(bigDec(Long.MIN_VALUE), NODES.numberNode(Long.MIN_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Long.MIN_VALUE), NODES.numberNode(Long.MIN_VALUE).decimalValueOpt().get());
        assertEquals(bigDec(Long.MAX_VALUE), NODES.numberNode(Long.MAX_VALUE).decimalValue());
        assertEquals(bigDec(Long.MAX_VALUE), NODES.numberNode(Long.MAX_VALUE).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Long.MAX_VALUE), NODES.numberNode(Long.MAX_VALUE).decimalValueOpt().get());

        assertEquals(BD_ONE, NODES.numberNode(BigInteger.valueOf(1)).decimalValue());
        assertEquals(BD_ONE, NODES.numberNode(BigInteger.valueOf(1)).decimalValue(BD_DEFAULT));
        assertEquals(BD_ONE_OPT, NODES.numberNode(BigInteger.valueOf(1)).decimalValueOpt());
        assertEquals(bigDec(Long.MIN_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)).decimalValue());
        assertEquals(bigDec(Long.MIN_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Long.MIN_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MIN_VALUE)).decimalValueOpt().get());
        assertEquals(bigDec(Long.MAX_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)).decimalValue());
        assertEquals(bigDec(Long.MAX_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)).decimalValue(BD_DEFAULT));
        assertEquals(bigDec(Long.MAX_VALUE), NODES.numberNode(BigInteger.valueOf(Long.MAX_VALUE)).decimalValueOpt().get());
    }

    // Cannot fail for Over/Underflow from Integer values
    //@Test public void failBigDecimalValueFromNumberIntRange() { }

    // // // decimalValue() + Numbers/FPs

    @Test
    public void decimalValueFromNumberFPOk()
    {
        assertEquals(BD_ONE_O, NODES.numberNode(1.0f).decimalValue());
        assertEquals(BD_ONE_O, NODES.numberNode(1.0f).decimalValue(BD_DEFAULT));
        assertEquals(BD_ONE_O, NODES.numberNode(1.0f).decimalValueOpt().get());
        assertEquals(bigDec("100000.0"), NODES.numberNode(100_000.0f).decimalValue());
        assertEquals(bigDec("100000.0"), NODES.numberNode(100_000.0f).decimalValue(BD_DEFAULT));
        assertEquals(bigDec("100000.0"), NODES.numberNode(100_000.0f).decimalValueOpt().get());
        assertEquals(bigDec("-100000.0"), NODES.numberNode(-100_000.0f).decimalValue());
        assertEquals(bigDec("-100000.0"), NODES.numberNode(-100_000.0f).decimalValue(BD_DEFAULT));
        assertEquals(bigDec("-100000.0"), NODES.numberNode(-100_000.0f).decimalValueOpt().get());
        
        assertEquals(BD_ONE_O, NODES.numberNode(1.0d).decimalValue());
        assertEquals(BD_ONE_O, NODES.numberNode(1.0d).decimalValue(BD_DEFAULT));
        assertEquals(BD_ONE_O, NODES.numberNode(1.0d).decimalValueOpt().get());
        assertEquals(bigDec("100000.0"), NODES.numberNode(100_000.0d).decimalValue());
        assertEquals(bigDec("100000.0"), NODES.numberNode(100_000.0d).decimalValue(BD_DEFAULT));
        assertEquals(bigDec("100000.0"), NODES.numberNode(100_000.0d).decimalValueOpt().get());
        assertEquals(bigDec("-100000.0"), NODES.numberNode(-100_000.0d).decimalValue());
        assertEquals(bigDec("-100000.0"), NODES.numberNode(-100_000.0d).decimalValue(BD_DEFAULT));
        assertEquals(bigDec("-100000.0"), NODES.numberNode(-100_000.0d).decimalValueOpt().get());

        assertEquals(new BigDecimal("100.001"),
                NODES.numberNode(new BigDecimal("100.001")).decimalValue());
        assertEquals(new BigDecimal("100.001"),
                NODES.numberNode(new BigDecimal("100.001")).decimalValue(BD_DEFAULT));
        assertEquals(new BigDecimal("100.001"),
                NODES.numberNode(new BigDecimal("100.001")).decimalValueOpt().get());
    }

    // Cannot fail for Over/Underflow from FP values either
    //@Test public void failBigDecimalFromNumberFPRange() { }

    // But can fail for NaN

    @Test
    public void decimalValueFromNumberFPFail()
    {
        _assertFailBigDecimalForNaN(NODES.numberNode(Float.NaN));

        _assertFailBigDecimalForNaN(NODES.numberNode(Double.NaN));
    }
    
    // // // decimalValue() + non-Numeric types

    @Test
    public void failBigDecimalFromNonNumberScalar()
    {
        _assertFailBigDecimalForNonNumber(NODES.booleanNode(true));
        _assertFailBigDecimalForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailBigDecimalForNonNumber(NODES.stringNode("123"));
        _assertFailBigDecimalForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailBigDecimalForNonNumber(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void failBigDecimalValueFromStructural()
    {
        _assertFailBigDecimalForNonNumber(NODES.arrayNode(3));
        _assertFailBigDecimalForNonNumber(NODES.objectNode());
    }

    @Test
    public void failDoubleValueFromMiscOther()
    {
        _assertFailBigDecimalForNonNumber(NODES.nullNode());
        _assertFailBigDecimalForNonNumber(NODES.missingNode());
    }

    // // // Shared helper methods

    private void _assertFailBigDecimalForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.decimalValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");

        // Verify default value handling
        assertEquals(BD_DEFAULT, node.decimalValue(BD_DEFAULT));
        assertEquals(Optional.empty(), node.decimalValueOpt());
    }

    private void _assertFailBigDecimalForNaN(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.decimalValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value non-Finite ('NaN')");

        // Verify default value handling
        assertEquals(BD_DEFAULT, node.decimalValue(BD_DEFAULT));
        assertEquals(Optional.empty(), node.decimalValueOpt());
    }

    protected static Optional<BigDecimal> bigDecOpt(BigDecimal bigDec) {
        return Optional.of(bigDec);
    }
}
