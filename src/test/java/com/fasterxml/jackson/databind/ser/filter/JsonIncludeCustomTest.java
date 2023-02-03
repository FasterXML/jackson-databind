package com.fasterxml.jackson.databind.ser.filter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    // [databind#3481]
    static class CountingFooFilter {
        public final static AtomicInteger counter = new AtomicInteger(0);

        @Override
        public boolean equals(Object other) {
            counter.incrementAndGet();
            return "foo".equals(other);
        }
    }

    static class CountingFooBean {
        @JsonInclude(value=JsonInclude.Include.CUSTOM,
                valueFilter=CountingFooFilter.class)
        public String value;

        public CountingFooBean(String v) { value = v; }
    }

    /*
    /**********************************************************
    /* Test methods, success
    /**********************************************************
     */

    final private ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleCustomFilter() throws Exception
    {
        assertEquals(a2q("{'value':'x'}"), MAPPER.writeValueAsString(new FooBean("x")));
        assertEquals("{}", MAPPER.writeValueAsString(new FooBean("foo")));
    }

    public void testCustomFilterWithMap() throws Exception
    {
        FooMapBean input = new FooMapBean()
                .add("a", "1")
                .add("b", "foo")
                .add("c", "2");

        assertEquals(a2q("{'stuff':{'a':'1','c':'2'}}"), MAPPER.writeValueAsString(input));
    }

    // [databind#3481]
    public void testRepeatedCalls() throws Exception
    {
        CountingFooFilter.counter.set(0);

        assertEquals(a2q("{'value':'x'}"),
                MAPPER.writeValueAsString(new CountingFooBean("x")));
        assertEquals(1, CountingFooFilter.counter.get());

        assertEquals("{}", MAPPER.writeValueAsString(new CountingFooBean("foo")));
        assertEquals(2, CountingFooFilter.counter.get());

        // except filter will be called again for `null`s, as per [databind#3481]
        assertEquals(a2q("{'value':null}"), MAPPER.writeValueAsString(new CountingFooBean(null)));
        assertEquals(3, CountingFooFilter.counter.get());
    }

    /*
    /**********************************************************
    /* Test methods, fail handling
    /**********************************************************
     */

    public void testBrokenFilter() throws Exception
    {
        try {
            String json = MAPPER.writeValueAsString(new BrokenBean(null));
            fail("Should not pass, produced: "+json);
        } catch (JsonMappingException e) {
            // 20-Jun-2022, tatu: Actual message seems to vary across JDKs...
            verifyException(e, "Problem determining whether filter");
            verifyException(e, "should filter out `null` values");
        }
    }
}
