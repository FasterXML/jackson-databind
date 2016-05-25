package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;

public class UnknownSubClassTest extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
            property = "clazz")
    abstract static class BaseClass {
    }    

    static class BaseWrapper {
        public BaseClass value;
    }
    
    public void testUnknownClassAsSubtype() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        BaseWrapper w = mapper.readValue(aposToQuotes
                ("{'value':{'clazz':'com.foobar.Nothing'}}'"),
                BaseWrapper.class);
        assertNotNull(w);
    }
}
