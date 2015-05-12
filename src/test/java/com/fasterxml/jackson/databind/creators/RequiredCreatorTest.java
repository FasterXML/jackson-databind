package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class RequiredCreatorTest extends BaseMapTest
{
    static class FascistPoint {
        int x, y;

        @JsonCreator
        public FascistPoint(@JsonProperty(value="x", required=true) int x,
                @JsonProperty(value="y", required=false) int y)
        {
            this.x = x;
            this.y = y;
        }
    }

    private final ObjectReader POINT_READER = objectMapper().readerFor(FascistPoint.class);
    
    public void testRequiredAnnotatedParam() throws Exception
    {
        FascistPoint p;

        // First: fine if both params passed
        p = POINT_READER.readValue(aposToQuotes("{'y':2,'x':1}"));
        assertEquals(1, p.x);
        assertEquals(2, p.y);
        p = POINT_READER.readValue(aposToQuotes("{'x':3,'y':4}"));
        assertEquals(3, p.x);
        assertEquals(4, p.y);

        // also fine if 'y' is MIA
        p = POINT_READER.readValue(aposToQuotes("{'x':3}"));
        assertEquals(3, p.x);
        assertEquals(0, p.y);

        // but not so good if 'x' missing
        try {
            POINT_READER.readValue(aposToQuotes("{'y':3}"));
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Missing required creator property 'x' (index 0)");
        }
    }

    public void testRequiredGloballyParam() throws Exception
    {
        FascistPoint p;

        // as per above, ok to miss 'y' with default settings:
        p = POINT_READER.readValue(aposToQuotes("{'x':2}"));
        assertEquals(2, p.x);
        assertEquals(0, p.y);

        // but not if global checks desired
        ObjectReader r = POINT_READER.with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
        try {
            r.readValue(aposToQuotes("{'x':6}"));
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Missing creator property 'y' (index 1)");
        }
    }
}
