package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This unit test suite that tests use of {@link com.fasterxml.jackson.annotation.JsonIncludeProperties}
 * annotation with deserialization.
 */
public class IncludeWithDeserTest
        extends BaseMapTest
{
    @JsonIncludeProperties({"y", "z"})
    static class OnlyYAndZ
    {
        int _x = 0;
        int _y = 0;
        int _z = 0;

        public void setX(int value)
        {
            _x = value;
        }

        public void setY(int value)
        {
            _y = value;
        }

        public void setZ(int value)
        {
            _z = value;
        }

        @JsonProperty("y")
        void replacementForY(int value)
        {
            _y = value * 2;
        }
    }

    @JsonIncludeProperties({"y", "z"})
    static class OnlyY
    {
        public int x;

        public int y = 1;
    }

    static class OnlyYWrapperForOnlyYAndZ
    {
        @JsonIncludeProperties("y")
        public OnlyYAndZ onlyY;
    }

    // for [databind#1060]
    static class IncludeForListValuesY
    {
        @JsonIncludeProperties({"y"})
        //@JsonIgnoreProperties({"z"})
        public List<OnlyYAndZ> onlyYs;

        public IncludeForListValuesY()
        {
            onlyYs = Arrays.asList(new OnlyYAndZ());
        }
    }

    @JsonIncludeProperties({"@class", "a"})
    static class MyMap extends HashMap<String, String>
    {
    }

    static class MapWrapper
    {
        @JsonIncludeProperties({"a"})
        public final HashMap<String, Integer> value = new HashMap<String, Integer>();
    }

    @JsonIncludeProperties({"foo", "bar"})
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class AnySetterObjectId
    {
        protected Map<String, AnySetterObjectId> values = new HashMap<String, AnySetterObjectId>();

        @JsonAnySetter
        public void anySet(String field, AnySetterObjectId value)
        {
            // Ensure that it is never called with null because of unresolved reference.
            assertNotNull(value);
            values.put(field, value);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testSimpleInclude() throws Exception
    {
        OnlyYAndZ result = MAPPER.readValue(
                aposToQuotes("{ 'x':1, '_x': 1, 'y':2, 'z':3 }"),
                OnlyYAndZ.class);
        assertEquals(0, result._x);
        assertEquals(4, result._y);
        assertEquals(3, result._z);
    }

    public void testIncludeIgnoredAndUnrecognizedField() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(OnlyY.class);

        // First, fine to get "y" only:
        OnlyY result = r.readValue(aposToQuotes("{'x':3, 'y': 4}"));
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


    public void testMergeInclude() throws Exception
    {
        OnlyYWrapperForOnlyYAndZ onlyY = MAPPER.readValue(
                aposToQuotes("{'onlyY': {'x': 2, 'y':3, 'z': 4}}"),
                OnlyYWrapperForOnlyYAndZ.class
        );
        assertEquals(0, onlyY.onlyY._x);
        assertEquals(6, onlyY.onlyY._y);
        assertEquals(0, onlyY.onlyY._z);
    }

    public void testListInclude() throws Exception
    {
        IncludeForListValuesY result = MAPPER.readValue(
                aposToQuotes("{'onlyYs':[{ 'x':1, 'y' : 2, 'z': 3 }]}"),
                IncludeForListValuesY.class);
        assertEquals(0, result.onlyYs.get(0)._x);
        assertEquals(4, result.onlyYs.get(0)._y);
        assertEquals(0, result.onlyYs.get(0)._z);
    }

    public void testMapWrapper() throws Exception
    {
        MapWrapper result = MAPPER.readValue(aposToQuotes("{'value': {'a': 2, 'b': 3}}"), MapWrapper.class);
        assertEquals(2, result.value.get("a").intValue());
        assertFalse(result.value.containsKey("b"));
    }

    public void testMyMap() throws Exception
    {
        MyMap result = MAPPER.readValue(aposToQuotes("{'a': 2, 'b': 3}"), MyMap.class);
        assertEquals("2", result.get("a"));
        assertFalse(result.containsKey("b"));
    }

    public void testForwardReferenceAnySetterComboWithInclude() throws Exception
    {
        String json = aposToQuotes("{'@id':1, 'foo':2, 'foo2':2, 'bar':{'@id':2, 'foo':1}}");
        AnySetterObjectId value = MAPPER.readValue(json, AnySetterObjectId.class);
        assertSame(value.values.get("bar"), value.values.get("foo"));
        assertNull(value.values.get("foo2"));
    }
}
