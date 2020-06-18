package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

/**
 * This unit test suite that tests use of {@link com.fasterxml.jackson.annotation.JsonIncludeProperties}
 * annotation with deserialization.
 */
public class IncludeWithDeserTest
    extends BaseMapTest
{
    // Class for testing {@link JsonIgnoreProperties} annotations with setters
    final static class SizeClassIgnore
    {
        int _x = 0;
        int _y = 0;

        public void setX(int value) { _x = value; }
        @JsonIgnore public void setY(int value) { _y = value; }

        /* Just igoring won't help a lot here; let's define a replacement
         * so that we won't get an exception for "unknown field"
         */
        @JsonProperty("y") void foobar(int value) {
            ; // nop
        }
    }

    @JsonIncludeProperties({ "y", "z" })
    final static class OnlyYOrZ
    {
        public int x;

        public int y = 1;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    public void testSimpleIgnore() throws Exception
    {
        SizeClassIgnore result = MAPPER.readValue("{ \"x\":1, \"y\" : 2 }",
             SizeClassIgnore.class);
        // x should be set, y not
        assertEquals(1, result._x);
        assertEquals(0, result._y);
    }

    public void testIncludeOneField() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(OnlyYOrZ.class);
        
        // First, fine to get "y":
        OnlyYOrZ result = r.readValue(aposToQuotes("{'x':3, 'y': 4}"));
        assertEquals(4, result.y);
        assertEquals(0, result.x);

        // but not 'y'
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            result = r.readValue(aposToQuotes("{'x':3, 'y': 4, 'z': 5}"));
            fail("Should fail");
        } catch (JsonMappingException e) {
            verifyException(e, "Ignored field");
        }

        // or 'z'
        try {
            result = r.readValue(aposToQuotes("{'z':2 }"));
            fail("Should fail");
        } catch (JsonMappingException e) {
            verifyException(e, "Ignored field");
        }
    }
}
