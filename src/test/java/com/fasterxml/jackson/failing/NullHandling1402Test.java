package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

// for [databind#1402]
public class NullHandling1402Test extends BaseMapTest
{
    static class NullFail {
        public String nullsOk = "a";

        @JsonSetter(nulls=JsonSetter.Nulls.FAIL)
        public String noNulls = "b";
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testFailOnNull() throws Exception
    {
        // first, ok if assigning non-null to not-nullable, null for nullable
        NullFail result = MAPPER.readValue(aposToQuotes("{'noNulls':'foo', 'nullsOk':null}"),
                NullFail.class);
        assertEquals("foo", result.noNulls);
        assertNull(result.nullsOk);

        // and then see that nulls are not ok for non-nullable
        try {
            result = MAPPER.readValue(aposToQuotes("{'noNulls':null}"),
                    NullFail.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "noNulls");
        }
    }
}
