package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IgnoredCreatorProperty1572Test extends BaseMapTest
{
    static class InnerTest
    {
        public String str;
        public String otherStr;
    }

    static class OuterTest
    {
        InnerTest inner;

        @JsonIgnore
        public String otherStr;
        
        @JsonCreator
        public OuterTest(@JsonProperty("inner") InnerTest inner,
                @JsonProperty("otherOtherStr") String otherStr) {
            this.inner = inner;
        }
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#1572]
    public void testIgnoredCtorParam() throws Exception
    {
        String JSON = aposToQuotes("{'innerTest': {\n"
                +"'str':'str',\n"
                +"'otherStr': 'otherStr'\n"
                +"}}\n");
        OuterTest result = MAPPER.readValue(JSON, OuterTest.class);
        assertNotNull(result);
    }
}
