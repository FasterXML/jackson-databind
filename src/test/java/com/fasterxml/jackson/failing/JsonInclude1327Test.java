package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Unit tests for checking that alternative settings for
 * {@link JsonSerialize#include} annotation property work
 * as expected.
 */
public class JsonInclude1327Test
    extends BaseMapTest
{
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Issues1327Bean {
        public String myString = "stuff";
        public List<String> myList = new ArrayList<String>();
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // for [databind#1327]
    public void testIssue1327() throws Exception {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        final Issues1327Bean input = new Issues1327Bean();
        final String jsonString = om.writeValueAsString(input);

        if (jsonString.contains("myList")) {
            fail("Should not contain `myList`: "+jsonString);
        }
    }
}
