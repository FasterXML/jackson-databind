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
 * Tests for [databind#4958], JsonNode.numberValue()
 * over all node types.
 */
public class JsonNodeNumberValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    @Test
    public void numberValueFromNumbersInt()
    {
        assertEquals(Integer.valueOf(127), NODES.numberNode((byte) 127).numberValue());
        assertEquals(Short.valueOf((short) 123), NODES.numberNode((short) 123).numberValue());
        assertEquals(Integer.valueOf(234), NODES.numberNode(234).numberValue());
        assertEquals(Long.valueOf(3456L), NODES.numberNode(3456L).numberValue());
        assertEquals(BigInteger.TWO, NODES.numberNode(BigInteger.TWO).numberValue());
    }

    @Test
    public void numberValueFromNumbersFP()
    {
        assertEquals(Float.valueOf(0.25f), NODES.numberNode(0.25f).numberValue());
        assertEquals(Double.valueOf(-2.125d), NODES.numberNode(-2.125d).numberValue());
        assertEquals(new BigDecimal("0.1"), NODES.numberNode(new BigDecimal("0.1")).numberValue());
    }

    @Test
    public void numberValueFromNonNumberScalars()
    {
        _assertFailForNonNumber(NODES.booleanNode(true));
        _assertFailForNonNumber(NODES.binaryNode(new byte[3]));
        _assertFailForNonNumber(NODES.stringNode("123"));
        _assertFailForNonNumber(NODES.rawValueNode(new RawValue("abc")));
        _assertFailForNonNumber(NODES.pojoNode(Boolean.TRUE));
    }

    @Test
    public void numberValueFromStructural()
    {
        _assertFailForNonNumber(NODES.arrayNode(3));
        _assertFailForNonNumber(NODES.objectNode());
    }

    @Test
    public void numberValueFromNonNumberMisc()
    {
        _assertFailForNonNumber(NODES.nullNode());
        _assertFailForNonNumber(NODES.missingNode());
    }

    private void _assertFailForNonNumber(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.intValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not numeric");
    }
}
