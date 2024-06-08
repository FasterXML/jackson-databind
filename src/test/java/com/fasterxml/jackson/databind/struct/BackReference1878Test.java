package com.fasterxml.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Reda.Housni-Alaoui
 */
public class BackReference1878Test extends DatabindTestUtil
{
    static class Child {
        @JsonBackReference
        public Parent b;
    }

    static class Parent {
        @JsonManagedReference
        public Child a;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testChildDeserialization() throws Exception {
        Child child = MAPPER.readValue("{\"b\": {}}", Child.class);
        assertNotNull(child.b);
    }
}
