package com.fasterxml.jackson.failing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;

public class AnyPropSorting518Test extends BaseMapTest
{
    @JsonPropertyOrder(alphabetic = true)
    static class Bean
    {
        public int b;

        protected Map<String,Object> extra = new HashMap<>();

        public int a;

        public Bean(int a, int b, Map<String,Object> x) {
            this.a = a;
            this.b = b;
            extra = x;
        }

        @JsonAnyGetter
        public Map<String,Object> getExtra() { return extra; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testAnyBeanWithSort() throws Exception
    {
        Map<String,Object> extra = new LinkedHashMap<>();
        extra.put("y", 4);
        extra.put("x", 3);
        String json = MAPPER.writeValueAsString(new Bean(1, 2, extra));
        assertEquals(a2q("{'a':1,'b':2,'x':3,'y':4}"), json);
    }
}
