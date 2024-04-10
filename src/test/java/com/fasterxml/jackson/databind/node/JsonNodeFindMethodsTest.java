package com.fasterxml.jackson.databind.node;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonNodeFindMethodsTest
    extends DatabindTestUtil
{
    private final String JSON_SAMPLE = "{ \"a\" : { \"value\" : 3 },"
            +"\"array\" : [ { \"b\" : 3 }, {\"value\" : 42}, { \"other\" : true } ]"
            +"}";
    
    private final String JSON_4229 = a2q("{"
            + "    'target': 'target1'," // Found in <= 2.15.3 and 2.16.0
            + "    'object1': {"
            + "        'target': 'target2' " // Found in <= 2.15.3, but not in 2.16.0
            + "    },"
            + "    'object2': {"
            + "        'target': { " // Found in <= 2.15.3, but not in 2.16.0
            + "            'target': 'ignoredAsParentIsTarget'" // Expect not to be found (as sub-tree search ends when parent is found)
            + "        }"
            + "    }"
            + "}");

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testNonMatching() throws Exception
    {
        JsonNode root = MAPPER.readTree(JSON_SAMPLE);

        assertNull(root.findValue("boogaboo"));
        assertNull(root.findParent("boogaboo"));
        JsonNode n = root.findPath("boogaboo");
        assertNotNull(n);
        assertTrue(n.isMissingNode());

        assertTrue(root.findValues("boogaboo").isEmpty());
        assertTrue(root.findParents("boogaboo").isEmpty());
    }

    @Test
    public void testMatchingSingle() throws Exception
    {
        JsonNode root = MAPPER.readTree(JSON_SAMPLE);

        JsonNode node = root.findValue("b");
        assertNotNull(node);
        assertEquals(3, node.intValue());
        node = root.findParent("b");
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(1, ((ObjectNode) node).size());
        assertEquals(3, node.path("b").intValue());
    }

    @Test
    public void testMatchingMultiple() throws Exception
    {
        JsonNode root = MAPPER.readTree(JSON_SAMPLE);

        List<JsonNode> nodes = root.findValues("value");
        assertEquals(2, nodes.size());
        // here we count on nodes being returned in order; true with Jackson:
        assertEquals(3, nodes.get(0).intValue());
        assertEquals(42, nodes.get(1).intValue());

        nodes = root.findParents("value");
        assertEquals(2, nodes.size());
        // should only return JSON Object nodes:
        assertTrue(nodes.get(0).isObject());
        assertTrue(nodes.get(1).isObject());
        assertEquals(3, nodes.get(0).path("value").intValue());
        assertEquals(42, nodes.get(1).path("value").intValue());

        // and finally, convenience conversion method
        List<String> values = root.findValuesAsText("value");
        assertEquals(2, values.size());
        assertEquals("3", values.get(0));
        assertEquals("42", values.get(1));
    }

    // [databind#4229]: regression in 2.16.0
    @Test
    public void testFindValues4229() throws Exception
    {
        JsonNode rootNode = MAPPER.readTree(JSON_4229);
        assertEquals(Arrays.asList(
                rootNode.at("/target"),
                rootNode.at("/object1/target"),
                rootNode.at("/object2/target")),
                rootNode.findValues("target"));
    }

    // [databind#4229]: regression in 2.16.0
    @Test
    public void testFindParents4229() throws Exception {
        JsonNode rootNode = MAPPER.readTree(JSON_4229);
        assertEquals(Arrays.asList(
                rootNode,
                rootNode.at("/object1"),
                rootNode.at("/object2")),
            rootNode.findParents("target"));
    }
}
