package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

// for [databind#1501]
public class InnerClassCreatorTest extends BaseMapTest
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
}
