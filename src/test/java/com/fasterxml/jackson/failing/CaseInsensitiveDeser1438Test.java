package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class CaseInsensitiveDeser1438Test extends BaseMapTest
{
    // [databind#1438]
    static class InsensitiveCreator
    {
        int v;

        @JsonCreator
        public InsensitiveCreator(@JsonProperty("value") int v0) {
            v = v0;
        }
    }
    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */
    private final ObjectMapper INSENSITIVE_MAPPER = new ObjectMapper();
    {
        INSENSITIVE_MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        
    }

    // [databind#1438]
    public void testCreatorWithInsensitive() throws Exception
    {
        final String json = aposToQuotes("{'VALUE':3}");
        InsensitiveCreator bean = INSENSITIVE_MAPPER.readValue(json, InsensitiveCreator.class);
        assertEquals(3, bean.v);
    }
}
