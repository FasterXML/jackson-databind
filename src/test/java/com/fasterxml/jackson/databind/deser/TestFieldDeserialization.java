package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Unit tests for verifying that field-backed properties can also be
 * deserialized as well as setter-accessible properties.
 */
public class TestFieldDeserialization
    extends BaseMapTest
{
    static class SimpleFieldBean
    {
        public int x, y;

        // not auto-detectable, not public
        int z;

        // ignored, not detectable either
        @JsonIgnore public int a;
    }

    static class SimpleFieldBean2
    {
        @JsonDeserialize String[] values;
    }

    @JsonAutoDetect(fieldVisibility=Visibility.NONE)
    static class NoAutoDetectBean
    {
        // not auto-detectable any more
        public int z;

        @JsonProperty("z")
        public int _z;
    }

    // Let's test invalid bean too
    static class DupFieldBean
    {
        public int z;

        @JsonProperty("z")
        public int _z;
    }

    public static class DupFieldBean2
    {
        @JsonProperty("foo")
        public int _z;

        @JsonDeserialize
        int foo;
    }

    public static class OkDupFieldBean
        extends SimpleFieldBean
    {
        @JsonProperty("x")
        protected int myX = 10;

        @SuppressWarnings("hiding")
        public int y = 11;
    }

    abstract static class Abstract { }
    static class Concrete extends Abstract
    {
        String value;

        public Concrete(String v) { value = v; }
    }

    static class AbstractWrapper {
        @JsonDeserialize(as=Concrete.class)
        public Abstract value;
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testSimpleAutoDetect() throws Exception
    {
        SimpleFieldBean result = MAPPER.readValue("{ \"x\" : -13 }",
                SimpleFieldBean.class);
        assertEquals(-13, result.x);
        assertEquals(0, result.y);
    }

    public void testSimpleAnnotation() throws Exception
    {
        SimpleFieldBean2 bean = MAPPER.readValue("{ \"values\" : [ \"x\", \"y\" ] }",
                SimpleFieldBean2.class);
        String[] values = bean.values;
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals("x", values[0]);
        assertEquals("y", values[1]);
    }

    public void testNoAutoDetect() throws Exception
    {
        NoAutoDetectBean bean = MAPPER.readValue("{ \"z\" : 7 }",
                NoAutoDetectBean.class);
        assertEquals(7, bean._z);
    }

    public void testTypeAnnotation() throws Exception
    {
        AbstractWrapper w = MAPPER.readValue("{ \"value\" : \"abc\" }",
                AbstractWrapper.class);
        Abstract bean = w.value;
        assertNotNull(bean);
        assertEquals(Concrete.class, bean.getClass());
        assertEquals("abc", ((Concrete)bean).value);
    }

    public void testResolvedDups1() throws Exception
    {
        DupFieldBean result = MAPPER.readValue(a2q("{'z':3}"), DupFieldBean.class);
        assertEquals(3, result._z);
        assertEquals(0, result.z);
    }

    public void testFailingDups2() throws Exception
    {
        // Fails because both fields have explicit annotation
        try {
            DupFieldBean2 result = MAPPER.readValue(a2q("{'foo':28}"), DupFieldBean2.class);
            fail("Should not pass but got: "+result);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Multiple fields representing property \"foo\"");
        }
    }

    public void testOkFieldOverride() throws Exception
    {
        OkDupFieldBean result = MAPPER.readValue("{ \"x\" : 1, \"y\" : 2 }",
                OkDupFieldBean.class);
        assertEquals(1, result.myX);
        assertEquals(2, result.y);
    }
}
