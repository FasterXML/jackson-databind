package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.BaseMapTest;

abstract class NodeTestBase extends BaseMapTest
{
    protected void assertNodeNumbersForNonNumeric(JsonNode n)
    { 
        assertFalse(n.isNumber());
        assertEquals(0, n.asInt());
        assertEquals(-42, n.asInt(-42));
        assertEquals(0, n.asLong());
        assertEquals(12345678901L, n.asLong(12345678901L));
        assertEquals(0.0, n.asDouble());
        assertEquals(-19.25, n.asDouble(-19.25));
    }
    
    protected void assertNodeNumbers(JsonNode n, int expInt, double expDouble)
    {
        assertEquals(expInt, n.asInt());
        assertEquals(expInt, n.asInt(-42));
        assertEquals((long) expInt, n.asLong());
        assertEquals((long) expInt, n.asLong(19L));
        assertEquals(expDouble, n.asDouble());
        assertEquals(expDouble, n.asDouble(-19.25));
    }

}
