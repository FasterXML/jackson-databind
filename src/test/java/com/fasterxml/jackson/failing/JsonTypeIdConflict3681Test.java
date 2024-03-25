package com.fasterxml.jackson.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#3681]: JsonSubTypes declared on multiple interfaces of a class
// results in order-dependent resolution and outcome
class JsonTypeIdConflict3681Test extends DatabindTestUtil {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "a_impl", value = A_Impl.class)
    })
    private interface A {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "b_impl", value = B_Impl.class),
    })
    private interface B {
    }

    /**
     * NOTE: the <b>"order"</b> of declarations on inherited interfaces makes
     * the difference in how types are resolved here.
     */
    private interface C extends B, A {
    }

    private static class A_Impl implements C {
    }

    private static class B_Impl implements B {
    }

    private static class WrapperC {
        @JsonProperty
        public C c;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Type resolution fails due to a conflict between the types --check the exception message below this test case.
     * <p>
     * This will only pass when we modify {@code C} to extend {@code A} first, like so:
     * <pre>
     *  private interface C extends A, B {}
     * </pre>
     */
    @Test
    void failureWithTypeIdConflict() throws Exception {
        WrapperC c = MAPPER.readValue(a2q("{'c': {'type': 'c_impl'}}"), WrapperC.class);
        assertNotNull(c);
    }
}
