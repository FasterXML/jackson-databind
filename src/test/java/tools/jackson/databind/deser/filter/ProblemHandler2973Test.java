package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class ProblemHandler2973Test
{
    // [databind#2973]
    static class WeirdTokenHandler
        extends DeserializationProblemHandler
    {
        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt,
                JavaType targetType, JsonToken t, JsonParser p,
                String failureMsg)
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
    @Test
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

        String str = mapper.readValue("{ }", String.class);
        assertEquals("START_OBJECT", str);
    }
}
