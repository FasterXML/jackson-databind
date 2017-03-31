package com.fasterxml.jackson.databind.ser.jdk;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;

@SuppressWarnings("serial")
public class MapKeySerializationTest extends BaseMapTest
{
    // for [databind#47]
    public static class Wat
    {
        private final String wat;

        @JsonCreator
        Wat(String wat) {
            this.wat = wat;
        }

        @JsonValue
        public String getWat() {
            return wat;
        }

        @Override
        public String toString() {
            return "(String)[Wat: " + wat + "]";
        }
    }

    static class WatMap extends HashMap<Wat,Boolean> { }

    static class DefaultKeySerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException
        {
            g.writeFieldName("DEFAULT:"+value);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();

    // [databind#47]
    public void testMapJsonValueKey47() throws Exception
    {
        WatMap input = new WatMap();
        input.put(new Wat("3"), true);

        String json = MAPPER.writeValueAsString(input);
        assertEquals(aposToQuotes("{'3':true}"), json);
    }    

    // [databind#682]
    public void testClassKey() throws IOException
    {
        Map<Class<?>,Integer> map = new LinkedHashMap<Class<?>,Integer>();
        map.put(String.class, 2);
        String json = MAPPER.writeValueAsString(map);
        assertEquals(aposToQuotes("{'java.lang.String':2}"), json);
    }

    public void testDefaultKeySerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.getSerializerProvider().setDefaultKeySerializer(new DefaultKeySerializer());
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        assertEquals("{\"DEFAULT:a\":\"b\"}", m.writeValueAsString(map));
    }

    // [databind#1552]
    public void testMapsWithBinaryKeys() throws Exception
    {
        byte[] binary = new byte[] { 1, 2, 3, 4, 5 };

        // First, using wrapper
        MapWrapper<byte[], String> input = new MapWrapper<>(binary, "stuff");
        String expBase64 = Base64Variants.MIME.encode(binary);
        
        assertEquals(aposToQuotes("{'map':{'"+expBase64+"':'stuff'}}"),
                MAPPER.writeValueAsString(input));

        // and then dynamically..
        Map<byte[],String> map = new LinkedHashMap<>();
        map.put(binary, "xyz");
        assertEquals(aposToQuotes("{'"+expBase64+"':'xyz'}"),
                MAPPER.writeValueAsString(map));
    }
}
