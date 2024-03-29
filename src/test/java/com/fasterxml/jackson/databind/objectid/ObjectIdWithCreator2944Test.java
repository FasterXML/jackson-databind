package com.fasterxml.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectIdWithCreator2944Test extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class JsonBean2944 {
        String _id, _value;
        String _setterId;

        @JsonCreator
        public JsonBean2944(@JsonProperty("id") String id, @JsonProperty("value") String value) {
            _id = id;
            _value = value;
        }

        public void setId(String v) {
            _setterId = v;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2944]
    @Test
    public void testObjectIdWithCreator() throws Exception {
        JsonBean2944 result = MAPPER.readValue(a2q("{'id': 'myId','value': 'myValue'}"),
                JsonBean2944.class);
        assertNotNull(result);
        assertEquals("myId", result._id,
            "Incorrect creator-passed-id (setter id: ["+result._setterId+"])");
    }
}
