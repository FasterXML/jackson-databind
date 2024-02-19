package com.fasterxml.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that annotations are shared and merged between members
 * of a property (getter and setter and so on)
 */
public class TestAnnotationMerging extends DatabindTestUtil
{
    static class Wrapper
    {
        protected Object value;

        public Wrapper() { }
        public Wrapper(Object o) { value = o; }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object getValue() { return value; }

        public void setValue(Object o) { value = o; }
    }

    static class SharedName {
        @JsonProperty("x")
        protected int value;

        public SharedName(int v) { value = v; }

        public int getValue() { return value; }
    }

    static class SharedName2
    {
        @JsonProperty("x")
        public int getValue() { return 1; }
        public void setValue(int x) { }
    }

    // Testing to ensure that ctor param and getter can "share" @JsonTypeInfo stuff
    static class TypeWrapper
    {
        protected Object value;

        @JsonCreator
        public TypeWrapper(
                @JsonProperty("value")
                @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Object o) {
            value = o;
        }
        public Object getValue() { return value; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    @Test
    public void testSharedNames() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("{\"x\":6}", mapper.writeValueAsString(new SharedName(6)));
    }

    @Test
    public void testSharedNamesFromGetterToSetter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new SharedName2());
        assertEquals("{\"x\":1}", json);
        SharedName2 result = mapper.readValue(json, SharedName2.class);
        assertNotNull(result);
    }

    @Test
    public void testSharedTypeInfo() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new Wrapper(13L));
        Wrapper result = mapper.readValue(json, Wrapper.class);
        assertEquals(Long.class, result.value.getClass());
    }

    @Test
    public void testSharedTypeInfoWithCtor() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new TypeWrapper(13L));
        TypeWrapper result = mapper.readValue(json, TypeWrapper.class);
        assertEquals(Long.class, result.value.getClass());
    }
}
