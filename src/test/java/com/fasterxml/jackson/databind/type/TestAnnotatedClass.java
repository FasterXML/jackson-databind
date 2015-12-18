package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;

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

    abstract static class GenericBase<T extends Number>
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

    // for [databind#1005]
    static class Bean1005 {
        // private to force creation of a synthetic constructor to avoid access issues
        private Bean1005(int i) {}     
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testFieldIntrospection()
    {
        SerializationConfig config = MAPPER.getSerializationConfig();
        JavaType t = MAPPER.constructType(FieldBean.class);
        AnnotatedClass ac = AnnotatedClass.construct(t, config);
        // AnnotatedClass does not ignore non-visible fields, yet
        assertEquals(2, ac.getFieldCount());
        for (AnnotatedField f : ac.fields()) {
            String fname = f.getName();
            if (!"bar".equals(fname) && !"props".equals(fname)) {
                fail("Unexpected field name '"+fname+"'");
            }
        }
    }

    // For [databind#1005]
    public void testConstructorIntrospection()
    {
        // Need this call to ensure there is a synthetic constructor being generated
        // (not really needed otherwise)
        Bean1005 bean = new Bean1005(13);
        SerializationConfig config = MAPPER.getSerializationConfig();
        JavaType t = MAPPER.constructType(bean.getClass());
        AnnotatedClass ac = AnnotatedClass.construct(t, config);
        assertEquals(1, ac.getConstructors().size());
    }
}
