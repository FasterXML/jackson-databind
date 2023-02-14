package com.fasterxml.jackson.databind.deser;

import java.io.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * This unit test suite tests use of basic Annotations for
 * bean deserialization; ones that indicate (non-constructor)
 * method types, explicit deserializer annotations.
 */
@SuppressWarnings("serial")
public class TestBasicAnnotations
    extends BaseMapTest
{
    /// Class for testing {@link JsonProperty} annotations
    final static class SizeClassSetter
    {
        int _size;
        int _length;
        int _other;

        @JsonProperty public void size(int value) { _size = value; }
        @JsonProperty("length") public void foobar(int value) { _length = value; }

        // note: need not be public if annotated
        @JsonProperty protected void other(int value) { _other = value; }

        // finally: let's add a red herring that should be avoided...
        public void errorOut(int value) { throw new Error(); }
    }

    static class Issue442Bean {
        @JsonUnwrapped
        protected IntWrapper w = new IntWrapper(13);
    }

    final static class SizeClassSetter2
    {
        int _x;

        @JsonProperty public void setX(int value) { _x = value; }

        // another red herring, which shouldn't be included
        public void setXandY(int x, int y) { throw new Error(); }
    }

    /**
     * One more, but this time checking for implied setter
     * using @JsonDeserialize
     */
    final static class SizeClassSetter3
    {
        int _x;

        @JsonDeserialize public void x(int value) { _x = value; }
    }


    /// Classes for testing Setter discovery with inheritance
    static class BaseBean
    {
        int _x = 0, _y = 0;

        public void setX(int value) { _x = value; }
        @JsonProperty("y") void foobar(int value) { _y = value; }
    }

    static class BeanSubClass extends BaseBean
    {
        int _z;

        public void setZ(int value) { _z = value; }
    }

    static class BeanWithDeserialize {
        @JsonDeserialize protected int a;
    }

    @JsonAutoDetect(setterVisibility=Visibility.NONE)
    final static class Dummy { }

    final static class EmptyDummy { }

    static class AnnoBean {
        int value = 3;

        @JsonProperty("y")
        public void setX(int v) { value = v; }
    }

    enum Alpha { A, B, C; }

    public static class SimpleBean {
        public int x, y;
    }

    /*
    /**********************************************************
    /* Other helper classes
    /**********************************************************
     */

    final static class IntsDeserializer extends StdDeserializer<int[]>
    {
        public IntsDeserializer() { super(int[].class); }
        @Override
        public int[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            return new int[] { jp.getIntValue() };
        }
    }

    /*
    /**********************************************************
    /* Test methods, basic
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleSetter() throws Exception
    {
        SizeClassSetter result = MAPPER.readValue
            ("{ \"other\":3, \"size\" : 2, \"length\" : -999 }",
             SizeClassSetter.class);

        assertEquals(3, result._other);
        assertEquals(2, result._size);
        assertEquals(-999, result._length);
    }

    // Test for checking [JACKSON-64]
    public void testSimpleSetter2() throws Exception
    {
        SizeClassSetter2 result = MAPPER.readValue("{ \"x\": -3 }",
             SizeClassSetter2.class);
        assertEquals(-3, result._x);
    }

    // Checking parts of [JACKSON-120]
    public void testSimpleSetter3() throws Exception
    {
        SizeClassSetter3 result = MAPPER.readValue
            ("{ \"x\": 128 }",
             SizeClassSetter3.class);
        assertEquals(128, result._x);
    }

    /**
     * Test for verifying that super-class setters are used as
     * expected.
     */
    public void testSetterInheritance() throws Exception
    {
        BeanSubClass result = MAPPER.readValue
            ("{ \"x\":1, \"z\" : 3, \"y\" : 2 }",
             BeanSubClass.class);
        assertEquals(1, result._x);
        assertEquals(2, result._y);
        assertEquals(3, result._z);
    }

    public void testImpliedProperty() throws Exception
    {
        BeanWithDeserialize bean = MAPPER.readValue("{\"a\":3}", BeanWithDeserialize.class);
        assertNotNull(bean);
        assertEquals(3, bean.a);
    }

    // [databind#442]
    public void testIssue442PrivateUnwrapped() throws Exception
    {
        Issue442Bean bean = MAPPER.readValue("{\"i\":5}", Issue442Bean.class);
        assertEquals(5, bean.w.i);
    }

    /*
    /**********************************************************
    /* Test methods, annotations disabled
    /**********************************************************
     */

    public void testAnnotationsDisabled() throws Exception
    {
        // first: verify that annotation introspection is enabled by default
        assertTrue(MAPPER.getDeserializationConfig().isEnabled(MapperFeature.USE_ANNOTATIONS));
        // with annotations, property is renamed
        AnnoBean bean = MAPPER.readValue("{ \"y\" : 0 }", AnnoBean.class);
        assertEquals(0, bean.value);

        ObjectMapper m = jsonMapperBuilder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        // without annotations, should default to default bean-based name...
        bean = m.readValue("{ \"x\" : 0 }", AnnoBean.class);
        assertEquals(0, bean.value);
    }

    public void testEnumsWhenDisabled() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals(Alpha.B, m.readValue(q("B"), Alpha.class));

        m = jsonMapperBuilder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        // should still use the basic name handling here
        assertEquals(Alpha.B, m.readValue(q("B"), Alpha.class));
    }

    public void testNoAccessOverrides() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                .build();
        SimpleBean bean = m.readValue("{\"x\":1,\"y\":2}", SimpleBean.class);
        assertEquals(1, bean.x);
        assertEquals(2, bean.y);
    }
}
