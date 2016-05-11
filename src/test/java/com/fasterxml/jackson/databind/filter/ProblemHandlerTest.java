package com.fasterxml.jackson.databind.filter;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

public class ProblemHandlerTest extends BaseMapTest
{
    static class TypeIdHandler
        extends DeserializationProblemHandler
    {
        @Override
        public JavaType handleUnknownTypeId(DeserializationContext ctxt,
                JavaType baseType, String subTypeId,
                String failureMsg)
            throws IOException
        {
            return ctxt.constructType(Impl.class);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    static class Base { }
    static class Impl extends Base {
        public int a;
    }

    static class BaseWrapper {
        public Base value;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testInvalidTypeId() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
            .addHandler(new TypeIdHandler());
        BaseWrapper w = mapper.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
    }
}
