package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;

public class TestOverlappingTypeIdNames312 extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "a", value = Impl312.class),
            @JsonSubTypes.Type(name = "b", value = Impl312.class)
    })
    static abstract class Base312 { }

    static class Impl312 extends Base312 {
        public int x;
    }
    
    public void testOverlappingNameDeser() throws Exception
    {
        final ObjectMapper mapper = objectMapper();
        Base312 value;

        // Ensure both type ids are acceptable

        value = mapper.readValue(aposToQuotes("{'type':'a','x':7}"), Base312.class);
        assertNotNull(value);
        assertEquals(Impl312.class, value.getClass());
        assertEquals(7, ((Impl312) value).x);
        
        value = mapper.readValue(aposToQuotes("{'type':'b','x':3}"), Base312.class);
        assertNotNull(value);
        assertEquals(Impl312.class, value.getClass());
        assertEquals(3, ((Impl312) value).x);
    }
}
