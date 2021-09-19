package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

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

    protected ObjectMapper MAPPER_WITH_BASE = jsonMapperBuilder()
                .enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
                .build();

    protected ObjectMapper MAPPER_WITHOUT_BASE = jsonMapperBuilder()
            .disable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
            .build();

    public void testPositiveForParent() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(Parent.class).readValue("{}");
        assertEquals(o.getClass(), Parent.class);
    }

    public void testPositiveForChild() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(Child.class).readValue("{}");
        assertEquals(o.getClass(), Child.class);
    }

    public void testNegativeForParent() throws Exception {
        try {
            /*Object o =*/ MAPPER_WITHOUT_BASE.readerFor(Parent.class).readValue("{}");
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            assertTrue(e.getMessage().contains("missing type id property '@class'"));
        }
    }

    public void testNegativeForChild() throws Exception {
        try {
            /*Object o =*/ MAPPER_WITHOUT_BASE.readerFor(Child.class).readValue("{}");
            fail("Should not pass");
        } catch (InvalidTypeIdException ex) {
            assertTrue(ex.getMessage().contains("missing type id property '@class'"));
        }
    }

    public void testConversionForAbstractWithDefault() throws Exception {
        // should pass shouldn't it?
        Object o = MAPPER_WITH_BASE.readerFor(AbstractParentWithDefault.class).readValue("{}");
        assertEquals(o.getClass(), ChildOfChild.class);
    }

    public void testPositiveWithTypeSpecification() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(Parent.class)
                .readValue("{\"@class\":\""+Child.class.getName()+"\"}");
        assertEquals(o.getClass(), Child.class);
    }

    public void testPositiveWithManualDefault() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(ChildOfAbstract.class).readValue("{}");

        assertEquals(o.getClass(), ChildOfChild.class);
    }
}
