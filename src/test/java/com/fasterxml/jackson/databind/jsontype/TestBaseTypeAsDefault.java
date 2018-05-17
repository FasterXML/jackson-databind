package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TestBaseTypeAsDefault extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    static class Parent {
    }


    static class Child extends Parent {
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class", defaultImpl = ChildOfChild.class)
    static abstract class AbstractParentWithDefault {
    }


    static class ChildOfAbstract extends AbstractParentWithDefault {
    }

    static class ChildOfChild extends ChildOfAbstract {
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    protected ObjectMapper MAPPER_WITH_BASE = new ObjectMapper()
                .enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL);

    protected ObjectMapper MAPPER_WITHOUT_BASE = new ObjectMapper()
            .disable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL);
    
    public void testPositiveForParent() throws IOException {
        Object o = MAPPER_WITH_BASE.readerFor(Parent.class).readValue("{}");
        assertEquals(o.getClass(), Parent.class);
    }

    public void testPositiveForChild() throws IOException {
        Object o = MAPPER_WITH_BASE.readerFor(Child.class).readValue("{}");
        assertEquals(o.getClass(), Child.class);
    }

    public void testNegativeForParent() throws IOException {
        try {
            /*Object o =*/ MAPPER_WITHOUT_BASE.readerFor(Parent.class).readValue("{}");
            fail("Should not pass");
        } catch (JsonMappingException ex) {
            assertTrue(ex.getMessage().contains("missing type id property '@class'"));
        }
    }

    public void testNegativeForChild() throws IOException {
        try {
            /*Object o =*/ MAPPER_WITHOUT_BASE.readerFor(Child.class).readValue("{}");
            fail("Should not pass");
        } catch (JsonMappingException ex) {
            assertTrue(ex.getMessage().contains("missing type id property '@class'"));
        }
    }

    public void testConversionForAbstractWithDefault() throws IOException {
        // should pass shouldn't it?
        Object o = MAPPER_WITH_BASE.readerFor(AbstractParentWithDefault.class).readValue("{}");
        assertEquals(o.getClass(), ChildOfChild.class);
    }

    public void testPositiveWithTypeSpecification() throws IOException {
        Object o = MAPPER_WITH_BASE.readerFor(Parent.class)
                .readValue("{\"@class\":\""+Child.class.getName()+"\"}");
        assertEquals(o.getClass(), Child.class);
    }

    public void testPositiveWithManualDefault() throws IOException {
        Object o = MAPPER_WITH_BASE.readerFor(ChildOfAbstract.class).readValue("{}");

        assertEquals(o.getClass(), ChildOfChild.class);
    }
}
