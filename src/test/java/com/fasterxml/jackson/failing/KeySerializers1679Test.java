package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.*;

public class KeySerializers1679Test extends BaseMapTest
{
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#1679]
    public void testRecursion1679() throws Exception
    {
        Map<Object, Object> objectMap = new HashMap<Object, Object>();
        objectMap.put(new Object(), new Object());
        String json = MAPPER.writeValueAsString(objectMap);
        assertEquals("{}", json);
    }
}
