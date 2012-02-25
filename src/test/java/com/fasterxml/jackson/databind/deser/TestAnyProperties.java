package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying that {@link JsonAnySetter} annotation
 * works as expected.
 */
public class TestAnyProperties
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    static class MapImitator
    {
        HashMap<String,Object> _map;

        public MapImitator() {
            _map = new HashMap<String,Object>();
        }

        @JsonAnySetter
        void addEntry(String key, Object value)
        {
            _map.put(key, value);
        }
    }

    /**
     * Let's also verify that it is possible to define different
     * value: not often useful, but possible.
     */
    static class MapImitatorWithValue
    {
        HashMap<String,int[]> _map;

        public MapImitatorWithValue() {
            _map = new HashMap<String,int[]>();
        }

        @JsonAnySetter
        void addEntry(String key, int[] value)
        {
            _map.put(key, value);
        }
    }

    // Bad; 2 "any setters"
    static class Broken
    {
        @JsonAnySetter
        void addEntry1(String key, Object value) { }
        @JsonAnySetter
        void addEntry2(String key, Object value) { }
    }

    @JsonIgnoreProperties("dummy")
    static class Ignored
    {
        HashMap<String,Object> map = new HashMap<String,Object>();
 
        @JsonIgnore
        public String bogus;
        
        @JsonAnySetter
        void addEntry(String key, Object value)
        {
            map.put(key, value);
        }        
    }

    static class Bean744
    {
        protected Map<String,Object> additionalProperties;
        
        @JsonAnySetter
        public void addAdditionalProperty(String key, Object value) {
            if (additionalProperties == null) additionalProperties = new HashMap<String, Object>();
            additionalProperties.put(key,value);
        }
        
        public void setAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        @JsonAnyGetter
        public Map<String,Object> getAdditionalProperties() { return additionalProperties; }

        @JsonIgnore
        public String getName() {
           return (String) additionalProperties.get("name");
        }
    }

    public class Bean797Base
    {
        @JsonAnyGetter
        public Map<String, JsonNode> getUndefinedProperties() {
            throw new IllegalStateException("Should not call parent version!");
        }
    }

    public class Bean797BaseImpl extends Bean797Base
    {
        @Override
        public Map<String, JsonNode> getUndefinedProperties() {
            return new HashMap<String, JsonNode>();
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testSimpleMapImitation() throws Exception
    {
        MapImitator mapHolder = MAPPER.readValue
            ("{ \"a\" : 3, \"b\" : true }", MapImitator.class);
        Map<String,Object> result = mapHolder._map;
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("a"));
        assertEquals(Boolean.TRUE, result.get("b"));
    }

    public void testSimpleTyped() throws Exception
    {
        MapImitatorWithValue mapHolder = MAPPER.readValue
            ("{ \"a\" : [ 3, -1 ], \"b\" : [ ] }", MapImitatorWithValue.class);
        Map<String,int[]> result = mapHolder._map;
        assertEquals(2, result.size());
        assertEquals(new int[] { 3, -1 }, result.get("a"));
        assertEquals(new int[0], result.get("b"));
    }

    public void testBrokenWithDoubleAnnotations() throws Exception
    {
        try {
            @SuppressWarnings("unused")
            Broken b = MAPPER.readValue("{ \"a\" : 3 }", Broken.class);
            fail("Should have gotten an exception");
        } catch (JsonMappingException e) {
            verifyException(e, "Multiple 'any-setters'");
        }
    }

    // [JACKSON-313]
    public void testIgnored() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        _testIgnorals(mapper);
    }

    public void testIgnored383() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        _testIgnorals(mapper);
    }

    public void testProblem744() throws Exception
    {
        Bean744 bean = MAPPER.readValue("{\"name\":\"Bob\"}", Bean744.class);
        assertNotNull(bean.additionalProperties);
        assertEquals(1, bean.additionalProperties.size());
        assertEquals("Bob", bean.additionalProperties.get("name"));
    }

    public void testIssue797() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Bean797BaseImpl());
        assertEquals("{}", json);
    }

    /*
    /**********************************************************
    /* Private helper methods
    /**********************************************************
     */

    private void _testIgnorals(ObjectMapper mapper) throws Exception
    {
        Ignored bean = mapper.readValue("{\"name\":\"Bob\", \"bogus\": [ 1, 2, 3], \"dummy\" : 13 }", Ignored.class);
        // as of 2.0, @JsonIgnoreProperties does block; @JsonIgnore not
        assertNull(bean.map.get("dummy"));
        assertEquals("[1, 2, 3]", ""+bean.map.get("bogus"));
        assertEquals("Bob", bean.map.get("name"));
        assertEquals(2, bean.map.size());
    }
}
