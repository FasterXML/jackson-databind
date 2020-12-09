package com.fasterxml.jackson.databind.deser.filter;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class ProblemHandler2973Test extends BaseMapTest
{
    // [databind#2973]
    static class WeirdTokenHandler
        extends DeserializationProblemHandler
    {
        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt,
                JavaType targetType, JsonToken t, JsonParser p,
                String failureMsg)
            throws IOException
        {
            String result = p.currentToken().toString();
            p.skipChildren();
            return result;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [databind#2973]
    public void testUnexpectedToken2973() throws Exception
    {
        // First: without handler, should get certain failure
        ObjectMapper mapper = sharedMapper();
        try {
            mapper.readValue("{ }", String.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.lang.String` from Object value");
        }

        // But DeserializationProblemHandler should resolve:
        mapper = jsonMapperBuilder()
            .addHandler(new WeirdTokenHandler())
            .build();
        ;
        String str = mapper.readValue("{ }", String.class);
        assertEquals("START_OBJECT", str);
    }
}
