package com.fasterxml.jackson.databind.tofix;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4697] Inconsistent Serialization with Jackson’s @JsonUnwrapped Annotation
//                 Using Shared vs. New ObjectMapper Instances #4697
public class JsonUnwrappedInconsistentSerialization4697Test
        extends DatabindTestUtil
{
    public static class First {
        @JsonUnwrapped(prefix = "")
        public Thrid thrid = new Thrid();
    }

    public static class Second {
        @JsonUnwrapped(prefix = "fromSecond")
        public Thrid thrid = new Thrid();
    }

    public static class Thrid {
        @JsonUnwrapped(prefix = "fromThird")
        public Common common = new Common();

        public Thrid() {
            this.common.a = "a";
            this.common.b = "b";
        }
    }

    public static class Common {
        public String a;
        public String b;
        public Common() {}
    }

    @JacksonTestFailureExpected
    @Test
    public void testInconsistentSer() throws Exception {
        First first = new First();
        Second second = new Second();

        ObjectMapper firstMapper = newJsonMapper();
        ObjectMapper secondMapper = newJsonMapper();

        firstMapper.writeValueAsString(first);
        assertEquals(
                firstMapper.writeValueAsString(second),
                secondMapper.writeValueAsString(second));
    }
}
