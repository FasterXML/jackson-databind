package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Unit test for verifying that {@link AnnotatedClass}
 * works as expected.
 */
public class TestAnnotatedClass
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    static class BaseClass
    {
        public int foo;

        public BaseClass(int x, int y) { }

        @JsonProperty public int x() { return 3; }
    }

    static class SubClass extends BaseClass
    {
        public SubClass() { this(1); }
        public SubClass(int x) { super(x, 2); }

        public int y() { return 3; }
    }

    static abstract class GenericBase<T extends Number>
    {
        public abstract void setX(T value);
    }

    static class NumberBean
        extends GenericBase<Integer>
    {
        @Override
        public void setX(Integer value) { }
    }

    /**
     * Test class for checking that field introspection
     * works as expected
     */
    @SuppressWarnings("unused")
    static class FieldBean
    {
        // static, not to be included:
        public static boolean DUMMY;

        // not public, no annotations, shouldn't be included
        private long bar;

        @JsonProperty
        private String props;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testFieldIntrospection()
    {
        // null -> no mix-in annotations
        AnnotatedClass ac = AnnotatedClass.construct(FieldBean.class, new JacksonAnnotationIntrospector(), null);
        // AnnotatedClass does not ignore non-visible fields, yet
        assertEquals(2, ac.getFieldCount());
        for (AnnotatedField f : ac.fields()) {
            String fname = f.getName();
            if (!"bar".equals(fname) && !"props".equals(fname)) {
                fail("Unexpected field name '"+fname+"'");
            }
        }
    }
}
