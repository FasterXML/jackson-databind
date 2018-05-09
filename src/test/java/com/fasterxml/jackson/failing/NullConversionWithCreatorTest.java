package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

public class NullConversionWithCreatorTest extends BaseMapTest
{
    // [databind#2024]
    static class EmptyFromNullViaCreator {
        @JsonSetter(nulls=Nulls.AS_EMPTY)
        Point p;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public EmptyFromNullViaCreator(@JsonSetter(nulls=Nulls.AS_EMPTY)
            @JsonProperty("p") Point p)
        {
            this.p = p;
        }
    }

    static class FailFromNullViaCreator {
        @JsonSetter(nulls=Nulls.AS_EMPTY)
        Point p;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public FailFromNullViaCreator(@JsonSetter(nulls=Nulls.FAIL)
            @JsonProperty("p") Point p)
        {
            this.p = p;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    private final ObjectMapper MAPPER = newObjectMapper();

    // [databind#2024]
    public void testEmptyFromNullViaCreator() throws Exception
    {
        EmptyFromNullViaCreator result = MAPPER.readValue(aposToQuotes("{'p':null}"),
                EmptyFromNullViaCreator.class);
        assertNotNull(result);
        assertNotNull(result.p);
    }

    // [databind#2024]
    public void testFailForNullViaCreator() throws Exception
    {
        try {
            /*FailFromNullViaCreator result =*/ MAPPER.readValue(aposToQuotes("{'p':null}"),
                    FailFromNullViaCreator.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"p\"");
        }
    }
}
