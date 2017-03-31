package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

// for [databind#1415]
public class CollectionType1415Test extends BaseMapTest
{
    static abstract class LongList implements List<Long> { }

    static abstract class StringLongMap implements Map<String,Long> { }

    /*
    /**********************************************************
    /* Unit tests
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
}
