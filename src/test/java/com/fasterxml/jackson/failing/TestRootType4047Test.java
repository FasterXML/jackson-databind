package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

// [databind#4047] : ObjectMapper.valueToTree will ignore the configuration SerializationFeature.WRAP_ROOT_VALUE
public class TestRootType4047Test extends BaseMapTest 
{

    @JsonRootName("event")
    static class Event {
        public Long id;
        public String name;
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    private final ObjectMapper WRAP_ROOT_MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .build();

    public void testValueToTree() throws Exception 
    {
        Event value = new Event();
        value.id = 1L;
        value.name = "foo";

        String expected = "{\"event\":{\"id\":1,\"name\":\"foo\"}}";
        
        // if this one passes... (it does)
        assertEquals(expected, WRAP_ROOT_MAPPER.writeValueAsString(value));

        // this one should pass also (but fails)
        assertEquals(expected, WRAP_ROOT_MAPPER.valueToTree(value).toString());
    }
}
