package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#4958], JsonNode.numberValue()
 * over all node types.
 */
public class JsonNodeStringValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // stringValue() tests

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
        _assertStringValueFailForNonString(NODES.numberNode((byte) 1));
        _assertStringValueFailForNonString(NODES.numberNode((short) 2));
        _assertStringValueFailForNonString(NODES.numberNode(3));
        _assertStringValueFailForNonString(NODES.numberNode(4L));
        _assertStringValueFailForNonString(NODES.numberNode(BigInteger.valueOf(5)));

        _assertStringValueFailForNonString(NODES.numberNode(0.25f));
        _assertStringValueFailForNonString(NODES.numberNode(-2.125d));
        _assertStringValueFailForNonString(NODES.numberNode(new BigDecimal("0.1")));
    }

    @Test
    public void stringValueFailFromNonNumberScalars()
    {
        _assertStringValueFailForNonString(NODES.binaryNode(new byte[3]));
        _assertStringValueFailForNonString(NODES.rawValueNode(new RawValue("abc")));
        _assertStringValueFailForNonString(NODES.pojoNode(new AtomicInteger(1)));
    }

    @Test
    public void stringValueFromStructural()
    {
        _assertStringValueFailForNonString(NODES.arrayNode(3));
        _assertStringValueFailForNonString(NODES.objectNode());
    }

    @Test
    public void stringValueFromNonNumberMisc()
    {
        _assertStringValueFailForNonString(NODES.missingNode());
    }

    @Test
    public void stringValueFromNullNode()
    {
        JsonNode node = NODES.nullNode();
        assertEquals(null, node.stringValue());

        // But also check defaulting
        assertEquals("foo", node.stringValue("foo"));
        assertFalse(node.stringValueOpt().isPresent());
    }

    // // // asString() tests

    @Test
    public void asStringSuccess()
    {
        _assertAsStringSuccess("abc", NODES.stringNode("abc"));
    }

    @Test
    public void asStringFromNumbers()
    {
        _assertAsStringSuccess("1", NODES.numberNode((byte) 1));
        _assertAsStringSuccess("2", NODES.numberNode((short) 2));
        _assertAsStringSuccess("3", NODES.numberNode(3));
        _assertAsStringSuccess("4", NODES.numberNode(4L));
        _assertAsStringSuccess("10", NODES.numberNode(BigInteger.TEN));

        _assertAsStringSuccess("0.25", NODES.numberNode(0.25f));
        _assertAsStringSuccess("-2.125", NODES.numberNode(-2.125d));
        _assertAsStringSuccess("0.1", NODES.numberNode(new BigDecimal("0.1")));
    }

    @Test
    public void asStringForNonNumberScalars()
    {
        // Binary converted to Base64
        _assertAsStringSuccess("AAA=", NODES.binaryNode(new byte[2]));
        _assertAsStringSuccess("xyz", NODES.pojoNode("xyz"));
        _assertAsStringFailForNonString(NODES.pojoNode(new AtomicInteger(1)));

        // RawValue's won't convert
        _assertAsStringFailForNonString(NODES.rawValueNode(new RawValue("abcd")));
    }

    @Test
    public void asStringFailForStructural()
    {
        _assertAsStringFailForNonString(NODES.arrayNode(3));
        _assertAsStringFailForNonString(NODES.objectNode());
    }

    @Test
    public void asStringFromNonNumberMisc()
    {
        _assertAsStringSuccess("", NODES.nullNode());

        _assertAsStringFailForNonString(NODES.missingNode());
    }

    // // // Helper methods:

    private void _assertStringValueFailForNonString(JsonNode node) {
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

    private void _assertAsStringSuccess(String expected, JsonNode node) {
        assertEquals(expected, node.asString());

        // But also fallbacks
        assertEquals(expected, node.asString("fallback"));
        assertEquals(expected, node.asStringOpt().get());
    }

    private void _assertAsStringFailForNonString(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asString(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not coercible to `String`");

        // But also check defaulting
        assertEquals("foo", node.asString("foo"));
        assertFalse(node.asStringOpt().isPresent());
    }
}
