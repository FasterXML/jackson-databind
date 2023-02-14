package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class MergePolymorphicTest extends BaseMapTest {

    static class Root {
        @JsonMerge
        public Child child;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = ChildA.class, name = "ChildA"),
        @JsonSubTypes.Type(value = ChildB.class, name = "ChildB")
    })
    static abstract class Child {
    }

    static class ChildA extends Child {
        public String name;
    }

    static class ChildB extends Child {
        public String code;
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public void testPolymorphicNewObject() throws Exception {
        Root root = MAPPER.readValue("{\"child\": { \"@type\": \"ChildA\", \"name\": \"I'm child A\" }}", Root.class);
        assertTrue(root.child instanceof ChildA);
        assertEquals("I'm child A", ((ChildA) root.child).name);
    }

    public void testPolymorphicFromNullToNewObject() throws Exception {
        Root root = new Root();
        MAPPER.readerForUpdating(root).readValue("{\"child\": { \"@type\": \"ChildA\", \"name\": \"I'm the new name\" }}");
        assertTrue(root.child instanceof ChildA);
        assertEquals("I'm the new name", ((ChildA) root.child).name);
    }

    public void testPolymorphicFromObjectToNull() throws Exception {
        Root root = new Root();
        ChildA childA = new ChildA();
        childA.name = "I'm child A";
        root.child = childA;
        MAPPER.readerForUpdating(root).readValue("{\"child\": null }");
        assertTrue(root.child == null);
    }

    public void testPolymorphicPropertyCanBeMerged() throws Exception {
        Root root = new Root();
        ChildA childA = new ChildA();
        childA.name = "I'm child A";
        root.child = childA;
        MAPPER.readerForUpdating(root).readValue("{\"child\": { \"@type\": \"ChildA\", \"name\": \"I'm the new name\" }}");
        assertTrue(root.child instanceof ChildA);
        assertEquals("I'm the new name", ((ChildA) root.child).name);
    }

    public void testPolymorphicPropertyTypeCanNotBeChanged() throws Exception {
        Root root = new Root();
        ChildA childA = new ChildA();
        childA.name = "I'm child A";
        root.child = childA;
        MAPPER.readerForUpdating(root).readValue("{\"child\": { \"@type\": \"ChildB\", \"code\": \"I'm the code\" }}");
        // The polymorphic type can't be changed
        assertTrue(root.child instanceof ChildA);
        assertEquals("I'm child A", ((ChildA) root.child).name);
    }

}
