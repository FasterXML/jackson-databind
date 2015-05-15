package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TestKeySerializers extends BaseMapTest
{
    public static class KarlSerializer extends JsonSerializer<String>
    {
        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeFieldName("Karl");
        }
    }

    public static class NotKarlBean
    {
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    public static class KarlBean
    {
        @JsonSerialize(keyUsing = KarlSerializer.class)
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    enum ABC {
        A, B, C
    }

    static class ABCSerializer extends JsonSerializer<ABC> {
        @Override
        public void serialize(ABC value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException {
            jgen.writeFieldName("xxx"+value);
        }
    }

    static class ABCMapWrapper {
        public Map<ABC,String> stuff = new HashMap<ABC,String>();
        public ABCMapWrapper() {
            stuff.put(ABC.B, "bar");
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testNotKarl() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String serialized = mapper.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", serialized);
    }

    public void testKarl() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String serialized = mapper.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", serialized);
    }

    // [Issue#75]: caching of KeySerializers
    public void testBoth() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        final String value1 = mapper.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", value1);
        final String value2 = mapper.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", value2);
    }

    // Test custom key serializer for enum
    public void testCustomForEnum() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCSerializer());
        mapper.registerModule(mod);

        String json = mapper.writeValueAsString(new ABCMapWrapper());
        assertEquals("{\"stuff\":{\"xxxB\":\"bar\"}}", json);
    }
}
