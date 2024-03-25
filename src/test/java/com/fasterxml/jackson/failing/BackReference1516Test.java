package com.fasterxml.jackson.failing;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BackReference1516Test extends DatabindTestUtil {
    static class ParentWithCreator {
        String id, name;

        @JsonManagedReference
        ChildObject1 child;

        @ConstructorProperties({"id", "name", "child"})
        public ParentWithCreator(String id, String name,
                                 ChildObject1 child) {
            this.id = id;
            this.name = name;
            this.child = child;
        }
    }

    static class ChildObject1 {
        public String id, name;

        @JsonBackReference
        public ParentWithCreator parent;

        @ConstructorProperties({"id", "name", "parent"})
        public ChildObject1(String id, String name,
                            ParentWithCreator parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
        }
    }

    static class ParentWithoutCreator {
        public String id, name;

        @JsonManagedReference
        public ChildObject2 child;
    }

    static class ChildObject2 {
        public String id, name;

        @JsonBackReference
        public ParentWithoutCreator parent;

        @ConstructorProperties({"id", "name", "parent"})
        public ChildObject2(String id, String name,
                            ParentWithoutCreator parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String PARENT_CHILD_JSON = a2q(
            "{ 'id': 'abc',\n" +
                    "  'name': 'Bob',\n" +
                    "  'child': { 'id': 'def', 'name':'Bert' }\n" +
                    "}");

    @Test
    void withParentCreator() throws Exception {
        ParentWithCreator result = MAPPER.readValue(PARENT_CHILD_JSON,
                ParentWithCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }

    @Test
    void withParentNoCreator() throws Exception {
        ParentWithoutCreator result = MAPPER.readValue(PARENT_CHILD_JSON,
                ParentWithoutCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }
}
