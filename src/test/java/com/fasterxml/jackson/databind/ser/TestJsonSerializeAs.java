package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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

    // for [databind#1023]
    static class FooableWithFieldWrapper {
        @JsonSerialize(as=Fooable.class)
        public Fooable getFoo() {
            return new FooImplNoAnno();
        }
    }

    interface Bean1178Base {
        public int getA();
    }

    @JsonPropertyOrder({"a","b"})
    static abstract class Bean1178Abstract implements Bean1178Base {
        @Override
        public int getA() { return 1; }

        public int getB() { return 2; }
    }

    static class Bean1178Impl extends Bean1178Abstract {
        public int getC() { return 3; }
    }

    static class Bean1178Wrapper {
        @JsonSerialize(contentAs=Bean1178Abstract.class)
        public List<Bean1178Base> values;
        public Bean1178Wrapper(int count) {
            values = new ArrayList<Bean1178Base>();
            for (int i = 0; i < count; ++i) {
                values.add(new Bean1178Impl());
            }
        }
    }

    static class Bean1178Holder {
        @JsonSerialize(as=Bean1178Abstract.class)
        public Bean1178Base value = new Bean1178Impl();
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

    // for [databind#1023]
    public void testSerializeWithFieldAnno() throws IOException {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new FooableWithFieldWrapper()));
    }

    // for [databind#1178]
    public void testSpecializedContentAs() throws IOException {
        assertEquals(a2q("{'values':[{'a':1,'b':2}]}"),
                WRITER.writeValueAsString(new Bean1178Wrapper(1)));
    }

    // for [databind#1231] (and continuation of [databind#1178])
    public void testSpecializedAsIntermediate() throws IOException {
        assertEquals(a2q("{'value':{'a':1,'b':2}}"),
                WRITER.writeValueAsString(new Bean1178Holder()));
    }
}
