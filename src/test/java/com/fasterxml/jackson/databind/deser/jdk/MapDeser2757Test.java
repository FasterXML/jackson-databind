package com.fasterxml.jackson.databind.deser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil.StringWrapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil.IntWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

@SuppressWarnings("serial")
public class MapDeser2757Test
{
    static class MyMap extends LinkedHashMap<String, String> {
        public MyMap() { }

        public void setValue(StringWrapper w) { }
        public void setValue(IntWrapper w) { }

        public long getValue() { return 0L; }
    }

    // [databind#2757]: should allow deserialization as Map despite conflicting setters
    @Test
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
