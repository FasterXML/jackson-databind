package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.*;

/**
 * Tests for verifying that bean property filtering using JsonFilter
 * works as expected.
 */
public class TestFiltering extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    @JsonFilter("RootFilter")
    static class Bean {
        public String a = "a";
        public String b = "b";
    }

    
    // [Issue#89]
    static class Pod
    {
        protected String username;

//        @JsonProperty(value = "user_password")
        protected String userPassword;

        public String getUsername() {
            return username;
        }

        public void setUsername(String value) {
            this.username = value;
        }

        @JsonIgnore
        @JsonProperty(value = "user_password")
        public java.lang.String getUserPassword() {
            return userPassword;
        }

        @JsonProperty(value = "user_password")
        public void setUserPassword(String value) {
            this.userPassword = value;
        }

    }    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testSimpleInclusionFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        assertEquals("{\"a\":\"a\"}", mapper.writer(prov).writeValueAsString(new Bean()));

        // [JACKSON-504]: also verify it works via mapper
        mapper = new ObjectMapper();
        mapper.setFilters(prov);
        assertEquals("{\"a\":\"a\"}", mapper.writeValueAsString(new Bean()));
    }

    public void testSimpleExclusionFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("a"));
        assertEquals("{\"b\":\"b\"}", mapper.writer(prov).writeValueAsString(new Bean()));
    }

    // should handle missing case gracefully
    public void testMissingFilter() throws Exception
    {
        // First: default behavior should be to throw an exception
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValueAsString(new Bean());
            fail("Should have failed without configured filter");
        } catch (JsonMappingException e) { // should be resolved to a MappingException (internally may be something else)
            verifyException(e, "Can not resolve BeanPropertyFilter with id 'RootFilter'");
        }
        
        // but when changing behavior, should work difference
        SimpleFilterProvider fp = new SimpleFilterProvider().setFailOnUnknownId(false);
        mapper.setFilters(fp);
        String json = mapper.writeValueAsString(new Bean());
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", json);
    }
    
    // defaulting, as per [JACKSON-449]
    public void testDefaultFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"b\"}", mapper.writer(prov).writeValueAsString(new Bean()));
    }

    // [Issue#89] combining @JsonIgnore, @JsonProperty
    public void testIssue89() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Pod pod = new Pod();
        pod.username = "Bob";
        pod.userPassword = "s3cr3t!";

        String json = mapper.writeValueAsString(pod);

        assertEquals("{\"username\":\"Bob\"}", json);

        Pod pod2 = mapper.readValue("{\"username\":\"Bill\",\"user_password\":\"foo!\"}", Pod.class);
        assertEquals("Bill", pod2.username);
        assertEquals("foo!", pod2.userPassword);
    }
}
