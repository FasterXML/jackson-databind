package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonTypeIdConflict3681Test extends BaseMapTest {

    /*
    /**********************************************************
    /* Set up
    /**********************************************************
    */

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "a_impl", value = A_Impl.class)
    })
    private interface A {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "b_impl", value = B_Impl.class),
    })
    private interface B {}

    /**
     * NOTE: the <b>"order"</b> of declarations on inherited interfaces makes
     * the difference in how types are resolved here.
     */
    private interface C extends B, A {}

    private static class A_Impl implements C {}

    private static class B_Impl implements B {}

    private static class WrapperC {
        @JsonProperty
        public C c;
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Type resolution fails due to a conflict between the types.
     * <p>
     * NOTE: This only pass when {@code C} extends {@code A} first.
     * For example, {@code private interface C extends A, B}
     */
    public void testFailureWithTypeIdConflict() throws Exception {
        WrapperC c = MAPPER.readValue(a2q("{'c': {'type': 'c_impl'}}"), WrapperC.class);
        assertNotNull(c);
    }
}
