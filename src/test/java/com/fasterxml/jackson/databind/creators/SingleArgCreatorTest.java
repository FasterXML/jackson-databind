package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class SingleArgCreatorTest extends BaseMapTest
{
    // [Issue#430]: single arg BUT named; should not delegate

    static class SingleNamedStringBean {

        final String _ss;

        @JsonCreator
        public SingleNamedStringBean(@JsonProperty("") String ss){
            this._ss = ss;
        }

        public String getSs() { return _ss; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = objectMapper();

    public void testNamedSingleArg() throws Exception
    {
        SingleNamedStringBean bean = MAPPER.readValue(quote("foobar"),
                SingleNamedStringBean.class);
        assertEquals("foobar", bean._ss);
    }
}
