package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

// [databind#1592]: allow "coercion" between primitive/wrapper (mostly just ignoring)
public class TypeCoercion1592Test extends BaseMapTest
{
    static class Bean1592
    {
        @JsonSerialize(as=Integer.class)
        public int i;

        @JsonDeserialize(as=Long.class)
        public long l;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testTypeCoercion1592() throws Exception
    {
        // first, serialize
        MAPPER.writeValueAsString(new Bean1592());
        Bean1592 result = MAPPER.readValue("{}", Bean1592.class);
        assertNotNull(result);
    }
}
