package com.fasterxml.jackson.failing;

import java.beans.ConstructorProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class ReadOnly1345Test extends BaseMapTest
{
    static class Foo1345 {
        @JsonProperty(access=JsonProperty.Access.READ_ONLY)
        public String id;
        public String name;

        @ConstructorProperties({ "id", "name" })
        public Foo1345(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public void testReadOnly1345() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Foo1345 result = mapper.readValue("{\"name\":\"test\"}", Foo1345.class);
        assertNotNull(result);
        assertEquals("test", result.name);
    }
}
