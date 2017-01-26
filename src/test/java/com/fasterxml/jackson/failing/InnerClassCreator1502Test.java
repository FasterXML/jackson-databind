package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

// for [databind#1502], [databind#1503]
public class InnerClassCreator1502Test extends BaseMapTest
{
    static class Something1501 {
        public InnerSomething a;

        // important: must name the parameter (param names module, or explicit)
        @JsonCreator
        public Something1501(@JsonProperty("a") InnerSomething a) { this.a = a; }

        public Something1501(boolean bogus) { a = new InnerSomething(); }

        class InnerSomething {
            @JsonCreator
            public InnerSomething() { }
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

    static class Broken1503 {
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
        MAPPER.readValue(ser, Something1501.class);
    }    

    public void testIssue1502() throws Exception
    {
        String ser = MAPPER.writeValueAsString(new Something1502(null));
//System.err.println("DEBUG: ser == "+ser);
        MAPPER.readValue(ser, Something1502.class);
    }

    public void testIssue1503() throws Exception
    {
        String ser = MAPPER.writeValueAsString(new Broken1503());
        MAPPER.readValue(ser, Broken1503.class);
    }
}
