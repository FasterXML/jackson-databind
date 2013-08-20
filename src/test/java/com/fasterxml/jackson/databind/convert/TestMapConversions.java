package com.fasterxml.jackson.databind.convert;

import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

public class TestMapConversions
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    final ObjectMapper MAPPER = new ObjectMapper();

    enum AB { A, B; }

    static class Bean {
        public Integer A;
        public String B;
    }

    // [Issue#287]
    
    @JsonSerialize(converter=RequestConverter.class)
    static class Request {
        public int x() {
            return 1;
        }
    }

    static class RequestConverter extends StdConverter<Request, Map<String,Object>> {
        @Override
        public Map<String,Object> convert(final Request value) {
            final Map<String, Object> test = new LinkedHashMap<String, Object>();
            final Map<String, Object> innerTest = new LinkedHashMap<String, Object>();
            innerTest.put("value", value.x());
            test.put("hello", innerTest);
            return test;
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    /**
     * Test that verifies that we can go between couple of types of Maps...
     */
    public void testMapToMap()
    {
        Map<String,Integer> input = new LinkedHashMap<String,Integer>();
        input.put("A", Integer.valueOf(3));
        input.put("B", Integer.valueOf(-4));
        Map<AB,String> output = MAPPER.convertValue(input,
                new TypeReference<Map<AB,String>>() { });
        assertEquals(2, output.size());
        assertEquals("3", output.get(AB.A));
        assertEquals("-4", output.get(AB.B));

        // Let's try the other way too... and mix up types a bit
        Map<String,Integer> roundtrip = MAPPER.convertValue(input,
                new TypeReference<TreeMap<String,Integer>>() { });
        assertEquals(2, roundtrip.size());
        assertEquals(Integer.valueOf(3), roundtrip.get("A"));
        assertEquals(Integer.valueOf(-4), roundtrip.get("B"));
    }

    public void testMapToBean()
    {
        EnumMap<AB,String> map = new EnumMap<AB,String>(AB.class);
        map.put(AB.A, "   17");
        map.put(AB.B, " -1");
        Bean bean = MAPPER.convertValue(map, Bean.class);
        assertEquals(Integer.valueOf(17), bean.A);
        assertEquals(" -1", bean.B);
    }

    public void testBeanToMap()
    {
        Bean bean = new Bean();
        bean.A = 129;
        bean.B = "13";
        EnumMap<AB,String> result = MAPPER.convertValue(bean,
                new TypeReference<EnumMap<AB,String>>() { });
        assertEquals("129", result.get(AB.A));
        assertEquals("13", result.get(AB.B));
    }

    // [Issue#287]: Odd problems with `Object` type, static typing
    public void testIssue287() throws Exception
    {
        // use local instance to ensure no caching affects it:
        final ObjectMapper mapper = new ObjectMapper();
        final Request request = new Request();
        final String retString = mapper.writeValueAsString(request);
        assertEquals("{\"hello\":{\"value\":1}}",retString);
    }
}
