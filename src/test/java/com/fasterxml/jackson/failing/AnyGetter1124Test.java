package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

// for [databind#1124]
public class AnyGetter1124Test extends BaseMapTest
{
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

    @SuppressWarnings("serial")
    static class MyUCSerializer extends StdScalarSerializer<String>
    {
        public MyUCSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeString(value.toUpperCase());
        }
    }
    
    public void testAnyGetterWithValueSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Bean1124 input = new Bean1124();
        input.addAdditionalProperty("key", "value");
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"key\":\"VALUE\"}", json);
    }
}
