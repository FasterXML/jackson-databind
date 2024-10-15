package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;

// for [databind#1807]
public class MapDeserializerCachingTest
{
    public static class NonAnnotatedMapHolderClass {
        public Map<String, String> data = new TreeMap<String, String>();
    }

    public static class MapHolder {
        @JsonDeserialize(keyUsing = MyKeyDeserializer.class)
        public Map<String, String> data = new TreeMap<String, String>();
    }

    public static class MyKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            return key + " (CUSTOM)";
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testCachedSerialize() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = a2q("{'data':{'1st':'onedata','2nd':'twodata'}}");

        // Do deserialization with non-annotated map property
        NonAnnotatedMapHolderClass ignored = mapper.readValue(json, NonAnnotatedMapHolderClass.class);
        assertTrue(ignored.data.containsKey("1st"));
        assertTrue(ignored.data.containsKey("2nd"));

//mapper = new ObjectMapper();

        MapHolder model2 = mapper.readValue(json, MapHolder.class);
        if (!model2.data.containsKey("1st (CUSTOM)")
            || !model2.data.containsKey("2nd (CUSTOM)")) {
            fail("Not using custom key deserializer for input: "+json+"; resulted in: "+model2.data);
        }
    }
}
