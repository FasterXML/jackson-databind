package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

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

    @JsonPropertyOrder({ "id", "parent" })
    static class Selfie2501 {
        public int id;

        public Selfie2501 parent;

        public Selfie2501(int id) { this.id = id; }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class Selfie2501AsArray extends Selfie2501 {
        public Selfie2501AsArray(int id) { super(id); }
    }

    /*
    /**********************************************************
    /* Types
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testLinkedButNotCyclic() throws Exception
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

    public void testSimpleDirectSelfReference() throws Exception
    {
        Bean selfRef = new Bean(null, "self-refs");
        Bean first = new Bean(selfRef, "first");
        selfRef.assignNext(selfRef);
        Bean[] wrapper = new Bean[] { first };
        try {
            writeAndMap(MAPPER, wrapper);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Direct self-reference leading to cycle");
        }
    }

    // [databind#2501]: Should be possible to replace null cyclic ref
    public void testReplacedCycle() throws Exception
    {
        Selfie2501 self1 = new Selfie2501(1);
        self1.parent = self1;
        ObjectWriter w = MAPPER.writer()
                .without(SerializationFeature.FAIL_ON_SELF_REFERENCES)
                .with(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)
                ;
        assertEquals(a2q("{'id':1,'parent':null}"), w.writeValueAsString(self1));

        // Also consider a variant of cyclic POJO in container
        Selfie2501AsArray self2 = new Selfie2501AsArray(2);
        self2.parent = self2;
        assertEquals(a2q("[2,null]"), w.writeValueAsString(self2));
    }
}
