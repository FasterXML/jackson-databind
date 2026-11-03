package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonNode#map(java.util.function.Function)} method
 */
public class JsonNodeMapTest extends NodeTestBase
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // // // Tests for JsonNode.map()

    @Test
    public void testMapWithStringNode()
    {
        JsonNode node = MAPPER.stringNode("hello");

        // Map to upper-case string
        String result = node.map(n -> n.asString().toUpperCase());
        assertEquals("HELLO", result);

        // Map to string length
        Integer length = node.map(n -> n.asString().length());
        assertEquals(5, length);
    }

    @Test
    public void testMapWithNumberNode()
    {
        JsonNode node = MAPPER.readTree("42");

        // Map to doubled value
        Integer doubled = node.map(n -> n.asInt() * 2);
        assertEquals(84, doubled);

        // Map to String node
        JsonNode result = node.map(n -> MAPPER.stringNode(n.asString()));
        assertTrue(result.isString());
        assertEquals("42", result.stringValue());
    }

    @Test
    public void testMapWithBooleanNode()
    {
        assertEquals("yes",
                MAPPER.booleanNode(true).map(n -> n.asBoolean() ? "yes" : "no"));
        assertEquals("no",
                MAPPER.booleanNode(false).map(n -> n.asBoolean() ? "yes" : "no"));
    }

    @Test
    public void testMapWithNullNode()
    {
        JsonNode node = MAPPER.readTree("null");

        // Map null to a default value
        String result = node.map(n -> n.isNull() ? "default" : n.asString());
        assertEquals("default", result);
    }

    @Test
    public void testMapWithObjectNode()
    {
        JsonNode node = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        // Map to extract a property
        assertEquals("John", node.map(n -> n.get("name").asString()));
    }

    @Test
    public void testMapWithArrayNode()
    {
        JsonNode node = MAPPER.readTree("[1,2,3,4,5]");
        Integer I = node.map(n -> n.size());
        assertEquals(5, I);
    }

    @Test
    public void testMapWithMissingNode()
    {
        JsonNode missingNode = MAPPER.missingNode();
        assertEquals("not found",
                missingNode.map(n -> n.isMissingNode() ? "not found" : n.asString()));
    }

    @Test
    public void testMapReturningNull()
    {
        assertNull(MAPPER.stringNode("test").map(n -> null));
    }

    @Test
    public void testMapWithComplexTransformation()
    {
        JsonNode node = MAPPER.readTree("{\"x\":10,\"y\":20}");
        Point point = node.map(n -> new Point(
            n.get("x").asInt(),
            n.get("y").asInt()
        ));

        assertEquals(10, point.x);
        assertEquals(20, point.y);
    }

    // // // Tests for JsonNode.nullAs()

    @Test
    public void testNullAsWithNullNode()
    {
        JsonNode defaultNode = MAPPER.stringNode("default");
        JsonNode result = MAPPER.nullNode().nullAs(defaultNode);
        assertSame(defaultNode, result);
    }

    @Test
    public void testNullAsWithNonNullNode()
    {
        JsonNode stringNode = MAPPER.stringNode("hello");
        JsonNode defaultNode = MAPPER.stringNode("default");

        assertSame(stringNode, stringNode.nullAs(defaultNode));
    }

    @Test
    public void testNullAsSupplierWithNullNode()
    {
        JsonNode defaultNode = MAPPER.stringNode("supplied");

        JsonNode result = MAPPER.nullNode().nullAs(() -> defaultNode);
        assertSame(defaultNode, result);
    }

    @Test
    public void testNullAsSupplierWithNonNullNode()
    {
        JsonNode stringNode = MAPPER.stringNode("hello");
        boolean[] supplierCalled = {false};

        JsonNode result = stringNode.nullAs(() -> {
            supplierCalled[0] = true;
            return MAPPER.stringNode("supplied");
        });

        assertSame(stringNode, result);
        assertFalse(supplierCalled[0], "Supplier should not be called for non-null node");
    }

    @Test
    public void testNullAsWithMissingNode()
    {
        // MissingNode is not a null node, so should return itself
        JsonNode missingNode = MAPPER.missingNode();
        JsonNode defaultNode = MAPPER.stringNode("default");

        assertSame(missingNode, missingNode.nullAs(defaultNode));
    }

    // // // Tests for JsonNode.missingAs()

    @Test
    public void testMissingAsWithMissingNode()
    {
        JsonNode defaultNode = MAPPER.stringNode("default");
        JsonNode result = MAPPER.missingNode().missingAs(defaultNode);
        assertSame(defaultNode, result);
    }

    @Test
    public void testMissingAsWithNonMissingNode()
    {
        JsonNode stringNode = MAPPER.stringNode("hello");
        JsonNode defaultNode = MAPPER.stringNode("default");

        assertSame(stringNode, stringNode.missingAs(defaultNode));
    }

    @Test
    public void testMissingAsSupplierWithMissingNode()
    {
        JsonNode defaultNode = MAPPER.stringNode("supplied");

        JsonNode result = MAPPER.missingNode().missingAs(() -> defaultNode);
        assertSame(defaultNode, result);
    }

    @Test
    public void testMissingAsSupplierWithNonMissingNode()
    {
        JsonNode stringNode = MAPPER.stringNode("hello");
        boolean[] supplierCalled = {false};

        JsonNode result = stringNode.missingAs(() -> {
            supplierCalled[0] = true;
            return MAPPER.stringNode("supplied");
        });

        assertSame(stringNode, result);
        assertFalse(supplierCalled[0], "Supplier should not be called for non-missing node");
    }

    @Test
    public void testMissingAsWithNullNode()
    {
        // NullNode is not a missing node, so should return itself
        JsonNode nullNode = MAPPER.nullNode();
        JsonNode defaultNode = MAPPER.stringNode("default");

        JsonNode result = nullNode.missingAs(defaultNode);
        assertSame(nullNode, result);
    }

    @Test
    public void testNullAsAndMissingAsCombined()
    {
        // Test chaining both methods together
        JsonNode defaultNode = MAPPER.stringNode("default");

        // null.nullAs(...).missingAs(...) -> should return from nullAs
        JsonNode nullNode = MAPPER.nullNode();
        JsonNode result1 = nullNode.nullAs(defaultNode).missingAs(MAPPER.stringNode("other"));
        assertSame(defaultNode, result1);

        // missing.nullAs(...).missingAs(...) -> should return from missingAs
        JsonNode missingNode = MAPPER.missingNode();
        JsonNode result2 = missingNode.nullAs(MAPPER.stringNode("other")).missingAs(defaultNode);
        assertSame(defaultNode, result2);

        // regular.nullAs(...).missingAs(...) -> should return original
        JsonNode regularNode = MAPPER.stringNode("regular");
        JsonNode result3 = regularNode.nullAs(MAPPER.stringNode("a")).missingAs(MAPPER.stringNode("b"));
        assertSame(regularNode, result3);
    }
}
