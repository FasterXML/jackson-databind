package com.fasterxml.jackson.databind.jsontype.jdk;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

class TypedArrayDeserTest
        extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Let's claim we need type here too (although we won't
     * really use any sub-classes)
     */
    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
    static class TypedList<T> extends ArrayList<T> { }

    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
    static class TypedListAsProp<T> extends ArrayList<T> { }

    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    static class TypedListAsWrapper<T> extends LinkedList<T> { }

    // Mix-in to force wrapper for things like primitive arrays
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    interface WrapperMixIn { }

    /*
    /**********************************************************
    /* Unit tests, Lists
    /**********************************************************
     */

    @Test
    void testIntList() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // uses WRAPPER_OBJECT inclusion
        String JSON = "{\""+TypedListAsWrapper.class.getName()+"\":[4,5, 6]}";
        JavaType type = defaultTypeFactory().constructCollectionType(TypedListAsWrapper.class, Integer.class);
        TypedListAsWrapper<Integer> result = m.readValue(JSON, type);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(4), result.get(0));
        assertEquals(Integer.valueOf(5), result.get(1));
        assertEquals(Integer.valueOf(6), result.get(2));
    }

    /**
     * Similar to above, but this time let's request adding type info
     * as property. That would not work (since there's no JSON Object to
     * add property in), so it will basically be same as using WRAPPER_ARRAY
     */
    @Test
    void testBooleanListAsProp() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // tries to use PROPERTY inclusion; but for ARRAYS (and scalars) will become ARRAY_WRAPPER
        String JSON = "[\""+TypedListAsProp.class.getName()+"\",[true, false]]";
        JavaType type = defaultTypeFactory().constructCollectionType(TypedListAsProp.class, Boolean.class);
        TypedListAsProp<Object> result = m.readValue(JSON, type);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Boolean.TRUE, result.get(0));
        assertEquals(Boolean.FALSE, result.get(1));
    }

    @Test
    void testLongListAsWrapper() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // uses OBJECT_ARRAY, works just fine

        String JSON = "{\""+TypedListAsWrapper.class.getName()+"\":[1, 3]}";
        JavaType type = defaultTypeFactory().constructCollectionType(TypedListAsWrapper.class, Long.class);
        TypedListAsWrapper<Object> result = m.readValue(JSON, type);
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(Long.class, result.get(0).getClass());
        assertEquals(Long.valueOf(1), result.get(0));
        assertEquals(Long.class, result.get(1).getClass());
        assertEquals(Long.valueOf(3), result.get(1));
    }

    /*
    /**********************************************************
    /* Unit tests, primitive arrays
    /**********************************************************
     */

    @Test
    void testLongArray() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // use class name, WRAPPER_OBJECT
        m.addMixIn(long[].class, WrapperMixIn.class);
        String JSON = "{\""+long[].class.getName()+"\":[5, 6, 7]}";
        long[] value = m.readValue(JSON, long[].class);
        assertNotNull(value);
        assertEquals(3, value.length);
        assertArrayEquals(new long[] { 5L, 6L, 7L} , value);
    }
}
