package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class NullConversionWithCreatorTest extends BaseMapTest
{
    static class EmptyFromNullViaCreator {
        Point p;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public EmptyFromNullViaCreator(@JsonSetter(nulls=Nulls.AS_EMPTY)
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

    public void testEmptyFromNullViaCreator() throws Exception
    {
        EmptyFromNullViaCreator result = MAPPER.readValue(aposToQuotes("{'p':null}"),
                EmptyFromNullViaCreator.class);
        assertNotNull(result);
        assertNotNull(result.p);
    }
}
