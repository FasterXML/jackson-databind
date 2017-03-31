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
    /* Test methods, wrapper keys
    /**********************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();

    public void testBooleanMapKeyDeserialization() throws Exception
    {
        TypeReference<?> type = new TypeReference<MapWrapper<Boolean, String>>() { };
        MapWrapper<byte[], String> result = MAPPER.readValue(aposToQuotes("{'map':{'true':'foobar'}}"), type);
                
        assertEquals(1, result.map.size());
        Assert.assertEquals(Boolean.TRUE, result.map.entrySet().iterator().next().getKey());

        result = MAPPER.readValue(aposToQuotes("{'map':{'false':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Boolean.FALSE, result.map.entrySet().iterator().next().getKey());
    }

    public void testByteMapKeyDeserialization() throws Exception
    {
        TypeReference<?> type = new TypeReference<MapWrapper<Byte, String>>() { };
        MapWrapper<byte[], String> result = MAPPER.readValue(aposToQuotes("{'map':{'13':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Byte.valueOf((byte) 13), result.map.entrySet().iterator().next().getKey());
    }

    public void testShortMapKeyDeserialization() throws Exception
    {
        TypeReference<?> type = new TypeReference<MapWrapper<Short, String>>() { };
        MapWrapper<byte[], String> result = MAPPER.readValue(aposToQuotes("{'map':{'13':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Short.valueOf((short) 13), result.map.entrySet().iterator().next().getKey());
    }

    public void testIntegerMapKeyDeserialization() throws Exception
    {
        TypeReference<?> type = new TypeReference<MapWrapper<Integer, String>>() { };
        MapWrapper<byte[], String> result = MAPPER.readValue(aposToQuotes("{'map':{'-3':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Integer.valueOf(-3), result.map.entrySet().iterator().next().getKey());
    }

    public void testLongMapKeyDeserialization() throws Exception
    {
        TypeReference<?> type = new TypeReference<MapWrapper<Long, String>>() { };
        MapWrapper<byte[], String> result = MAPPER.readValue(aposToQuotes("{'map':{'42':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Long.valueOf(42), result.map.entrySet().iterator().next().getKey());
    }

    public void testFloatMapKeyDeserialization() throws Exception
    {
        TypeReference<?> type = new TypeReference<MapWrapper<Float, String>>() { };
        MapWrapper<byte[], String> result = MAPPER.readValue(aposToQuotes("{'map':{'3.5':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Float.valueOf(3.5f), result.map.entrySet().iterator().next().getKey());
    }

    public void testDoubleMapKeyDeserialization() throws Exception
    {
        TypeReference<?> type = new TypeReference<MapWrapper<Double, String>>() { };
        MapWrapper<byte[], String> result = MAPPER.readValue(aposToQuotes("{'map':{'0.25':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Double.valueOf(0.25), result.map.entrySet().iterator().next().getKey());
    }

    /*
    /**********************************************************
    /* Test methods, other
    /**********************************************************
     */

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

    public void testByteArrayMapKeyDeserialization() throws Exception
    {
        byte[] binary = new byte[] { 1, 2, 4, 8, 16, 33, 79 };
        String encoded = Base64Variants.MIME.encode(binary);

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
