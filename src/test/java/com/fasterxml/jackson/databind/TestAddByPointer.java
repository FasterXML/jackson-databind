package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Basic tests to ensure we can add into the tree using JSON pointers.
 */
public class TestAddByPointer extends BaseMapTest {

    /*
    /**********************************************************
    /* JsonNode
    /**********************************************************
     */
    
    // TODO It would be nice to have a "TestNode" to isolate implementations in the base class

    public void testAddEmpty() {
        try {
            ArrayNode n = objectMapper().createArrayNode();
            n.add(JsonPointer.compile(""), n.numberNode(1));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testAddRootPath() {
        ArrayNode n = objectMapper().createArrayNode();
        JsonNode v = n.numberNode(1);
        assertEquals(v, n.add(JsonPointer.compile("/"), v));
    }
    
    /*
    /**********************************************************
    /* ArrayNode
    /**********************************************************
     */
    
    public void testAddArrayDepth1() {
        ArrayNode n = objectMapper().createArrayNode();
        
        n.add(JsonPointer.compile("/0"), n.numberNode(1));
        assertEquals(1, n.get(0).asInt());
        
        n.add(JsonPointer.compile("/0"), n.numberNode(2));
        assertEquals(2, n.get(0).asInt());
        assertEquals(1, n.get(1).asInt());
        
        n.add(JsonPointer.compile("/-"), n.numberNode(3)); // special case: append
        assertEquals(2, n.get(0).asInt());
        assertEquals(1, n.get(1).asInt());
        assertEquals(3, n.get(2).asInt());
    }
    
    public void testAddArrayDepth2() {
        ObjectNode n = objectMapper().createObjectNode();
        n.set("a", n.arrayNode());
        JsonPointer A_APPEND = JsonPointer.compile("/a/-");
        n.add(A_APPEND, n.numberNode(1));
        n.add(A_APPEND, n.numberNode(2));
        n.add(A_APPEND, n.numberNode(3));
        assertEquals(1, n.at("/a/0").asInt());
        assertEquals(2, n.at("/a/1").asInt());
        assertEquals(3, n.at("/a/2").asInt());
    }
    
    /**
     * RFC 6902 isn't clear about what this behavior should be: error, silent
     * ignore or perform insert. We error out to allow higher level
     * implementations the opportunity to handle the problem as they see fit.
     */
    public void testAddInvalidArrayElementPointer() {
        try {
            ArrayNode n = objectMapper().createArrayNode();
            n.add(JsonPointer.compile("/a"), n.numberNode(1));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }
    
    /*
    /**********************************************************
    /* ObjectNode
    /**********************************************************
     */
    
    public void testAddObjectDepth1() {
        ObjectNode n = objectMapper().createObjectNode();
        n.add(JsonPointer.compile("/a"), n.numberNode(1));
        assertEquals(1, n.get("a").asInt());
    }
    
    public void testAddObjectDepth2() {
        ObjectNode n = objectMapper().createObjectNode();
        n.set("o", n.objectNode());
        n.add(JsonPointer.compile("/o/i"), n.numberNode(1));
        assertEquals(1, n.at("/o/i").asInt());
    }
    
    /*
    /**********************************************************
    /* ValueNode
    /**********************************************************
     */

    public void testAddValue() {
        try {
            NumericNode n = objectMapper().getNodeFactory().numberNode(1);
            n.add(JsonPointer.compile("/0"), objectMapper().getNodeFactory().numberNode(2));
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }
    
}
