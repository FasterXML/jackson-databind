package com.fasterxml.jackson.databind.deser.jdk;

import java.util.*;

import com.fasterxml.jackson.databind.*;

@SuppressWarnings("serial")
public class MapDeser2757Test extends BaseMapTest
{
    static class MyMap extends LinkedHashMap<String, String> {
        public MyMap() { }

        public void setValue(StringWrapper w) { }
        public void setValue(IntWrapper w) { }

        public long getValue() { return 0L; }
    }

    // [databind#2757]: should allow deserialization as Map despite conflicting setters
    public void testCanDeserializeMap() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .build();

        MyMap input = new MyMap();
        input.put("a", "b");
        final String json = mapper.writeValueAsString(input);
        MyMap x = mapper.readValue(json, MyMap.class);
        assertEquals(1, x.size());
        assertEquals("b", input.get("a"));
    }
}
