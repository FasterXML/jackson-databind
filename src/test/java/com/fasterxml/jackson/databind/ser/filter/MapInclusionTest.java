package com.fasterxml.jackson.databind.ser.filter;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;

public class MapInclusionTest extends BaseMapTest
{
    static class NoEmptiesMapContainer {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_EMPTY)
        public Map<String,String> stuff = new LinkedHashMap<String,String>();

        public NoEmptiesMapContainer add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    static class NoNullsMapContainer {
        @JsonInclude(value=JsonInclude.Include.NON_NULL,
                content=JsonInclude.Include.NON_NULL)
        public Map<String,String> stuff = new LinkedHashMap<String,String>();

        public NoNullsMapContainer add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    static class NoNullsNotEmptyMapContainer {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_NULL)
        public Map<String,String> stuff = new LinkedHashMap<String,String>();

        public NoNullsNotEmptyMapContainer add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();

    // [databind#588]
    public void testNonEmptyValueMapViaProp() throws IOException
    {
        String json = MAPPER.writeValueAsString(new NoEmptiesMapContainer()
            .add("a", null)
            .add("b", ""));
        assertEquals(aposToQuotes("{}"), json);
    }

    public void testNoNullsMap() throws IOException
    {
        NoNullsMapContainer input = new NoNullsMapContainer()
                .add("a", null)
                .add("b", "");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(aposToQuotes("{'stuff':{'b':''}}"), json);
    }

    public void testNonEmptyNoNullsMap() throws IOException
    {
        NoNullsNotEmptyMapContainer input = new NoNullsNotEmptyMapContainer()
                .add("a", null)
                .add("b", "");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(aposToQuotes("{'stuff':{'b':''}}"), json);

        json = MAPPER.writeValueAsString(new NoNullsNotEmptyMapContainer()
                .add("a", null)
                .add("b", null));
        assertEquals(aposToQuotes("{}"), json);
    }
}
