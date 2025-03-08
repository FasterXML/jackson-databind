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
public class JsonNodeStringValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    @Test
    public void stringValueSuccess()
    {
        assertEquals("abc", NODES.stringNode("abc").stringValue());
        assertEquals("abc", NODES.stringNode("abc").stringValue("xyz"));
        assertEquals("abc", NODES.stringNode("abc").stringValueOpt().get());
    }

    @Test
    public void stringValueFailFromNumbers()
    {
        _assertFailForNonString(NODES.numberNode((byte) 1));
        _assertFailForNonString(NODES.numberNode((short) 2));
        _assertFailForNonString(NODES.numberNode(3));
        _assertFailForNonString(NODES.numberNode(4L));
        _assertFailForNonString(NODES.numberNode(BigInteger.valueOf(5)));

        _assertFailForNonString(NODES.numberNode(0.25f));
        _assertFailForNonString(NODES.numberNode(-2.125d));
        _assertFailForNonString(NODES.numberNode(new BigDecimal("0.1")));
    }

    @Test
    public void stringValueFailFromNonNumberScalars()
    {
        _assertFailForNonString(NODES.binaryNode(new byte[3]));
        _assertFailForNonString(NODES.rawValueNode(new RawValue("abc")));
        _assertFailForNonString(NODES.pojoNode(new AtomicInteger(1)));
    }

    @Test
    public void numberValueFromStructural()
    {
        _assertFailForNonString(NODES.arrayNode(3));
        _assertFailForNonString(NODES.objectNode());
    }

    @Test
    public void numberValueFromNonNumberMisc()
    {
        _assertFailForNonString(NODES.nullNode());
        _assertFailForNonString(NODES.missingNode());
    }

    private void _assertFailForNonString(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.stringValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not String");

        // But also check defaulting
        assertEquals("foo", node.stringValue("foo"));
        assertFalse(node.stringValueOpt().isPresent());
    }
}
