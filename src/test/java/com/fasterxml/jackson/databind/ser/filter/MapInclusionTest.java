package com.fasterxml.jackson.databind.ser.filter;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class MapInclusionTest extends DatabindTestUtil
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

    // [databind#2909]
    static class Wrapper2909 {
        @JsonValue
        public Map<String, String> values = new HashMap<>();
    }

    static class TopLevel2099 {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Wrapper2909 nested = new Wrapper2909();
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = newJsonMapper();

    // [databind#588]
    @Test
    public void testNonEmptyValueMapViaProp() throws IOException
    {
        String json = MAPPER.writeValueAsString(new NoEmptiesMapContainer()
            .add("a", null)
            .add("b", ""));
        assertEquals(a2q("{}"), json);
    }

    @Test
    public void testNoNullsMap() throws IOException
    {
        NoNullsMapContainer input = new NoNullsMapContainer()
                .add("a", null)
                .add("b", "");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'stuff':{'b':''}}"), json);
    }

    @Test
    public void testNonEmptyNoNullsMap() throws IOException
    {
        NoNullsNotEmptyMapContainer input = new NoNullsNotEmptyMapContainer()
                .add("a", null)
                .add("b", "");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'stuff':{'b':''}}"), json);

        json = MAPPER.writeValueAsString(new NoNullsNotEmptyMapContainer()
                .add("a", null)
                .add("b", null));
        assertEquals(a2q("{}"), json);
    }

    // [databind#2909]
    @Test
    public void testMapViaJsonValue() throws Exception
    {
        assertEquals(a2q("{}"), MAPPER.writeValueAsString(new TopLevel2099()));
    }
}
