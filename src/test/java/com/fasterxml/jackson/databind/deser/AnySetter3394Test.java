package com.fasterxml.jackson.databind.deser;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

// for [databind#3394]
public class AnySetter3394Test extends BaseMapTest
{
    static class AnySetter3394Bean {
        @JsonAnySetter
        public JsonNode extraData = new ObjectNode(null);
    }

    private static class JsonAnySetterOnMapXXX {
        public int id;

        @JsonAnySetter
        public Map<String, String> other;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testAnySetterWithJsonNode() throws Exception
    {
        final String DOC = a2q("{'test':3,'value':true}");
        AnySetter3394Bean bean = MAPPER.readValue(DOC, AnySetter3394Bean.class);
        assertEquals(DOC, ""+bean.extraData);
    }

    /*
    public void testJsonAnySetterOnMap() throws Exception {
        JsonAnySetterOnMapXXX result = MAPPER.readValue("{\"id\":2,\"name\":\"Joe\", \"city\":\"New Jersey\"}",
                JsonAnySetterOnMapXXX.class);
        assertEquals(2, result.id);
        assertNotNull(result.other);
        assertEquals("Joe", result.other.get("name"));
        assertEquals("New Jersey", result.other.get("city"));
    }
    */
}
