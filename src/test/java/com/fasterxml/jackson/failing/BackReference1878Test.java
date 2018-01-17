package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created on 09/01/18.
 *
 * @author Reda.Housni-Alaoui
 */
public class BackReference1878Test extends BaseMapTest {

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
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Child child = MAPPER.readValue("{\"b\": {}}", Child.class);
        assertNotNull(child.b);
    }

}
