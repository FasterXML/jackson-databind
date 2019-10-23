package com.fasterxml.jackson.databind.ser;

import java.util.*;


import com.fasterxml.jackson.databind.*;

/**
 * Simple unit tests to verify that it is possible to handle
 * potentially cyclic structures, as long as object graph itself
 * is not cyclic. This is the case for directed hierarchies like
 * trees and DAGs.
 */
public class CyclicTypeSerTest
    extends BaseMapTest
{
    static class Bean
    {
        Bean _next;
        final String _name;

        public Bean(Bean next, String name) {
            _next = next;
            _name = name;
        }

        public Bean getNext() { return _next; }
        public String getName() { return _name; }

        public void assignNext(Bean n) { _next = n; }
    }

    /*
    /**********************************************************
    /* Types
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testLinked() throws Exception
    {
        Bean last = new Bean(null, "last");
        Bean first = new Bean(last, "first");
        Map<String,Object> map = writeAndMap(MAPPER, first);

        assertEquals(2, map.size());
        assertEquals("first", map.get("name"));

        @SuppressWarnings("unchecked")
        Map<String,Object> map2 = (Map<String,Object>) map.get("next");
        assertNotNull(map2);
        assertEquals(2, map2.size());
        assertEquals("last", map2.get("name"));
        assertNull(map2.get("next"));
    }

    /**
     * Test for verifying that [JACKSON-158] works as expected
     */
    public void testSelfReference() throws Exception
    {
        Bean selfRef = new Bean(null, "self-refs");
        Bean first = new Bean(selfRef, "first");
        selfRef.assignNext(selfRef);
        Bean[] wrapper = new Bean[] { first };
        try {
            writeAndMap(MAPPER, wrapper);
        } catch (JsonMappingException e) {
            verifyException(e, "Direct self-reference leading to cycle");
        }
    }
}
