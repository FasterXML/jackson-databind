package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#1401]: should allow "Any Setter" to back up otherwise
// problematic Creator properties?
public class AnySetterAsCreatorFallback1401Test extends DatabindTestUtil
{
    // for [databind#1401]
    static class NoSetter1401 {
        int _a;

        @JsonCreator
        public NoSetter1401(@JsonProperty("a") int a) {
            _a = a;
        }

        @JsonAnySetter
        public void any(String key, Object value) { }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1401]
    public void testCreatorNoSetter() throws Exception
    {
        NoSetter1401 b = MAPPER.readValue(a2q("{'a':1,'b':2}"),
                NoSetter1401.class);
        assertEquals(1, b._a);

        NoSetter1401 b2 = MAPPER.readerForUpdating(new NoSetter1401(1))
                .readValue(a2q("{'a':1}"));
        assertEquals(1, b2._a);
    }
}
