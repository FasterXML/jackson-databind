package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class ObjectIdWithCreator2944Test extends BaseMapTest
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class JsonBean2944 {

        private final String id;
        private final String value;

        @JsonCreator
        public JsonBean2944(@JsonProperty("id") String id, @JsonProperty("value") String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2944]
    public void testObjectIdWithCreator() throws Exception {
        JsonBean2944 result = MAPPER.readValue(a2q("{'id': 'myId','value': 'myValue'}"),
                JsonBean2944.class);
        assertNotNull(result);
        assertEquals("myId", result.getId());
    }
}
