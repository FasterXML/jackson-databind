package com.fasterxml.jackson.databind.ser;

import java.io.IOException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class TestJsonSerializeAs extends BaseMapTest
{
    // [JACKSON-799] stuff:
    
    public interface Fooable {
        public int getFoo();
    }

    // force use of interface
    @JsonSerialize(as=Fooable.class)
    public static class FooImpl implements Fooable {
        @Override
        public int getFoo() { return 42; }
        public int getBar() { return 15; }
    }

    public class Fooables {
        public FooImpl[] getFoos() {
            return new FooImpl[] { new FooImpl() };
        }
    }

    public class FooableWrapper {
        public FooImpl getFoo() {
            return new FooImpl();
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectWriter WRITER = objectWriter();
    
    // [JACKSON-799]
    public void testSerializeAsInClass() throws IOException
    {
        assertEquals("{\"foo\":42}", WRITER.writeValueAsString(new FooImpl()));
    }

    public void testSerializeAsForArrayProp() throws IOException
    {
        assertEquals("{\"foos\":[{\"foo\":42}]}", WRITER.writeValueAsString(new Fooables()));
    }

    public void testSerializeAsForSimpleProp() throws IOException
    {
        assertEquals("{\"foo\":{\"foo\":42}}", WRITER.writeValueAsString(new FooableWrapper()));
    }
}
