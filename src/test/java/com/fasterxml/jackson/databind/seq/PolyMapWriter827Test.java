package com.fasterxml.jackson.databind.seq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

// for [databind#827]
@SuppressWarnings("serial")
public class PolyMapWriter827Test extends BaseMapTest
{
    static class CustomKey {
        String a;
        int b;

        @Override
        public String toString() { return "BAD-KEY"; }
    }

    public class CustomKeySerializer extends StdSerializer<CustomKey> {
        public CustomKeySerializer() { super(CustomKey.class); }
        @Override
        public void serialize(CustomKey key, JsonGenerator g, SerializerProvider serializerProvider) throws IOException {
            g.writeFieldName(key.a + "," + key.b);
        }
    }

    public void testPolyCustomKeySerializer() throws Exception
    {
        ObjectMapper mapper = ObjectMapper.builder()
                .enableDefaultTyping(DefaultTyping.NON_FINAL)
                .addModule(new SimpleModule("keySerializerModule")
                        .addKeySerializer(CustomKey.class, new CustomKeySerializer()))
                .build();
        Map<CustomKey, String> map = new HashMap<CustomKey, String>();
        CustomKey key = new CustomKey();
        key.a = "foo";
        key.b = 1;
        map.put(key, "bar");

        final ObjectWriter writer = mapper.writerFor(new TypeReference<Map<CustomKey,String>>() { });
        String json = writer.writeValueAsString(map);
        Assert.assertEquals("[\"java.util.HashMap\",{\"foo,1\":\"bar\"}]", json);
    }
}
