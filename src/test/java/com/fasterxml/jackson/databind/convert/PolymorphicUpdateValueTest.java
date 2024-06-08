package com.fasterxml.jackson.databind.convert;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Unit tests for verifying handling of update value on polymorphic
 * objects.
 */
public class PolymorphicUpdateValueTest
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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
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
