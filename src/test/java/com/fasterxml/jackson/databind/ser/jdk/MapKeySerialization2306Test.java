package com.fasterxml.jackson.databind.ser.jdk;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.*;

public class MapKeySerialization2306Test extends BaseMapTest
{
    static class JsonValue2306Key {
        @JsonValue
        private String id;

        public JsonValue2306Key(String id) {
            this.id = id;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testMapKeyWithJsonValue() throws Exception
    {
        final Map<JsonValue2306Key, String> map = Collections.singletonMap(
                new JsonValue2306Key("myId"), "value");
        assertEquals(aposToQuotes("{'myId':'value'}"),
                MAPPER.writeValueAsString(map));
    }
}
