package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class BaseTypeAsDefaultTest extends DatabindTestUtil
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

    protected ObjectMapper MAPPER_WITHOUT_BASE_OR_SUBTYPE_ID = jsonMapperBuilder()
            .disable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
            .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
            .build();

    @Test
    void positiveForParent() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(Parent.class).readValue("{}");
        assertEquals(o.getClass(), Parent.class);
    }

    @Test
    void positiveForChild() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(Child.class).readValue("{}");
        assertEquals(o.getClass(), Child.class);
    }

    @Test
    void negativeForParent() throws Exception {
        try {
            /*Object o =*/ MAPPER_WITHOUT_BASE.readerFor(Parent.class).readValue("{}");
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            assertTrue(e.getMessage().contains("missing type id property '@class'"));
        }
    }

    @Test
    void negativeForChild() throws Exception {
        try {
            /*Object o =*/ MAPPER_WITHOUT_BASE.readerFor(Child.class).readValue("{}");
            fail("Should not pass");
        } catch (InvalidTypeIdException ex) {
            assertTrue(ex.getMessage().contains("missing type id property '@class'"));
        }
    }

    @Test
    void negativeForChildWithoutRequiringTypeId() throws Exception {
        Child child = MAPPER_WITHOUT_BASE_OR_SUBTYPE_ID.readerFor(Child.class).readValue("{}");

        assertEquals(Child.class, child.getClass());
    }

    @Test
    void conversionForAbstractWithDefault() throws Exception {
        // should pass shouldn't it?
        Object o = MAPPER_WITH_BASE.readerFor(AbstractParentWithDefault.class).readValue("{}");
        assertEquals(o.getClass(), ChildOfChild.class);
    }

    @Test
    void positiveWithTypeSpecification() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(Parent.class)
                .readValue("{\"@class\":\""+Child.class.getName()+"\"}");
        assertEquals(o.getClass(), Child.class);
    }

    @Test
    void positiveWithManualDefault() throws Exception {
        Object o = MAPPER_WITH_BASE.readerFor(ChildOfAbstract.class).readValue("{}");

        assertEquals(o.getClass(), ChildOfChild.class);
    }
}
