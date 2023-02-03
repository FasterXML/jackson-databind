package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

// For [databind#1501], [databind#1502], [databind#1503]; mostly to
// test that for non-static inner classes constructors are ignored
// and no Creators should be processed (since they cannot be made
// to work in standard way anyway).
public class InnerClassCreatorTest extends BaseMapTest
{
    static class Something1501 {
        public InnerSomething1501 a;

        // important: must name the parameter (param names module, or explicit)
        @JsonCreator
        public Something1501(@JsonProperty("a") InnerSomething1501 a) { this.a = a; }

        public Something1501(boolean bogus) { a = new InnerSomething1501(); }

        class InnerSomething1501 {
            @JsonCreator
            public InnerSomething1501() { }
        }
    }

    static class Something1502 {
        @JsonProperty
        public InnerSomething1502 a;

        @JsonCreator
        public Something1502(InnerSomething1502 a) {}

        class InnerSomething1502 {
            @JsonCreator
            public InnerSomething1502() {}
        }
    }

    static class Outer1503 {
        public InnerClass1503 innerClass;

        class InnerClass1503 {
            public Generic<?> generic;
            public InnerClass1503(@JsonProperty("generic") Generic<?> generic) {}
        }

        static class Generic<T> {
            public int ignored;
        }
    }

    private final ObjectMapper MAPPER = new ObjectMapper();
    {
        MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    // Used to trigger `ArrayIndexOutOfBoundsException` for missing creator property index
    public void testIssue1501() throws Exception
    {
        String ser = MAPPER.writeValueAsString(new Something1501(false));
        try {
            MAPPER.readValue(ser, Something1501.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "InnerSomething1501");
            verifyException(e, "non-static inner classes like this can only by instantiated using default");
        }
    }

    public void testIssue1502() throws Exception
    {
        String ser = MAPPER.writeValueAsString(new Something1502(null));
        try {
            MAPPER.readValue(ser, Something1502.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "InnerSomething1502");
            verifyException(e, "non-static inner classes like this can only by instantiated using default");
        }
    }

    public void testIssue1503() throws Exception
    {
        String ser = MAPPER.writeValueAsString(new Outer1503());
        Outer1503 result = MAPPER.readValue(ser, Outer1503.class);
        assertNotNull(result);
    }
}
