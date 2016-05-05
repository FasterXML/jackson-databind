package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;

public class EnumCreatorTest extends BaseMapTest
{
    static enum MyEnum960
    {
        VALUE, BOGUS;
        
        @JsonCreator
        public static MyEnum960 getInstance() {
            return VALUE;
        }
    }

    static class MyEnum960Wrapper {
        public MyEnum960 value;
    }
    
    private final ObjectMapper MAPPER = new ObjectMapper();

    // for [databind#960]
    public void testNoArgEnumCreator() throws Exception
    {
        MyEnum960 v = MAPPER.readValue("{\"value\":\"bogus\"}", MyEnum960.class);
        assertEquals(MyEnum960.VALUE, v);
    }
}
