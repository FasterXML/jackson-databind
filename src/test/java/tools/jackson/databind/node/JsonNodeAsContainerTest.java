package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#2343], {@code JsonNode.asArray()}
 * and {@code JsonNode.asObject()}.
 */
public class JsonNodeAsContainerTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // asArray() tests

    @Test
    public void asArrayOkFromArrayNode()
    {
        ArrayNode array = NODES.arrayNode();
        array.add(1);
        array.add("two");

        // asArray() should return the same instance
        assertSame(array, array.asArray());

        // asArrayOpt() should return Optional containing the same instance
        Optional<ArrayNode> opt = array.asArrayOpt();
        assertTrue(opt.isPresent());
        assertSame(array, opt.get());
    }

    @Test
    public void asArrayFailFromObjectNode()
    {
        _assertFailAsArrayFor(NODES.objectNode());
    }

    @Test
    public void asArrayFailFromScalars()
    {
        _assertFailAsArrayFor(NODES.booleanNode(true));
        _assertFailAsArrayFor(NODES.booleanNode(false));
        _assertFailAsArrayFor(NODES.numberNode(42));
        _assertFailAsArrayFor(NODES.numberNode(42L));
        _assertFailAsArrayFor(NODES.numberNode(42.5));
        _assertFailAsArrayFor(NODES.numberNode(42.5f));
        _assertFailAsArrayFor(NODES.numberNode(BigInteger.TEN));
        _assertFailAsArrayFor(NODES.numberNode(BigDecimal.valueOf(12.5)));
        _assertFailAsArrayFor(NODES.stringNode("test"));
        _assertFailAsArrayFor(NODES.binaryNode(new byte[3]));
        _assertFailAsArrayFor(NODES.nullNode());
        _assertFailAsArrayFor(NODES.missingNode());
        _assertFailAsArrayFor(NODES.rawValueNode(new RawValue("abc")));
        _assertFailAsArrayFor(NODES.pojoNode(new AtomicInteger(1)));
    }

    // // // asObject() tests

    @Test
    public void asObjectOkFromObjectNode()
    {
        ObjectNode obj = NODES.objectNode();
        obj.put("key", "value");

        // asObject() should return the same instance
        assertSame(obj, obj.asObject());

        // asObjectOpt() should return Optional containing the same instance
        Optional<ObjectNode> opt = obj.asObjectOpt();
        assertTrue(opt.isPresent());
        assertSame(obj, opt.get());
    }

    @Test
    public void asObjectFailFromArrayNode()
    {
        _assertFailAsObjectFor(NODES.arrayNode());
    }

    @Test
    public void asObjectFailFromScalars()
    {
        _assertFailAsObjectFor(NODES.booleanNode(true));
        _assertFailAsObjectFor(NODES.booleanNode(false));
        _assertFailAsObjectFor(NODES.numberNode(42));
        _assertFailAsObjectFor(NODES.numberNode(42L));
        _assertFailAsObjectFor(NODES.numberNode(42.5));
        _assertFailAsObjectFor(NODES.numberNode(42.5f));
        _assertFailAsObjectFor(NODES.numberNode(BigInteger.TEN));
        _assertFailAsObjectFor(NODES.numberNode(BigDecimal.valueOf(12.5)));
        _assertFailAsObjectFor(NODES.stringNode("test"));
        _assertFailAsObjectFor(NODES.binaryNode(new byte[3]));
        _assertFailAsObjectFor(NODES.nullNode());
        _assertFailAsObjectFor(NODES.missingNode());
        _assertFailAsObjectFor(NODES.rawValueNode(new RawValue("abc")));
        _assertFailAsObjectFor(NODES.pojoNode(new AtomicInteger(1)));
    }

    // // // Use case from issue: iterating array elements as objects

    @Test
    public void iterateArrayElementsAsObjects()
    {
        ArrayNode array = NODES.arrayNode();
        array.addObject().put("name", "first");
        array.addObject().put("name", "second");

        // Using asObject() to modify elements
        for (JsonNode element : array) {
            element.asObject().put("added", true);
        }

        // Verify modifications
        assertEquals(true, array.get(0).get("added").booleanValue());
        assertEquals(true, array.get(1).get("added").booleanValue());
    }

    @Test
    public void iterateArrayElementsAsArrays()
    {
        ArrayNode outer = NODES.arrayNode();
        outer.addArray().add(1).add(2);
        outer.addArray().add(3).add(4);

        // Using asArray() to modify elements
        for (JsonNode element : outer) {
            element.asArray().add(99);
        }

        // Verify modifications
        assertEquals(3, outer.get(0).size());
        assertEquals(3, outer.get(1).size());
        assertEquals(99, outer.get(0).get(2).intValue());
        assertEquals(99, outer.get(1).get(2).intValue());
    }

    // // // Helper methods

    private void _assertFailAsArrayFor(JsonNode node)
    {
        // asArray() should throw exception
        Exception e = assertThrows(JsonNodeException.class,
                () -> node.asArray(),
                "For (" + node.getClass().getSimpleName() + ") value: " + node);
        assertThat(e.getMessage())
            .contains("asArray()")
            .contains("on `ArrayNode`");

        // asArrayOpt() should return empty Optional
        Optional<ArrayNode> opt = node.asArrayOpt();
        assertFalse(opt.isPresent(),
                "Expected empty Optional for " + node.getClass().getSimpleName());
    }

    private void _assertFailAsObjectFor(JsonNode node)
    {
        // asObject() should throw exception
        Exception e = assertThrows(JsonNodeException.class,
                () -> node.asObject(),
                "For (" + node.getClass().getSimpleName() + ") value: " + node);
        assertThat(e.getMessage())
            .contains("asObject()")
            .contains("on `ObjectNode`");

        // asObjectOpt() should return empty Optional
        Optional<ObjectNode> opt = node.asObjectOpt();
        assertFalse(opt.isPresent(),
                "Expected empty Optional for " + node.getClass().getSimpleName());
    }
}
