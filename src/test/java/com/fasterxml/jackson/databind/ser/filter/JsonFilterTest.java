package com.fasterxml.jackson.databind.ser.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying that bean property filtering using JsonFilter
 * works as expected.
 */
public class JsonFilterTest extends DatabindTestUtil
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

    @JsonFilter("filterB")
    @JsonPropertyOrder({ "a", "b", "c"})
    static class BeanB {
        public String a;
        public String b;
        public String c;

        public BeanB(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    static class CheckSiblingContextFilter extends SimpleBeanPropertyFilter {
        @Override
        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer) throws Exception {
            JsonStreamContext sc = jgen.getOutputContext();

            if (writer.getName() != null && writer.getName().equals("c")) {
                //This assertion is failing as sc.getParent() incorrectly returns 'a'. If you comment out the member 'a'
                // in the CheckSiblingContextBean, you'll see that the sc.getParent() correctly returns 'b'
                assertEquals("b", sc.getParent().getCurrentName());
            }
            writer.serializeAsField(bean, jgen, prov);
        }
    }

    @Test
    public void testCheckSiblingContextFilter() {
        FilterProvider prov = new SimpleFilterProvider().addFilter("checkSiblingContextFilter",
                new CheckSiblingContextFilter());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(prov);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.valueToTree(new CheckSiblingContextBean());
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

    // [Issue#306]: JsonFilter for properties, too!

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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleInclusionFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        assertEquals("{\"a\":\"a\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));

        // [JACKSON-504]: also verify it works via mapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(prov);
        assertEquals("{\"a\":\"a\"}", mapper.writeValueAsString(new Bean()));
    }

    @Test
    public void testIncludeAllFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAll());
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    @Test
    public void testExcludeAllFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
            SimpleBeanPropertyFilter.filterOutAll());
        assertEquals("{}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    @Test
    public void testSimpleExclusionFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("a"));
        assertEquals("{\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    // should handle missing case gracefully
    @Test
    public void testMissingFilter() throws Exception
    {
        // First: default behavior should be to throw an exception
        try {
            MAPPER.writeValueAsString(new Bean());
            fail("Should have failed without configured filter");
        } catch (InvalidDefinitionException e) { // should be resolved to this (internally may be something else)
            verifyException(e, "Cannot resolve PropertyFilter with id 'RootFilter'");
        }

        // but when changing behavior, should work difference
        SimpleFilterProvider fp = new SimpleFilterProvider().setFailOnUnknownId(false);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(fp);
        String json = mapper.writeValueAsString(new Bean());
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", json);
    }

    // defaulting, as per [JACKSON-449]
    @Test
    public void testDefaultFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    // [databind#89] combining @JsonIgnore, @JsonProperty
    @Test
    public void testIssue89() throws Exception
    {
        Pod pod = new Pod();
        pod.username = "Bob";
        pod.userPassword = "s3cr3t!";

        String json = MAPPER.writeValueAsString(pod);

        assertEquals("{\"username\":\"Bob\"}", json);

        Pod pod2 = MAPPER.readValue("{\"username\":\"Bill\",\"user_password\":\"foo!\"}", Pod.class);
        assertEquals("Bill", pod2.username);
        assertEquals("foo!", pod2.userPassword);
    }

    // Wrt [databind#306]
    @Test
    public void testFilterOnProperty() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider()
            .addFilter("RootFilter", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
            .addFilter("b", SimpleBeanPropertyFilter.filterOutAllExcept("b"));

        assertEquals("{\"first\":{\"a\":\"a\"},\"second\":{\"b\":\"b\"}}",
                MAPPER.writer(prov).writeValueAsString(new FilteredProps()));
    }

    @Test
    public void testAllFiltersWithSameOutput() throws Exception
    {
        // Setup
        SimpleBeanPropertyFilter[] allPossibleFilters = new SimpleBeanPropertyFilter[]{
            // Parent class : SimpleBeanPropertyFilter
            SimpleBeanPropertyFilter.filterOutAllExcept("a", "b"),
            SimpleBeanPropertyFilter.filterOutAllExcept(setOf("a", "b")),
            SimpleBeanPropertyFilter.serializeAllExcept("c"),
            SimpleBeanPropertyFilter.serializeAllExcept(setOf("c")),
            // Subclass : SerializeExceptFilter
            new SimpleBeanPropertyFilter.SerializeExceptFilter(setOf("c")),
            SimpleBeanPropertyFilter.SerializeExceptFilter.serializeAllExcept("c"),
            SimpleBeanPropertyFilter.SerializeExceptFilter.serializeAllExcept(setOf("c")),
            SimpleBeanPropertyFilter.SerializeExceptFilter.filterOutAllExcept("a", "b"),
            SimpleBeanPropertyFilter.SerializeExceptFilter.filterOutAllExcept(setOf("a", "b")),
            // Subclass : FilterExceptFilter
            new SimpleBeanPropertyFilter.FilterExceptFilter(setOf("a", "b")),
            SimpleBeanPropertyFilter.FilterExceptFilter.serializeAllExcept("c"),
            SimpleBeanPropertyFilter.FilterExceptFilter.serializeAllExcept(setOf("c")),
            SimpleBeanPropertyFilter.FilterExceptFilter.filterOutAllExcept(setOf("a", "b")),
            SimpleBeanPropertyFilter.FilterExceptFilter.filterOutAllExcept("a", "b")
        };

        // Tests
        for (SimpleBeanPropertyFilter filter : allPossibleFilters) {
            BeanB beanB = new BeanB("aa", "bb", "cc");
            SimpleFilterProvider prov = new SimpleFilterProvider().addFilter("filterB", filter);

            String jsonStr = MAPPER.writer(prov).writeValueAsString(beanB);

            assertEquals(a2q("{'a':'aa','b':'bb'}"), jsonStr);
        }
    }

    private Set<String> setOf(String... properties) {
        Set<String> set = new HashSet<>(properties.length);
        set.addAll(Arrays.asList(properties));
        return set;
    }
}
