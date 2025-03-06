package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

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
public class JsonNodeBooleanValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    @Test
    public void booleanValueSuccess()
    {
        assertEquals(true, NODES.booleanNode(true).booleanValue());
        assertEquals(false, NODES.booleanNode(false).booleanValue());
    }

    @Test
    public void booleanValueFailFromNumbers()
    {
        _assertFailForNonBoolean(NODES.numberNode((byte) 1));
        _assertFailForNonBoolean(NODES.numberNode((short) 2));
        _assertFailForNonBoolean(NODES.numberNode(3));
        _assertFailForNonBoolean(NODES.numberNode(4L));
        _assertFailForNonBoolean(NODES.numberNode(BigInteger.valueOf(5)));

        _assertFailForNonBoolean(NODES.numberNode(0.25f));
        _assertFailForNonBoolean(NODES.numberNode(-2.125d));
        _assertFailForNonBoolean(NODES.numberNode(new BigDecimal("0.1")));
    }

    @Test
    public void booleanValueFailFromNonNumberScalars()
    {
        _assertFailForNonBoolean(NODES.binaryNode(new byte[3]));
        _assertFailForNonBoolean(NODES.stringNode("123"));
        _assertFailForNonBoolean(NODES.rawValueNode(new RawValue("abc")));
        _assertFailForNonBoolean(NODES.pojoNode(new AtomicInteger(1)));
    }

    @Test
    public void numberValueFromStructural()
    {
        _assertFailForNonBoolean(NODES.arrayNode(3));
        _assertFailForNonBoolean(NODES.objectNode());
    }

    @Test
    public void numberValueFromNonNumberMisc()
    {
        _assertFailForNonBoolean(NODES.nullNode());
        _assertFailForNonBoolean(NODES.missingNode());
    }

    private void _assertFailForNonBoolean(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.booleanValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not boolean");

        // But also check defaulting
        assertFalse(node.booleanValue(false));
        assertTrue(node.booleanValue(true));

        assertFalse(node.booleanValueOpt().isPresent());
    }
}
