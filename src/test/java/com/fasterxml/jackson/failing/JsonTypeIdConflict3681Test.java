package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class JsonTypeIdConflict3681Test extends BaseMapTest {

    /*
    /**********************************************************
    /* Set up
    /**********************************************************
    */

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "c_impl", value = C_Impl.class)
    })
    private interface A {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "b_impl", value = B_impl.class),
    })
    private interface B {}

    /***
     * Note the order of declarations on inherited interfaces - it makes
     * the difference in how types are resolved here.
     */
    private interface C extends A, B {}

    private static class C_Impl implements C {}

    private static class B_impl implements B {}

    private static class Clazz {
        @JsonProperty
        public C c;
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testCheckSanity() throws Exception {
        try {

            MAPPER.readValue(a2q("{'c': {'type': 'c_impl'}}"), Clazz.class);
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Cannot resolve type id 'c_impl' as a subtype of");
        }
    }
}
