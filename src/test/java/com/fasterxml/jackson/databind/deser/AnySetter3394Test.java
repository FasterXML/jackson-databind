package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import com.fasterxml.jackson.databind.*;

// for [databind#3394]
public class AnySetter3394Test extends BaseMapTest
{
    static class AnySetter3394Bean {
        @JsonAnySetter
        public JsonNode extraData;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testAnySetterWithJsonNode() throws Exception
    {
        final String DOC = a2q("{'test': 3}");
        AnySetter3394Bean bean = MAPPER.readValue(DOC, AnySetter3394Bean.class);
        assertEquals(DOC, ""+bean.extraData);
    }
}
