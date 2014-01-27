package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Basic tests to ensure we can remove from the tree using JSON pointers.
 */
public class TestRemoveByPointer extends BaseMapTest {

    /*
    /**********************************************************
    /* JsonNode
    /**********************************************************
     */
    
    // TODO It would be nice to have a "TestNode" to isolate implementations in the base class

    public void testRemoveEmpty() {
        JsonNode n = new ArrayNode(JsonNodeFactory.instance);
        assertEquals(n, n.remove(JsonPointer.compile("")));
    }

    /*
    /**********************************************************
    /* ArrayNode
    /**********************************************************
     */
    
    public void testRemoveArrayRootPath() {
        try {
            JsonNode n = new ArrayNode(JsonNodeFactory.instance);
            assertEquals(n, n.remove(JsonPointer.compile("/")));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }
    
    public void testRemoveArrayDepth1() {
        JsonNode n = new ArrayNode(JsonNodeFactory.instance).add(1).add(2).add(3);
        
        n.remove(JsonPointer.compile("/1"));
        assertEquals(1, n.get(0).asInt());
        assertEquals(3, n.get(1).asInt());
    }
    
    public void testRemoveArrayDepth2() {
        JsonNode n = new ObjectNode(JsonNodeFactory.instance);
        ((ObjectNode) n).set("a", new ArrayNode(JsonNodeFactory.instance).add(1).add(2).add(3));
        
        n.remove(JsonPointer.compile("/a/1"));
        assertEquals(1, n.at("/a/0").asInt());
        assertEquals(3, n.at("/a/1").asInt());
    }
    
    /*
    /**********************************************************
    /* ObjectNode
    /**********************************************************
     */

    public void testObjectRemoveRootPath() {
        try {
            JsonNode n = new ObjectNode(JsonNodeFactory.instance);
            assertEquals(n, n.remove(JsonPointer.compile("/")));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }
    
    public void testRemoveObjectDepth1() {
        JsonNode n = new ObjectNode(JsonNodeFactory.instance).set("i", new IntNode(1));
        assertEquals(1,  n.remove(JsonPointer.compile("/i")).asInt());
    }
    
    public void testRemoveObjectDepth2() {
        JsonNode n = new ObjectNode(JsonNodeFactory.instance);
        ((ObjectNode) n).set("o", new ObjectNode(JsonNodeFactory.instance).set("i", new IntNode(1)));
        assertEquals(1, n.remove(JsonPointer.compile("/o/i")).asInt());
    }
    
    /*
    /**********************************************************
    /* ValueNode
    /**********************************************************
     */
    
    public void testValueRemoveRootPath() {
        try {
            JsonNode n = new IntNode(1);
            assertEquals(n, n.remove(JsonPointer.compile("/")));
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }
    
}
