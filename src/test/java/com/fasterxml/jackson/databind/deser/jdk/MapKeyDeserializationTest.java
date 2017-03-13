package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

import org.junit.Assert;

public class MapKeyDeserializationTest extends BaseMapTest
{
    static class FullName {
        private String _firstname, _lastname;

        private FullName(String firstname, String lastname) {
            _firstname = firstname;
            _lastname = lastname;
        }

        @JsonCreator
        public static FullName valueOf(String value) {
            String[] mySplit = value.split("\\.");
            return new FullName(mySplit[0], mySplit[1]);
        }

        public static FullName valueOf(String firstname, String lastname) {
            return new FullName(firstname, lastname);
        }

        @JsonValue
        @Override
        public String toString() {
            return _firstname + "." + _lastname;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();
    
    public void testDeserializeKeyViaFactory() throws Exception
    {
        Map<FullName, Double> map =
            MAPPER.readValue("{\"first.last\": 42}",
                    new TypeReference<Map<FullName, Double>>() { });
        Map.Entry<FullName, Double> entry = map.entrySet().iterator().next();
        FullName key = entry.getKey();
        assertEquals(key._firstname, "first");
        assertEquals(key._lastname, "last");
        assertEquals(entry.getValue().doubleValue(), 42, 0);
    }

    public void testByteArrayKeyDeserialization() throws Exception
    {
        byte[] binary = new byte[] { 1, 2, 4, 8, 16, 33, 79 };
        String encoded = Base64Variants.MIME.encode(binary);

        // First, using wrapper
        MapWrapper<byte[], String> result = MAPPER.readValue(
                aposToQuotes("{'map':{'"+encoded+"':'foobar'}}"),
                new TypeReference<MapWrapper<byte[], String>>() { });
        assertEquals(1, result.map.size());
        Map.Entry<byte[],String> entry = result.map.entrySet().iterator().next();
        assertEquals("foobar", entry.getValue());
        byte[] key = entry.getKey();
        Assert.assertArrayEquals(binary, key);
    }
}
