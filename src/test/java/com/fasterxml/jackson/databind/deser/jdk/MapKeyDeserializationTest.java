package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.junit.Assert;

public class MapKeyDeserializationTest extends BaseMapTest
{
    static class FullName {
        String _firstname, _lastname;

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

    // [databind#2725]
    enum TestEnum2725 {
        FOO(1);

        private final int i;

        TestEnum2725(final int i) {
            this.i = i;
        }

        @JsonValue
        public int getI() {
            return i;
        }

        @JsonCreator
        public static TestEnum2725 getByIntegerId(final Integer id) {
            return id == FOO.i ? FOO : null;
        }

        @JsonCreator
        public static TestEnum2725 getByStringId(final String id) {
            return Integer.parseInt(id) == FOO.i ? FOO : null;
        }
    }

    // for [databind#2158]
    private static final class DummyDto2158 {
        @JsonValue
        private final String value;

        private DummyDto2158(String value) {
            this.value = value;
        }

        @JsonCreator
        static DummyDto2158 fromValue(String value) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Value must be nonempty");
            }

            return new DummyDto2158(value.toLowerCase(Locale.ROOT));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof DummyDto2158 && ((DummyDto2158) o).value.equals(value);
        }

        @Override
        public int hashCode() { return Objects.hash(value); }

        @Override
        public String toString() { return String.format("DummyDto{value=%s}", value); }
    }

    private static final TypeReference<Map<DummyDto2158, Integer>> MAP_TYPE_2158 =
            new TypeReference<Map<DummyDto2158, Integer>>() {};

    /*
    /**********************************************************************
    /* Test methods, wrapper keys
    /**********************************************************************
     */

    final private ObjectMapper MAPPER = objectMapper();

    public void testBooleanMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Boolean, String>> type = new TypeReference<MapWrapper<Boolean, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'true':'foobar'}}"), type);

        assertEquals(1, result.map.size());
        Assert.assertEquals(Boolean.TRUE, result.map.entrySet().iterator().next().getKey());

        result = MAPPER.readValue(a2q("{'map':{'false':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Boolean.FALSE, result.map.entrySet().iterator().next().getKey());
    }

    public void testByteMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Byte, String>> type = new TypeReference<MapWrapper<Byte, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'13':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Byte.valueOf((byte) 13), result.map.entrySet().iterator().next().getKey());
    }

    public void testShortMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Short, String>> type = new TypeReference<MapWrapper<Short, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'13':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Short.valueOf((short) 13), result.map.entrySet().iterator().next().getKey());
    }

    public void testIntegerMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Integer, String>> type = new TypeReference<MapWrapper<Integer, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'-3':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Integer.valueOf(-3), result.map.entrySet().iterator().next().getKey());
    }

    public void testLongMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Long, String>> type = new TypeReference<MapWrapper<Long, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'42':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Long.valueOf(42), result.map.entrySet().iterator().next().getKey());
    }

    public void testFloatMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Float, String>> type = new TypeReference<MapWrapper<Float, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'3.5':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Float.valueOf(3.5f), result.map.entrySet().iterator().next().getKey());
    }

    public void testDoubleMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Double, String>> type = new TypeReference<MapWrapper<Double, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'0.25':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        Assert.assertEquals(Double.valueOf(0.25), result.map.entrySet().iterator().next().getKey());
    }

    /*
    /**********************************************************************
    /* Test methods, other
    /**********************************************************************
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
                a2q("{'map':{'"+encoded+"':'foobar'}}"),
                new TypeReference<MapWrapper<byte[], String>>() { });
        assertEquals(1, result.map.size());
        Map.Entry<byte[],String> entry = result.map.entrySet().iterator().next();
        assertEquals("foobar", entry.getValue());
        byte[] key = entry.getKey();
        Assert.assertArrayEquals(binary, key);
    }

    // [databind#2725]
    public void testEnumWithCreatorMapKeyDeserialization() throws Exception
    {
        final Map<TestEnum2725, String> input = Collections.singletonMap(TestEnum2725.FOO, "Hello");
        final String json = MAPPER.writeValueAsString(input);
        final Map<TestEnum2725, String> output = MAPPER.readValue(json,
                new TypeReference<Map<TestEnum2725, String>>() { });

        assertNotNull(output);
        assertEquals(1, output.size());
    }

    // [databind#2158]
    public void testDeserializeInvalidKey() throws Exception
    {
        try {
            MAPPER.readValue("{ \"\": 0 }", MAP_TYPE_2158);
            fail("Should no pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Value must be nonempty");
        }
    }

    // [databind#2158]
    public void testNormalizeKey() throws Exception
    {
        assertEquals(Collections.singletonMap(DummyDto2158.fromValue("foo"), 0),
                MAPPER.readValue("{ \"FOO\": 0 }", MAP_TYPE_2158));
    }
}
