package com.fasterxml.jackson.failing;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Tests for [#171]
public class TestUnwrappedWithPrefix extends BaseMapTest
{
    static class MapUnwrap {

        public MapUnwrap() { }
        public MapUnwrap(String key, Object value) {
            map = Collections.singletonMap(key, value);
        }

        @JsonUnwrapped(prefix="map.")
        public Map<String, Object> map;
    }

    static class Parent {
        @JsonUnwrapped(prefix="c1")
        public Child c1;
        @JsonUnwrapped(prefix="c2")
        public Child c2;
      }

    static class Child {
        @JsonUnwrapped(prefix="sc2")
        public SubChild sc1;
      }

    static class SubChild {
        public String value;
    }
    
    // // // Reuse mapper to keep tests bit faster

    private final ObjectMapper MAPPER = new ObjectMapper();

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    public void testMapUnwrapSerialize() throws Exception
    {
        String json = MAPPER.writeValueAsString(new MapUnwrap("test", 6));
        assertEquals("{\"map.test\": 6}", json);
    }

    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */

    public void testMapUnwrapDeserialize() throws Exception
    {
        MapUnwrap root = MAPPER.readValue("{\"map.test\": 6}", MapUnwrap.class);

        assertEquals(1, root.map.size());
        assertEquals(6, ((Number)root.map.get("test")).intValue());
    }

    public void testIssue226() throws Exception
    {
        Parent input = new Parent();
        input.c1 = new Child();
        input.c1.sc1 = new SubChild();
        input.c1.sc1.value = "a";
        input.c2 = new Child();
        input.c2.sc1 = new SubChild();
        input.c2.sc1.value = "b";

        String json = MAPPER.writeValueAsString(input);
//System.out.println("JSON -> "+json);

        Parent output = MAPPER.readValue(json, Parent.class);
        assertNotNull(output.c1);
        assertNotNull(output.c2);

        assertNotNull(output.c1.sc1);
        assertNotNull(output.c2.sc1);
        
        assertEquals("a", output.c1.sc1.value);
        assertEquals("b", output.c2.sc1.value);
    }
}
