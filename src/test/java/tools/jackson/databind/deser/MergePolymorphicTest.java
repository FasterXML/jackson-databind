package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MergePolymorphicTest
{
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

    @Test
    public void testPolymorphicNewObject() throws Exception {
        Root root = MAPPER.readValue("{\"child\": { \"@type\": \"ChildA\", \"name\": \"I'm child A\" }}", Root.class);
        ChildA childA = assertInstanceOf(ChildA.class, root.child);
        assertEquals("I'm child A", childA.name);
    }

    @Test
    public void testPolymorphicFromNullToNewObject() throws Exception {
        Root root = new Root();
        MAPPER.readerForUpdating(root).readValue("{\"child\": { \"@type\": \"ChildA\", \"name\": \"I'm the new name\" }}");
        ChildA childA = assertInstanceOf(ChildA.class, root.child);
        assertEquals("I'm the new name", childA.name);
    }

    @Test
    public void testPolymorphicFromObjectToNull() throws Exception {
        Root root = new Root();
        ChildA childA = new ChildA();
        childA.name = "I'm child A";
        root.child = childA;
        MAPPER.readerForUpdating(root).readValue("{\"child\": null }");
        assertNull(root.child);
    }

    @Test
    public void testPolymorphicPropertyCanBeMerged() throws Exception {
        Root root = new Root();
        ChildA childA = new ChildA();
        childA.name = "I'm child A";
        root.child = childA;
        MAPPER.readerForUpdating(root).readValue("{\"child\": { \"@type\": \"ChildA\", \"name\": \"I'm the new name\" }}");
        ChildA childA1 = assertInstanceOf(ChildA.class, root.child);
        assertEquals("I'm the new name", childA1.name);
    }

    @Test
    public void testPolymorphicPropertyTypeCanNotBeChanged() throws Exception {
        Root root = new Root();
        ChildA childA = new ChildA();
        childA.name = "I'm child A";
        root.child = childA;
        MAPPER.readerForUpdating(root).readValue("{\"child\": { \"@type\": \"ChildB\", \"code\": \"I'm the code\" }}");
        // The polymorphic type can't be changed
        ChildA childA1 = assertInstanceOf(ChildA.class, root.child);
        assertEquals("I'm child A", childA1.name);
    }

}
