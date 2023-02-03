package com.fasterxml.jackson.failing;

import java.beans.ConstructorProperties;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackReference1516Test extends BaseMapTest
{
    static class ParentWithCreator {
        String id, name;

        @JsonManagedReference
        ChildObject1 child;

        @ConstructorProperties({ "id", "name", "child" })
        public ParentWithCreator(String id, String name,
                ChildObject1 child) {
            this.id = id;
            this.name = name;
            this.child = child;
        }
    }

    static class ChildObject1
    {
        public String id, name;

        @JsonBackReference
        public ParentWithCreator parent;

        @ConstructorProperties({ "id", "name", "parent" })
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

    static class ChildObject2
    {
        public String id, name;

        @JsonBackReference
        public ParentWithoutCreator parent;

        @ConstructorProperties({ "id", "name", "parent" })
        public ChildObject2(String id, String name,
                ParentWithoutCreator parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String PARENT_CHILD_JSON = a2q(
"{ 'id': 'abc',\n"+
"  'name': 'Bob',\n"+
"  'child': { 'id': 'def', 'name':'Bert' }\n"+
"}");

    public void testWithParentCreator() throws Exception
    {
        ParentWithCreator result = MAPPER.readValue(PARENT_CHILD_JSON,
                ParentWithCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }

    public void testWithParentNoCreator() throws Exception
    {
        ParentWithoutCreator result = MAPPER.readValue(PARENT_CHILD_JSON,
                ParentWithoutCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }
}
