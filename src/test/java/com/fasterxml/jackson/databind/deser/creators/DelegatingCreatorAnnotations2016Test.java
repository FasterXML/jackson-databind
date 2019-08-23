package com.fasterxml.jackson.databind.deser.creators;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

// Tests for problems uncovered with [databind#2016]; related to
// `@JsonDeserialize` modifications to type, deserializer(s)
public class DelegatingCreatorAnnotations2016Test extends BaseMapTest
{
    // [databind#2016]
    static class Wrapper2016As {
        Object value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Wrapper2016As(@JsonDeserialize(as = java.util.Date.class) Object v) {
            value = v;
        }
    }

    static class Wrapper2016ContentAs {
        List<Object> value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Wrapper2016ContentAs(@JsonDeserialize(contentAs = java.util.Date.class) List<Object> v) {
            value = v;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2016]
    public void testDelegatingWithAs() throws Exception
    {
        Wrapper2016As actual = MAPPER.readValue("123", Wrapper2016As.class);
        assertEquals(Date.class, actual.value.getClass());
    }

    public void testDelegatingWithContentAs() throws Exception
    {
        Wrapper2016ContentAs actual = MAPPER.readValue("[123]", Wrapper2016ContentAs.class);
        List<Object> l = actual.value;
        assertEquals(1, l.size());
        assertEquals(Date.class, l.get(0).getClass());
    }
}
