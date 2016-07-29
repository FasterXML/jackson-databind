package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.databind.ObjectMapper;

// Not sure if this is valid, but for what it's worth, shows
// the thing wrt [databind#1311]. May be removed if we can't find
// improvements.
public class TestSubtypes1311 extends com.fasterxml.jackson.databind.BaseMapTest
{
    // [databind#1311]
    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, defaultImpl = Factory1311ImplA.class)
    interface Factory1311 { }

    @JsonTypeName("implA")
    static class Factory1311ImplA implements Factory1311 { }

    @JsonTypeName("implB")
    static class Factory1311ImplB implements Factory1311 { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // [databind#1311]
    public void testSubtypeAssignmentCheck() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(Factory1311ImplA.class, Factory1311ImplB.class);
        Factory1311ImplB result = mapper.readValue("{\"type\":\"implB\"}", Factory1311ImplB.class);
        assertNotNull(result);
        assertEquals(Factory1311ImplB.class, result.getClass());
    }
}
