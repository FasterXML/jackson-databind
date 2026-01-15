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

    // // // Other tests
}
