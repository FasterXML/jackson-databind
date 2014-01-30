package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.NumericNode;
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
        JsonNode n = objectMapper().createArrayNode();
        assertEquals(n, n.remove(JsonPointer.compile("")));
    }
    
    public void testRemoveMissing() {
        try {
            JsonNode n = objectMapper().createObjectNode();
            n.remove(JsonPointer.compile("/o/i"));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    /*
    /**********************************************************
    /* ArrayNode
    /**********************************************************
     */
    
    public void testRemoveArrayRootPath() {
        try {
            JsonNode n = objectMapper().createArrayNode();
            assertEquals(n, n.remove(JsonPointer.compile("/")));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }
    
    public void testRemoveArrayDepth1() {
        JsonNode n = objectMapper().createArrayNode().add(1).add(2).add(3);
        
        n.remove(JsonPointer.compile("/1"));
        assertEquals(1, n.get(0).asInt());
        assertEquals(3, n.get(1).asInt());
    }
    
    public void testRemoveArrayDepth2() {
        ObjectNode n = objectMapper().createObjectNode();
        n.set("a", n.arrayNode().add(1).add(2).add(3));
        
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
            JsonNode n = objectMapper().createObjectNode();
            assertEquals(n, n.remove(JsonPointer.compile("/")));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }
    
    public void testRemoveObjectDepth1() {
        ObjectNode n = objectMapper().createObjectNode();
        n.set("i", n.numberNode(1));
        assertEquals(1,  n.remove(JsonPointer.compile("/i")).asInt());
    }
    
    public void testRemoveObjectDepth2() {
        ObjectNode n = objectMapper().createObjectNode();
        n.set("o", n.objectNode().set("i", n.numberNode(1)));
        assertEquals(1, n.remove(JsonPointer.compile("/o/i")).asInt());
    }
    
    /*
    /**********************************************************
    /* ValueNode
    /**********************************************************
     */
    
    public void testValueRemoveRootPath() {
        try {
            NumericNode n = objectMapper().getNodeFactory().numberNode(1);
            assertEquals(n, n.remove(JsonPointer.compile("/")));
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }
    
}
