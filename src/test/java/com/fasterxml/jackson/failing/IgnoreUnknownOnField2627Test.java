package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.databind.*;

public class IgnoreUnknownOnField2627Test extends BaseMapTest
{
    // [databind#2627]

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyPojoValue {
        @JsonIgnoreProperties(ignoreUnknown = true)
        MyPojo2627 value;

        public MyPojo2627 getValue() {
            return value;
        }
    }

    static class MyPojo2627 {
        public String name;
    }

    // [databind#2627]
    public void testFieldIgnoral() throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{\"value\": {\"name\": \"my_name\", \"extra\": \"val\"}, \"type\":\"Json\"}";
        MyPojoValue value = objectMapper.readValue(json, MyPojoValue.class);
        assertNotNull(value);
        assertNotNull(value.getValue());
        assertEquals("my_name", value.getValue().name);
    }

}
