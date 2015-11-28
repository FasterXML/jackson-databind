package com.fasterxml.jackson.databind.ser;

import java.io.IOException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class TestJsonSerializeAs extends BaseMapTest
{
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

    static class FooImplNoAnno implements Fooable {
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

    // Also test via Field
    static class FooableWithFieldWrapper {
        @JsonSerialize(as=Fooable.class)
        public Fooable getFoo() {
            return new FooImplNoAnno();
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectWriter WRITER = objectWriter();

    public void testSerializeAsInClass() throws IOException {
        assertEquals("{\"foo\":42}", WRITER.writeValueAsString(new FooImpl()));
    }

    public void testSerializeAsForArrayProp() throws IOException {
        assertEquals("{\"foos\":[{\"foo\":42}]}",
                WRITER.writeValueAsString(new Fooables()));
    }

    public void testSerializeAsForSimpleProp() throws IOException {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new FooableWrapper()));
    }

    public void testSerializeWithFieldAnno() throws IOException {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new FooableWithFieldWrapper()));
    }
}
