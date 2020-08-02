package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonIgnoreProperties2803Test extends BaseMapTest
{
    // [databind#2803]
    static class Building2803 {
        @JsonIgnoreProperties({"something"})
        @JsonProperty
        private Room2803 lobby;
    }

    static class Museum2803 extends Building2803 {
    }

    static class Room2803 {
        public Building2803 something;
        public String id;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2803]
    public void testIgnoreProps2803() throws Exception
    {
        final String DOC = "{\"lobby\":{\"id\":\"L1\"}}";

        // Important! Must do both calls, in this order
        Museum2803 museum = MAPPER.readValue(DOC, Museum2803.class);
        assertNotNull(museum);
//System.err.println();
//System.err.println("------------------------------");
//System.err.println();
        Building2803 building = MAPPER.readValue(DOC, Building2803.class);
        assertNotNull(building);
    }
}
