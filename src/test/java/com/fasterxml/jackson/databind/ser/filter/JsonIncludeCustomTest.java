package com.fasterxml.jackson.databind.ser.filter;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

// Tests for [databind#888]
public class JsonIncludeCustomTest extends BaseMapTest
{
    static class FooFilter {
        @Override
        public boolean equals(Object other) {
            if (other == null) { // do NOT filter out nulls
                return false;
            }
            // in fact, only filter out exact String "foo"
            return "foo".equals(other);
        }
    }

    // for testing prob with `equals(null)` which SHOULD be allowed
    static class BrokenFilter {
        @Override
        public boolean equals(Object other) {
            /*String str = */ other.toString();
            return false;
        }
    }
    
    static class FooBean {
        @JsonInclude(value=JsonInclude.Include.CUSTOM,
                valueFilter=FooFilter.class)
        public String value;

        public FooBean(String v) { value = v; }
    }

    static class FooMapBean {
        @JsonInclude(content=JsonInclude.Include.CUSTOM,
                contentFilter=FooFilter.class)
        public Map<String,String> stuff = new LinkedHashMap<String,String>();

        public FooMapBean add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    static class BrokenBean {
        @JsonInclude(value=JsonInclude.Include.CUSTOM,
                valueFilter=BrokenFilter.class)
        public String value;

        public BrokenBean(String v) { value = v; }
    }

    /*
    /**********************************************************
    /* Test methods, success
    /**********************************************************
     */

    final private ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleCustomFilter() throws Exception
    {
        assertEquals(aposToQuotes("{'value':'x'}"), MAPPER.writeValueAsString(new FooBean("x")));
        assertEquals("{}", MAPPER.writeValueAsString(new FooBean("foo")));
    }

    public void testCustomFilterWithMap() throws Exception
    {
        FooMapBean input = new FooMapBean()
                .add("a", "1")
                .add("b", "foo")
                .add("c", "2");
        
        assertEquals(aposToQuotes("{'stuff':{'a':'1','c':'2'}}"), MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************
    /* Test methods, fail handling
    /**********************************************************
     */
    
    public void testBrokenFilter() throws Exception
    {
        try {
            String json = MAPPER.writeValueAsString(new BrokenBean("foo"));
            fail("Should not pass, produced: "+json);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Problem determining whether filter of type");
            verifyException(e, "filter out `null`");
        }
    }
}
