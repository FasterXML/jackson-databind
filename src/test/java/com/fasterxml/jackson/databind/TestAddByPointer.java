package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Basic tests to ensure we can add into the tree using JSON pointers.
 */
public class TestAddByPointer extends BaseMapTest {
 
    private final JsonNode ONE = new IntNode(1);
    
    /*
    /**********************************************************
    /* JsonNode
    /**********************************************************
     */
    
    // TODO It would be nice to have a "TestNode" to isolate implementations in the base class

    public void testAddEmpty() {
        try {
            JsonNode n = new ArrayNode(JsonNodeFactory.instance);
            n.add(JsonPointer.compile(""), ONE);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testAddRootPath() {
        JsonNode n = new ArrayNode(JsonNodeFactory.instance);
        assertEquals(ONE, n.add(JsonPointer.compile("/"), ONE));
    }
    
    /*
    /**********************************************************
    /* ArrayNode
    /**********************************************************
     */
    
    public void testAddArrayDepth1() {
        JsonNode n = new ArrayNode(JsonNodeFactory.instance);
        
        n.add(JsonPointer.compile("/0"), new IntNode(1));
        assertEquals(1, n.get(0).asInt());
        
        n.add(JsonPointer.compile("/0"), new IntNode(2));
        assertEquals(2, n.get(0).asInt());
        assertEquals(1, n.get(1).asInt());
        
        n.add(JsonPointer.compile("/-"), new IntNode(3)); // special case: append
        assertEquals(2, n.get(0).asInt());
        assertEquals(1, n.get(1).asInt());
        assertEquals(3, n.get(2).asInt());
    }
    
    public void testAddArrayDepth2() {
        JsonNode n = new ObjectNode(JsonNodeFactory.instance);
        ((ObjectNode) n).set("a", new ArrayNode(JsonNodeFactory.instance));
        JsonPointer A_APPEND = JsonPointer.compile("/a/-");
        n.add(A_APPEND, new IntNode(1));
        n.add(A_APPEND, new IntNode(2));
        n.add(A_APPEND, new IntNode(3));
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
            JsonNode n = new ArrayNode(JsonNodeFactory.instance);
            n.add(JsonPointer.compile("/a"), ONE);
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
        JsonNode n = new ObjectNode(JsonNodeFactory.instance);
        n.add(JsonPointer.compile("/a"), ONE);
        assertEquals(1, n.get("a").asInt());
    }
    
    public void testAddObjectDepth2() {
        JsonNode n = new ObjectNode(JsonNodeFactory.instance);
        ((ObjectNode) n).set("o", new ObjectNode(JsonNodeFactory.instance));
        n.add(JsonPointer.compile("/o/i"), ONE);
        assertEquals(1, n.at("/o/i").asInt());
    }
    
    /*
    /**********************************************************
    /* ValueNode
    /**********************************************************
     */

    public void testAddValue() {
        try {
            ONE.add(JsonPointer.compile("/0"), ONE);
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }
    
}
