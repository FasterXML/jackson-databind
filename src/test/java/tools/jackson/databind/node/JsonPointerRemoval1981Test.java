package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JsonPointer-based removal functionality ([databind#1981]).
 */
public class JsonPointerRemoval1981Test extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testRemoveSimpleProperty() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("a", 1);
        root.put("b", 2);
        root.put("c", 3);

        // Remove property "b"
        JsonNode removed = root.remove(JsonPointer.compile("/b"));
        assertEquals(2, removed.asInt());

        assertEquals(MAPPER.readTree(a2q("""
                {
                   "a": 1,
                   "c": 3
                }
                """)), root);
    }

    @Test
    public void testRemoveNestedProperty() throws Exception
    {
        String json = a2q("{'a':{'b':{'c':13,'d':14},'e':15},'f':16}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Remove nested property /a/b/c
        JsonNode removed = root.remove(JsonPointer.compile("/a/b/c"));
        assertEquals(13, removed.asInt());

        assertEquals(MAPPER.readTree(
                a2q("{'a':{'b':{'d':14},'e':15},'f':16}")),
                root);
    }

    @Test
    public void testRemoveArrayElement() throws Exception
    {
        ArrayNode array = MAPPER.createArrayNode();
        array.add(10);
        array.add(20);
        array.add(30);
        array.add(40);

        // Remove element at index 1
        JsonNode removed = array.remove(JsonPointer.compile("/1"));
        assertEquals(20, removed.asInt());

        assertEquals(MAPPER.readTree("[10,30,40]"), array);
    }

    @Test
    public void testRemoveFromNestedArray() throws Exception
    {
        String json = a2q("{'array':[1,2,3,4,5]}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Remove array element at /array/2
        JsonNode removed = root.remove(JsonPointer.compile("/array/2"));
        assertEquals(3, removed.asInt());

        assertEquals(MAPPER.readTree(a2q("{'array':[1,2,4,5]}")), root);
    }

    @Test
    public void testRemoveMixedNestedStructure() throws Exception
    {
        String json = a2q("{'Image':{'Width':800,'Height':600,'IDs':[116,943,234,38793]}}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Remove array element /Image/IDs/2
        JsonNode removed = root.remove(JsonPointer.compile("/Image/IDs/2"));
        assertEquals(234, removed.asInt());

        assertEquals(MAPPER.readTree(a2q(
                "{'Image':{'Width':800,'Height':600,'IDs':[116,943,38793]}}")), root);
    }

    @Test
    public void testRemoveWithCompiledPointer() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("x", 100);
        root.put("y", 200);

        // Use JsonPointer.compile() for path-based removal
        JsonNode removed = root.remove(JsonPointer.compile("/x"));
        assertEquals(100, removed.asInt());

        assertEquals(MAPPER.readTree(a2q("{'y':200}")), root);
    }

    @Test
    public void testRemoveNonExistentProperty() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("a", 1);

        // Try to remove non-existent property
        JsonNode removed = root.remove(JsonPointer.compile("/nonexistent"));
        assertTrue(removed.isMissingNode());

        // and non-existing array element too
        removed = root.remove(JsonPointer.compile("/0"));
        assertTrue(removed.isMissingNode());

        // Structure should be unchanged
        assertEquals(MAPPER.readTree(a2q("{'a':1}")), root);
    }

    @Test
    public void testRemoveNonExistentArrayIndex() throws Exception
    {
        ArrayNode array = MAPPER.createArrayNode();
        array.add(1);
        array.add(2);

        // Try to remove out-of-bounds index
        JsonNode removed = array.remove(JsonPointer.compile("/10"));
        assertTrue(removed.isMissingNode());

        // and property (that arrays can't have)
        removed = array.remove(JsonPointer.compile("/a"));
        assertTrue(removed.isMissingNode());
        
        // Array should be unchanged
        assertEquals(MAPPER.readTree("[1, 2]"), array);
    }

    @Test
    public void testRemoveNonExistentNestedPath() throws Exception
    {
        String json = a2q("{'a':{'b':1}}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Try to remove from non-existent nested path
        JsonNode removed = root.remove(JsonPointer.compile("/a/x/y"));
        assertTrue(removed.isMissingNode());

        // Structure should be unchanged
        assertEquals(MAPPER.readTree(json), root);
    }

    @Test
    public void testRemoveEmptyPointer() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("a", 1);

        // Empty pointer should return null (can't remove root from itself)
        JsonNode removed = root.remove(JsonPointer.compile(""));
        assertTrue(removed.isMissingNode());

        // Structure should be unchanged
        assertEquals(MAPPER.readTree(a2q("{'a':1}")), root);
    }

    @Test
    public void testRemoveFromValueNode() throws Exception
    {
        String json = a2q("{'a':{'b':123}}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Try to remove from a value node (not a container)
        JsonNode removed = root.remove(JsonPointer.compile("/a/b/c"));
        assertTrue(removed.isMissingNode());

        // Structure should be unchanged
        assertEquals(MAPPER.readTree(json), root);
    }

    @Test
    public void testRemovePropertyWithSpecialCharacters() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("a/b", 1);
        root.put("c~d", 2);
        root.put("normal", 3);

        // Remove property with slash (escaped as ~1)
        JsonNode removed = root.remove(JsonPointer.compile("/a~1b"));
        assertEquals(1, removed.asInt());
        assertFalse(root.has("a/b"));

        // Remove property with tilde (escaped as ~0)
        removed = root.remove(JsonPointer.compile("/c~0d"));
        assertEquals(2, removed.asInt());
        assertFalse(root.has("c~d"));

        assertEquals(1, root.size());
        assertTrue(root.has("normal"));
    }

    @Test
    public void testRemoveEntireObject() throws Exception
    {
        String json = a2q("{'outer':{'inner':{'deep':42}}}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Remove entire inner object
        JsonNode removed = root.remove(JsonPointer.compile("/outer/inner"));
        assertTrue(removed.isObject());
        assertEquals(42, removed.path("deep").asInt());

        // Verify structure - inner should be gone
        assertEquals(MAPPER.readTree(a2q("{'outer':{}}")), root);
    }

    @Test
    public void testRemoveEntireArray() throws Exception
    {
        String json = a2q("{'data':{'values':[1,2,3,4,5]}}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Remove entire array
        JsonNode removed = root.remove(JsonPointer.compile("/data/values"));
        assertTrue(removed.isArray());
        assertEquals(5, removed.size());

        // Verify structure - values array should be gone
        assertEquals(MAPPER.readTree(a2q("{'data':{}}")), root);
    }

    @Test
    public void testRemoveFromEmptyObject() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        assertTrue(root.remove(JsonPointer.compile("/anything")).isMissingNode());
    }

    @Test
    public void testRemoveFromEmptyArray() throws Exception
    {
        ArrayNode array = MAPPER.createArrayNode();
        assertTrue(array.remove(JsonPointer.compile("/0")).isMissingNode());
    }

    @Test
    public void testRemoveFirstAndLastArrayElements() throws Exception
    {
        ArrayNode array = MAPPER.createArrayNode();
        array.add("first");
        array.add("middle");
        array.add("last");

        // Remove first element
        JsonNode removed = array.remove(JsonPointer.compile("/0"));
        assertEquals("first", removed.asString());
        assertEquals(2, array.size());
        assertEquals("middle", array.get(0).asString());

        // Remove last element (now at index 1)
        removed = array.remove(JsonPointer.compile("/1"));
        assertEquals("last", removed.asString());
        assertEquals(1, array.size());
        assertEquals("middle", array.get(0).asString());
    }

    @Test
    public void testRemoveChainedOperations() throws Exception
    {
        String json = a2q("{'a':1,'b':2,'c':3,'d':4}");
        ObjectNode root = (ObjectNode) MAPPER.readTree(json);

        // Remove multiple properties in sequence using JsonPointer
        assertNotNull(root.remove(JsonPointer.compile("/a")));
        assertNotNull(root.remove(JsonPointer.compile("/c")));
        assertNotNull(root.remove(JsonPointer.compile("/d")));

        // Only 'b' should remain
        assertEquals(1, root.size());
        assertTrue(root.has("b"));
        assertEquals(2, root.get("b").asInt());
    }

    @Test
    public void testRemoveNullValueProperty() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.putNull("nullProp");
        root.put("normalProp", 42);

        // Remove the null property
        JsonNode removed = root.remove(JsonPointer.compile("/nullProp"));
        assertNotNull(removed);
        assertTrue(removed.isNull());

        assertEquals(1, root.size());
        assertFalse(root.has("nullProp"));
        assertTrue(root.has("normalProp"));
    }

    @Test
    public void testRemoveWithEmptyStringPropertyName() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("", "empty key");
        root.put("normal", "normal key");

        // Remove property with empty string key (pointer is "/")
        JsonNode removed = root.remove(JsonPointer.compile("/"));
        assertNotNull(removed);
        assertEquals("empty key", removed.asString());

        assertEquals(1, root.size());
        assertFalse(root.has(""));
        assertTrue(root.has("normal"));
    }
}
