package com.fasterxml.jackson.databind.ser.filter;

import com.fasterxml.jackson.annotation.*;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TokenStreamContext;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.*;

/**
 * Tests for verifying that bean property filtering using JsonFilter
 * works as expected.
 */
public class TestJsonFilter extends BaseMapTest
{
    @JsonFilter("RootFilter")
    @JsonPropertyOrder({ "a", "b" })
    static class Bean {
        public String a = "a";
        public String b = "b";
    }

    @JsonFilter("checkSiblingContextFilter")
    static class CheckSiblingContextBean {
        public A a = new A();
        public B b = new B();
        @JsonFilter("checkSiblingContextFilter")
        static class A {
        }
        @JsonFilter("checkSiblingContextFilter")
        static class B {
            public C c = new C();
            @JsonFilter("checkSiblingContextFilter")
            static class C {
            }
        }
    }

    static class CheckSiblingContextFilter extends SimpleBeanPropertyFilter {
        @Override
        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer) throws Exception {
            TokenStreamContext sc = jgen.getOutputContext();

            if (writer.getName() != null && writer.getName().equals("c")) {
                //This assertion is failing as sc.getParent() incorrectly returns 'a'. If you comment out the member 'a'
                // in the CheckSiblingContextBean, you'll see that the sc.getParent() correctly returns 'b'
                assertEquals("b", sc.getParent().currentName());
            }
            writer.serializeAsField(bean, jgen, prov);
        }
    }

    public void testCheckSiblingContextFilter() {
        FilterProvider prov = new SimpleFilterProvider().addFilter("checkSiblingContextFilter",
                new CheckSiblingContextFilter());

        ObjectMapper mapper = ObjectMapper.builder()
                .filterProvider(prov)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
        mapper.valueToTree(new CheckSiblingContextBean());
    }

    // [databind#89]
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

    // [databind#306]: JsonFilter for properties, too!

    @JsonPropertyOrder(alphabetic=true)
    static class FilteredProps
    {
        // will default to using "RootFilter", only including 'a'
        public Bean first = new Bean();

        // but minimal includes 'b'
        @JsonFilter("b")
        public Bean second = new Bean();
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleInclusionFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        assertEquals("{\"a\":\"a\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));

        ObjectMapper mapper = ObjectMapper.builder()
                .filterProvider(prov)
                .build();
        assertEquals("{\"a\":\"a\"}", mapper.writeValueAsString(new Bean()));
    }

    public void testIncludeAllFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAll());
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }
    
    public void testSimpleExclusionFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("a"));
        assertEquals("{\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    // should handle missing case gracefully
    public void testMissingFilter() throws Exception
    {
        // First: default behavior should be to throw an exception
        try {
            MAPPER.writeValueAsString(new Bean());
            fail("Should have failed without configured filter");
        } catch (JsonMappingException e) { // should be resolved to a MappingException (internally may be something else)
            verifyException(e, "Cannot resolve PropertyFilter with id 'RootFilter'");
        }
        
        // but when changing behavior, should work difference
        SimpleFilterProvider fp = new SimpleFilterProvider().setFailOnUnknownId(false);
        ObjectMapper mapper = ObjectMapper.builder()
                .filterProvider(fp)
                .build();
        String json = mapper.writeValueAsString(new Bean());
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", json);
    }

    public void testDefaultFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }
    
    // [databind#89] combining @JsonIgnore, @JsonProperty
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

    // Wrt [databind#306]
    public void testFilterOnProperty() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider()
            .addFilter("RootFilter", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
            .addFilter("b", SimpleBeanPropertyFilter.filterOutAllExcept("b"));

        assertEquals("{\"first\":{\"a\":\"a\"},\"second\":{\"b\":\"b\"}}",
                MAPPER.writer(prov).writeValueAsString(new FilteredProps()));
    }
}
