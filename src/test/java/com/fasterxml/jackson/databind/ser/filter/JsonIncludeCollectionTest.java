package com.fasterxml.jackson.databind.ser.filter;

import java.util.Arrays;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class JsonIncludeCollectionTest extends DatabindTestUtil
{
    static class NonEmptyEnumSet {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public EnumSet<ABC> v;

        public NonEmptyEnumSet(ABC...values) {
            if (values.length == 0) {
                v = EnumSet.noneOf(ABC.class);
            } else {
                v = EnumSet.copyOf(Arrays.asList(values));
            }
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testEnumSet() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyEnumSet()));
        assertEquals("{\"v\":[\"B\"]}", MAPPER.writeValueAsString(new NonEmptyEnumSet(ABC.B)));
    }
}
