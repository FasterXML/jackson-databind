package com.fasterxml.jackson.databind.deser;

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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testLinked() throws Exception
    {
        Bean first = new ObjectMapper().readValue
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
        StringLink link = new ObjectMapper().readValue
            ("{\"next\":null}", StringLink.class);
        assertNotNull(link);
        assertNull(link.next);
    }

    public void testCycleWith2Classes() throws Exception
    {
        LinkA a = new ObjectMapper().readValue("{\"next\":{\"a\":null}}", LinkA.class);
        assertNotNull(a.next);
        LinkB b = a.next;
        assertNull(b.a);
    }
}
