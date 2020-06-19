package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This unit test suite that tests use of {@link com.fasterxml.jackson.annotation.JsonIncludeProperties}
 * annotation with deserialization.
 */
public class IncludeWithDeserTest
        extends BaseMapTest
{
    // Class for testing {@link JsonIgnoreProperties} annotations with setters
    @JsonIncludeProperties({"y", "z"})
    final static class SizeClassInclude
    {
        int _x = 0;
        int _y = 0;
        int _z = 0;

        public void setX(int value) { _x = value; }

        public void setY(int value) { _y = value; }

        @JsonProperty("y")
        void replacementForY(int value)
        {
            _y = value * 2;
        }
    }

    @JsonIncludeProperties({"y", "z"})
    final static class OnlyYOrZ
    {
        public int x;

        public int y = 1;
    }

    // for [databind#1060]
    static class IncludeForListValuesY
    {
        @JsonIncludeProperties({"y"})
        public List<SizeClassInclude> coordinates;

        public IncludeForListValuesY()
        {
            coordinates = Arrays.asList(new SizeClassInclude());
        }
    }

    @JsonIncludeProperties({"@class", "a"})
    static class MyMap extends HashMap<String, String> { }

    static class MapWrapper
    {
        @JsonIncludeProperties({"a"})
        public final HashMap<String, Integer> value = new HashMap<String, Integer>();
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testSimpleInclude() throws Exception
    {
        SizeClassInclude result = MAPPER.readValue("{ \"x\":1, \"y\" : 2 }",
                SizeClassInclude.class);
        assertEquals(0, result._x);
        assertEquals(4, result._y);
    }

    public void testIncludeIgnoredAndUnrecognizedField() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(OnlyYOrZ.class);

        // First, fine to get "y" only:
        OnlyYOrZ result = r.readValue(aposToQuotes("{'x':3, 'y': 4}"));
        assertEquals(0, result.x);
        assertEquals(4, result.y);

        // but fail on ignored properties.
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            r.readValue(aposToQuotes("{'x':3, 'y': 4, 'z': 5}"));
            fail("Should fail");
        } catch (JsonMappingException e) {
            verifyException(e, "Ignored field");
        }

        // or fail on unrecognized properties
        try {
            r.readValue(aposToQuotes("{'y': 3, 'z':2 }"));
            fail("Should fail");
        } catch (JsonMappingException e) {
            verifyException(e, "Unrecognized field");
        }

        // or success with the both settings disabled.
        r = r.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        r = r.without(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        r.readValue(aposToQuotes("{'y': 3, 'z':2 }"));
        assertEquals(4, result.y);
    }

    public void testListInclude() throws Exception
    {
        IncludeForListValuesY result = MAPPER.readValue(
                "{\"coordinates\":[{ \"x\":1, \"y\" : 2, \"z\": 3 }]}",
                IncludeForListValuesY.class);
        assertEquals(0, result.coordinates.get(0)._x);
        assertEquals(4, result.coordinates.get(0)._y);
        assertEquals(0, result.coordinates.get(0)._z);
    }

    public void testMapWrapper() throws Exception
    {
        MapWrapper result = MAPPER.readValue("{\"value\": {\"a\": 2, \"b\": 3}}", MapWrapper.class);
        assertEquals(2, result.value.get("a").intValue());
        assertFalse(result.value.containsKey("b"));
    }

    public void testMyMap() throws Exception
    {
        MyMap result = MAPPER.readValue("{\"a\": 2, \"b\": 3}", MyMap.class);
        assertEquals("2", result.get("a"));
        assertFalse(result.containsKey("b"));
    }
}
