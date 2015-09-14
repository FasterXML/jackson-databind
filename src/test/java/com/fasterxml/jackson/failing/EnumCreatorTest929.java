package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class EnumCreatorTest929 extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();

    static enum MyEnum
    {
        A, B, C;
        
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static MyEnum forValues(@JsonProperty("id") int intProp,
                @JsonProperty("name") String name)
        {
            return MyEnum.valueOf(name);
        }
    }
    
    // for [databind#929]
    public void testMultiArgEnumCreator() throws Exception
    {
        MyEnum v = MAPPER.readValue("{\"id\":3,\"name\":\"B\"}", MyEnum.class);
        assertEquals(MyEnum.B, v);
    }
}
