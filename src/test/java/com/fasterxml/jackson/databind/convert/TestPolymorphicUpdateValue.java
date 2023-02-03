package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying handling of update value on polymorphic
 * objects.
 */
public class TestPolymorphicUpdateValue extends BaseMapTest
{
    @JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_ARRAY //PROPERTY
            ,use=JsonTypeInfo.Id.NAME, property="type")
    @JsonSubTypes(value={ @JsonSubTypes.Type(value=Child.class)})
    abstract static class Parent {
        public int x;
        public int y;
    }

    @JsonTypeName("child")
    public static class Child extends Parent {
        public int w;
        public int h;
    }

    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testPolymorphicTest() throws Exception
    {
         Child c = new Child();
         c.w = 10;
         c.h = 11;
         MAPPER.readerForUpdating(c).readValue("{\"x\":3,\"y\":4,\"w\":111}");
         assertEquals(3, c.x);
         assertEquals(4, c.y);
         assertEquals(111, c.w);
    }
}
