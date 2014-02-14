package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.*;

/**
 * Simple unit tests to verify that it is possible to handle
 * potentially cyclic structures, as long as object graph itself
 * is not cyclic. This is the case for directed hierarchies like
 * trees and DAGs.
 */
public class TestCyclicTypes
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    static class Bean
    {
        Bean _next;
        String _name;

        public Bean() { }

        public void setNext(Bean b) { _next = b; }
        public void setName(String n) { _name = n; }

    }

    static class LinkA {
        public LinkB next;
    }

    static class LinkB {
        private LinkA a;

        public void setA(LinkA a) { this.a = a; }
        public LinkA getA() { return a; }
    }

    static class GenericLink<T> {
        public GenericLink<T> next;
    }

    static class StringLink extends GenericLink<String> {
    }

    static class Selfie382 {
        public int id;

        @JsonIgnoreProperties({ "parent", "ignoredRef" })
        public Selfie382 parent;
        
        public Selfie382(int id) { this.id = id; }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    public void testLinked() throws Exception
    {
        Bean first = MAPPER.readValue
            ("{\"name\":\"first\", \"next\": { "
             +" \"name\":\"last\", \"next\" : null }}",
             Bean.class);

        assertNotNull(first);
        assertEquals("first", first._name);
        Bean last = first._next;
        assertNotNull(last);
        assertEquals("last", last._name);
        assertNull(last._next);
    }

    public void testLinkedGeneric() throws Exception
    {
        StringLink link = MAPPER.readValue("{\"next\":null}", StringLink.class);
        assertNotNull(link);
        assertNull(link.next);
    }

    public void testCycleWith2Classes() throws Exception
    {
        LinkA a = MAPPER.readValue("{\"next\":{\"a\":null}}", LinkA.class);
        assertNotNull(a.next);
        LinkB b = a.next;
        assertNull(b.a);
    }

    // [Issue#382]: Should be possible to ignore cyclic ref
    public void testIgnoredCycle() throws Exception
    {
        Selfie382 self1 = new Selfie382(1);
        Selfie382 self2 = new Selfie382(2);
        self1.parent = self2;
        self2.parent = self1;
        String json = MAPPER.writeValueAsString(self1);
        assertNotNull(json);
        assertEquals(aposToQuotes("{'id':1,'parent':{'id':2}}"), json);
    }

}
