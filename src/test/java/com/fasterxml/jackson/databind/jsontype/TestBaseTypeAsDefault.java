package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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


    static abstract class AbstractParentWithoutDefault {}

    static class ChildOfParentWithoutDefault extends AbstractParentWithoutDefault {
        public Map mapField;
        public Parent objectField;
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

    protected ObjectMapper MAPPER_WITH_RESOLVER_BUILDER_AND_DEFAULT;
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
                .addModule(new SimpleModule()
                        .addAbstractTypeMapping(AbstractParentWithoutDefault.class, ChildOfParentWithoutDefault.class)
                        .addAbstractTypeMapping(Map.class, TreeMap.class)
                        .addAbstractTypeMapping(List.class, LinkedList.class)
                )
                .registerSubtypes(TreeMap.class, LinkedList.class, ChildOfParentWithoutDefault.class)
                .build();
        mapper.setDefaultTyping(
                new ObjectMapper.DefaultTypeResolverBuilder(
                        ObjectMapper.DefaultTyping.NON_FINAL, LaissezFaireSubTypeValidator.instance
                ).init(JsonTypeInfo.Id.CLASS, null
                ).inclusion(JsonTypeInfo.As.PROPERTY)
        );
        MAPPER_WITH_RESOLVER_BUILDER_AND_DEFAULT = mapper;
    }

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
        } catch (InvalidTypeIdException e) {
            assertTrue(e.getMessage().contains("missing type id property '@class'"));
        }
    }

    public void testNegativeForChild() throws IOException {
        try {
            /*Object o =*/ MAPPER_WITHOUT_BASE.readerFor(Child.class).readValue("{}");
            fail("Should not pass");
        } catch (InvalidTypeIdException ex) {
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

    public void testForAbstractTypeMapping() throws IOException {
        String t =
                "{" +
                "  'mapField': {" +
                "    'a':'a'" +
                "  }, " +
                "  'objectField': {}" +
                "}";
        Object o = MAPPER_WITH_RESOLVER_BUILDER_AND_DEFAULT.readValue(a2q(t), AbstractParentWithoutDefault.class);
        assertEquals(o.getClass(), ChildOfParentWithoutDefault.class);
        ChildOfParentWithoutDefault ot = (ChildOfParentWithoutDefault) o;
        assertEquals(ot.mapField.getClass(), TreeMap.class);
        assertEquals(ot.objectField.getClass(), Parent.class);
    }
}
