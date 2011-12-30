package com.fasterxml.jackson.databind.interop;

import java.util.*;

import groovy.lang.GroovyClassLoader;

import com.fasterxml.jackson.databind.*;

/**
 * Basic tests to see that simple Groovy beans can be serialized
 * and deserialized
 */
public class TestGroovyBeans
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    final static String SIMPLE_POGO = 
        "public class GBean {\n"
        +"long id = 3;\n"
        +"String name = \"whome\";\n"
        +"}";

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleSerialization() throws Exception
    {
        Object ob = newGroovyObject(SIMPLE_POGO);
        Map<String,Object> result = writeAndMap(MAPPER, ob);
        assertEquals(2, result.size());
        assertEquals("whome", result.get("name"));
        /* 26-Nov-2009, tatu: Strange... Groovy seems to decide
         *    'long' means 'int'... Oh well.
         */
        Object num = result.get("id");
        assertNotNull(num);
        assertTrue(num instanceof Number);
        assertEquals(3, ((Number) num).intValue());
    }

    public void testSimpleDeserialization() throws Exception
    {
        Class<?> cls = defineGroovyClass(SIMPLE_POGO);
        // First: deserialize from data
        Object pogo = MAPPER.readValue("{\"id\":9,\"name\":\"Bob\"}", cls);
        assertNotNull(pogo);
        /* Hmmh. Could try to access using Reflection, or by defining
         * a Java interface it implements. Or, maybe simplest, just
         * re-serialize and see what we got.
         */
        Map<String,Object> result = writeAndMap(MAPPER, pogo);
        assertEquals(2, result.size());
        assertEquals("Bob", result.get("name"));
        // as per earlier, we just get a number...
        Object num = result.get("id");
        assertNotNull(num);
        assertTrue(num instanceof Number);
        assertEquals(9, ((Number) num).intValue());
    }

    /*
    *************************************************
    * Helper methods
    *************************************************
    */

    protected Class<?> defineGroovyClass(String src) throws Exception
    {
        return new GroovyClassLoader(getClass().getClassLoader()).parseClass(src);

    }

    protected Object newGroovyObject(String src) throws Exception
    {
        Class<?> cls = defineGroovyClass(src);
        return cls.newInstance();
    }
}
