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

    // // // BigInteger + bigIntegerValue()

    @Test
    public void bigIntegerValueFromNumberIntOk()
    {
        // Integer types, byte/short/int/long/BigInteger
        assertEquals(BigInteger.ONE, NODES.numberNode((byte) 1).bigIntegerValue());
        assertEquals(bigInt(Byte.MIN_VALUE), NODES.numberNode(Byte.MIN_VALUE).bigIntegerValue());
        assertEquals(bigInt(Byte.MAX_VALUE), NODES.numberNode(Byte.MAX_VALUE).bigIntegerValue());

        assertEquals(BigInteger.ONE, NODES.numberNode((short) 1).bigIntegerValue());
        assertEquals(bigInt(Short.MIN_VALUE), NODES.numberNode(Short.MIN_VALUE).bigIntegerValue());
        assertEquals(bigInt(Short.MAX_VALUE), NODES.numberNode(Short.MAX_VALUE).bigIntegerValue());

        assertEquals(BigInteger.ONE, NODES.numberNode(1).bigIntegerValue());
        assertEquals(bigInt(Integer.MIN_VALUE), NODES.numberNode(Integer.MIN_VALUE).bigIntegerValue());
        assertEquals(bigInt(Integer.MAX_VALUE), NODES.numberNode(Integer.MAX_VALUE).bigIntegerValue());

        assertEquals(BigInteger.ONE, NODES.numberNode(1L).bigIntegerValue());
        assertEquals(bigInt(Long.MIN_VALUE), NODES.numberNode(Long.MIN_VALUE).bigIntegerValue());
        assertEquals(bigInt(Long.MAX_VALUE), NODES.numberNode(Long.MAX_VALUE).bigIntegerValue());

        assertEquals(BigInteger.ONE, NODES.numberNode(BigInteger.ONE).bigIntegerValue());
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE),
                NODES.numberNode(Long.MIN_VALUE).bigIntegerValue());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE),
                NODES.numberNode(Long.MAX_VALUE).bigIntegerValue());
    }

    // NOTE: BigInteger has unlimited range so cannot fail for Under-/Overflow (hence no tests)

    @Test
    public void bigIntegerValueFromNumberFPOk()
    {
        assertEquals(BigInteger.ONE, NODES.numberNode(1.0f).bigIntegerValue());
        assertEquals(bigInt(100_000), NODES.numberNode(100_000.0f).bigIntegerValue());
        assertEquals(bigInt(-100_000), NODES.numberNode(-100_000.0f).bigIntegerValue());
    
        assertEquals(BigInteger.ONE, NODES.numberNode(1.0f).bigIntegerValue(BigInteger.TEN));
        assertEquals(BigInteger.ONE, NODES.numberNode(1.0f).bigIntegerValueOpt().get());
        
        assertEquals(BigInteger.ONE, NODES.numberNode(1.0d).bigIntegerValue());
        assertEquals(bigInt(100_000_000), NODES.numberNode(100_000_000.0d).bigIntegerValue());
        assertEquals(bigInt(-100_000_000), NODES.numberNode(-100_000_000.0d).bigIntegerValue());
    
        assertEquals(BigInteger.ONE, NODES.numberNode(1.0d).bigIntegerValue(BigInteger.TEN));
        assertEquals(BigInteger.ONE, NODES.numberNode(1.0d).bigIntegerValueOpt().get());
    
        assertEquals(BigInteger.ONE,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).bigIntegerValue());
        assertEquals(bigInt(Long.MIN_VALUE),
                NODES.numberNode(new BigDecimal(Long.MIN_VALUE+".0")).bigIntegerValue());
        assertEquals(bigInt(Long.MAX_VALUE),
                NODES.numberNode(new BigDecimal(Long.MAX_VALUE+".0")).bigIntegerValue());
    
        assertEquals(BigInteger.ONE,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).bigIntegerValue(BigInteger.TEN));
        assertEquals(BigInteger.ONE,
                NODES.numberNode(BigDecimal.valueOf(1.0d)).bigIntegerValueOpt().get());
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
    
    // // // Shared helper methods

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
}
