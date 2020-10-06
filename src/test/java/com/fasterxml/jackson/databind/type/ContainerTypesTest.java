package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.LookupCache;
import com.fasterxml.jackson.databind.util.UnlimitedLookupCache;

// for [databind#1415]
public class ContainerTypesTest extends BaseMapTest
{
    static abstract class LongList implements List<Long> { }

    static abstract class StringLongMap implements Map<String,Long> { }

    /*
    /**********************************************************
    /* Unit tests, success
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testExplicitCollectionType() throws Exception
    {
        JavaType t = MAPPER.getTypeFactory()
                .constructCollectionType(LongList.class, Long.class);
        assertEquals(LongList.class, t.getRawClass());
        assertEquals(Long.class, t.getContentType().getRawClass());
    }

    public void testImplicitCollectionType() throws Exception
    {
        JavaType t = MAPPER.getTypeFactory()
                .constructParametricType(List.class, Long.class);
        assertEquals(CollectionType.class, t.getClass());
        assertEquals(List.class, t.getRawClass());
        assertEquals(Long.class, t.getContentType().getRawClass());
    }

    // [databind#1725]
    public void testMissingCollectionType() throws Exception
    {
        TypeFactory tf = MAPPER.getTypeFactory().withCache((LookupCache<Object,JavaType>)new LRUMap<Object,JavaType>(4, 8));
        JavaType t = tf.constructParametricType(List.class, HashMap.class);
        assertEquals(CollectionType.class, t.getClass());
        assertEquals(List.class, t.getRawClass());
        assertEquals(HashMap.class, t.getContentType().getRawClass());
    }

    public void testCustomLookupCache() throws Exception
    {
        TypeFactory tf = MAPPER.getTypeFactory().withCache(new UnlimitedLookupCache<Object, JavaType>(0));
        JavaType t = tf.constructParametricType(List.class, HashMap.class);
        assertEquals(CollectionType.class, t.getClass());
        assertEquals(List.class, t.getRawClass());
        assertEquals(HashMap.class, t.getContentType().getRawClass());
    }

    public void testExplicitMapType() throws Exception
    {
        JavaType t = MAPPER.getTypeFactory()
                .constructMapType(StringLongMap.class,
                        String.class, Long.class);
        assertEquals(StringLongMap.class, t.getRawClass());
        assertEquals(String.class, t.getKeyType().getRawClass());
        assertEquals(Long.class, t.getContentType().getRawClass());
    }

    public void testImplicitMapType() throws Exception
    {
        JavaType t = MAPPER.getTypeFactory()
                .constructParametricType(Map.class, Long.class, Boolean.class);
        assertEquals(MapType.class, t.getClass());
        assertEquals(Long.class, t.getKeyType().getRawClass());
        assertEquals(Boolean.class, t.getContentType().getRawClass());
    }

    /*
    /**********************************************************
    /* Unit tests, fails
    /**********************************************************
     */

    public void testMismatchedCollectionType() throws Exception
    {
        try {
            MAPPER.getTypeFactory()
                .constructCollectionType(LongList.class, String.class);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "`"+getClass().getName()+"$LongList` did not resolve to something");
            verifyException(e, "element type");
        }
    }

    public void testMismatchedMapType() throws Exception
    {
        // first, mismatched key type
        try {
            MAPPER.getTypeFactory()
                .constructMapType(StringLongMap.class, Boolean.class, Long.class);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "`"+getClass().getName()+"$StringLongMap` did not resolve to something");
            verifyException(e, "key type");
        }
        // then, mismatched value type
        try {
            MAPPER.getTypeFactory()
                .constructMapType(StringLongMap.class, String.class, Class.class);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "`"+getClass().getName()+"$StringLongMap` did not resolve to something");
            verifyException(e, "value type");
        }
    }
}

