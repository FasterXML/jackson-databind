package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Reda.Housni-Alaoui
 */
public class BackReference1878Test extends BaseMapTest
{
    static class Child {
        @JsonBackReference
        public Parent b;
    }

    static class Parent {
        @JsonManagedReference
        public Child a;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testChildDeserialization() throws Exception {
        Child child = MAPPER.readValue("{\"b\": {}}", Child.class);
        assertNotNull(child.b);
    }
}
