package com.fasterxml.jackson.databind.seq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// for [databind#827]
public class PolyMapWriter827Test extends BaseMapTest
{
    static class CustomKey {
        String a;
        int b;

        @Override
        public String toString() { return "BAD-KEY"; }
    }

    public class CustomKeySerializer extends JsonSerializer<CustomKey> {
        @Override
        public void serialize(CustomKey key, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeFieldName(key.a + "," + key.b);
        }
    }

    public void testPolyCustomKeySerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(NoCheckSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        mapper.registerModule(new SimpleModule("keySerializerModule")
            .addKeySerializer(CustomKey.class, new CustomKeySerializer()));

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
