package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class AnyGetterTest extends BaseMapTest
{
    static class Bean
    {
        final static Map<String,Boolean> extra = new HashMap<String,Boolean>();
        static {
            extra.put("a", Boolean.TRUE);
        }
        
        public int getX() { return 3; }

        @JsonAnyGetter
        public Map<String,Boolean> getExtra() { return extra; }
    }

    static class AnyOnlyBean
    {
        @JsonAnyGetter
        public Map<String,Integer> any() {
            HashMap<String,Integer> map = new HashMap<String,Integer>();
            map.put("a", 3);
            return map;
        }
    }

    // For [databind#1376]: allow disabling any-getter
    static class NotEvenAnyBean extends AnyOnlyBean
    {
        @JsonAnyGetter(enabled=false)
        @Override
        public Map<String,Integer> any() {
            throw new RuntimeException("Should not get called!)");
        }

        public int getValue() { return 42; }
    }
    
    static class MapAsAny
    {
        protected Map<String,Object> stuff = new LinkedHashMap<String,Object>();
        
        @JsonAnyGetter
        public Map<String,Object> any() {
            return stuff;
        }

        public void add(String key, Object value) {
            stuff.put(key, value);
        }
    }

    static class Issue705Bean
    {
        protected Map<String,String> stuff;

        public Issue705Bean(String key, String value) {
            stuff = new LinkedHashMap<String,String>();
            stuff.put(key, value);
        }
        
        @JsonSerialize(using = Issue705Serializer.class)
//    @JsonSerialize(converter = MyConverter.class)
        @JsonAnyGetter
        public Map<String, String> getParameters(){
            return stuff;
        }
    }

    static class Issue705Serializer extends StdSerializer<Object>
    {
        public Issue705Serializer() {
            super(Map.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException
        {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?,?> entry : ((Map<?,?>) value).entrySet()) {
                sb.append('[').append(entry.getKey()).append('/').append(entry.getValue()).append(']');
            }
            jgen.writeStringField("stuff", sb.toString());
        }
    }

    // [databind#1124]
    static class Bean1124
    {
        protected Map<String,String> additionalProperties;

        public void addAdditionalProperty(String key, String value) {
            if (additionalProperties == null) {
                additionalProperties = new HashMap<String,String>();
            }
            additionalProperties.put(key,value);
        }
        
        public void setAdditionalProperties(Map<String, String> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        @JsonAnyGetter
        @JsonSerialize(contentUsing=MyUCSerializer.class)
        public Map<String,String> getAdditionalProperties() { return additionalProperties; }
    }

    // [databind#1124]
    static class MyUCSerializer extends StdScalarSerializer<String>
    {
        public MyUCSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeString(value.toUpperCase());
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testSimpleAnyBean() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Bean());
        Map<?,?> map = MAPPER.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("x"));
        assertEquals(Boolean.TRUE, map.get("a"));
    }

    public void testAnyOnly() throws Exception
    {
        ObjectMapper m;

        // First, with normal fail settings:
        m = jsonMapperBuilder()
                .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
        assertEquals("{\"a\":3}", m.writeValueAsString(new AnyOnlyBean()));

        // then without fail
        String json = m.writer()
                .without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .writeValueAsString(new AnyOnlyBean());
        assertEquals("{\"a\":3}", json);
    }

    public void testAnyDisabling() throws Exception
    {
        String json = MAPPER.writeValueAsString(new NotEvenAnyBean());
        assertEquals(aposToQuotes("{'value':42}"), json);
    }

    // Trying to repro [databind#577]
    public void testAnyWithNull() throws Exception
    {
        MapAsAny input = new MapAsAny();
        input.add("bar", null);
        assertEquals(aposToQuotes("{'bar':null}"),
                MAPPER.writeValueAsString(input));
    }

    public void testIssue705() throws Exception
    {
        Issue705Bean input = new Issue705Bean("key", "value");        
        String json = MAPPER.writeValueAsString(input);
        assertEquals("{\"stuff\":\"[key/value]\"}", json);
    }

    // [databind#1124]
    public void testAnyGetterWithValueSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Bean1124 input = new Bean1124();
        input.addAdditionalProperty("key", "value");
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"key\":\"VALUE\"}", json);
    }
}
