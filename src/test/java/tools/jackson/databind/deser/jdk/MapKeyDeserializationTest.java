package tools.jackson.databind.deser.jdk;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.Base64Variants;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidFormatException;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class MapKeyDeserializationTest
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
            return o instanceof DummyDto2158 dd && dd.value.equals(value);
        }

        @Override
        public int hashCode() { return Objects.hash(value); }

        @Override
        public String toString() { return String.format("DummyDto{value=%s}", value); }
    }

    private static final TypeReference<Map<DummyDto2158, Integer>> MAP_TYPE_2158 =
            new TypeReference<Map<DummyDto2158, Integer>>() {};

    // [databind#3143]
    static class Key3143Factories {
        protected String value;

        private Key3143Factories(String v, boolean bogus) {
            value = v;
        }

        // Specifically wrong one :)
        public static Key3143Factories cantUse() {
            throw new RuntimeException("Invalid factory");
        }

        @JsonCreator
        public static Key3143Factories create(String v) {
            return new Key3143Factories(v.toLowerCase(), true);
        }

        // Wrong one...
        public static Key3143Factories valueOf(String id) {
            return new Key3143Factories(id.toUpperCase(), false);
        }
    }

    // [databind#3143]: case of conflict
    static class Key3143FactoriesFail {
        @JsonCreator
        public static Key3143FactoriesFail create(String v) {
            throw new Error("Can't use");
        }

        @JsonCreator
        public static Key3143FactoriesFail valueOf(String id) {
            throw new Error("Can't use");
        }
    }

    // [databind#3143]
    static class Key3143Ctor {
        protected String value;

        public static Key3143Ctor valueOf(String id) {
            return new Key3143Ctor(id.toUpperCase());
        }

        @JsonCreator
        private Key3143Ctor(String v) {
            value = v;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, wrapper keys
    /**********************************************************************
     */

    final private ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testBooleanMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Boolean, String>> type = new TypeReference<MapWrapper<Boolean, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'true':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Boolean.TRUE, result.map.entrySet().iterator().next().getKey());

        result = MAPPER.readValue(a2q("{'map':{'false':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Boolean.FALSE, result.map.entrySet().iterator().next().getKey());
    }

    @Test
    public void testByteMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Byte, String>> type = new TypeReference<MapWrapper<Byte, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'13':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Byte.valueOf((byte) 13), result.map.entrySet().iterator().next().getKey());
    }

    @Test
    public void testShortMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Short, String>> type = new TypeReference<MapWrapper<Short, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'13':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Short.valueOf((short) 13), result.map.entrySet().iterator().next().getKey());
    }

    @Test
    public void testIntegerMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Integer, String>> type = new TypeReference<MapWrapper<Integer, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'-3':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Integer.valueOf(-3), result.map.entrySet().iterator().next().getKey());
    }

    @Test
    public void testLongMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Long, String>> type = new TypeReference<MapWrapper<Long, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'42':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Long.valueOf(42), result.map.entrySet().iterator().next().getKey());
    }

    @Test
    public void testFloatMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Float, String>> type = new TypeReference<MapWrapper<Float, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'3.5':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Float.valueOf(3.5f), result.map.entrySet().iterator().next().getKey());
    }

    @Test
    public void testDoubleMapKeyDeserialization() throws Exception
    {
        TypeReference<MapWrapper<Double, String>> type = new TypeReference<MapWrapper<Double, String>>() { };
        MapWrapper<?,?> result = MAPPER.readValue(a2q("{'map':{'0.25':'foobar'}}"), type);
        assertEquals(1, result.map.size());
        assertEquals(Double.valueOf(0.25), result.map.entrySet().iterator().next().getKey());
    }

    /*
    /**********************************************************************
    /* Test methods, other
    /**********************************************************************
     */

    @Test
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

    @Test
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
        assertArrayEquals(binary, key);
    }

    // [databind#2725]
    @Test
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
    @Test
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
    @Test
    public void testNormalizeKey() throws Exception
    {
        assertEquals(Collections.singletonMap(DummyDto2158.fromValue("foo"), 0),
                MAPPER.readValue("{ \"FOO\": 0 }", MAP_TYPE_2158));
    }

    // [databind#3143]
    @Test
    public void testKeyWithCtorAndCreator3143() throws Exception
    {
        // Use Constructor if annotated:
        Map<Key3143Ctor,Integer> map = MAPPER.readValue("{\"bar\":3}",
                new TypeReference<Map<Key3143Ctor,Integer>>() {} );
        assertEquals(1, map.size());
        assertEquals("bar", map.keySet().iterator().next().value);
    }

    // [databind#3143]
    @Test
    public void testKeyWith2Creators3143() throws Exception
    {
        // Select explicitly annotated factory method
        Map<Key3143Factories,Integer> map = MAPPER.readValue("{\"Foo\":3}",
                new TypeReference<Map<Key3143Factories,Integer>>() {} );
        assertEquals(1, map.size());
        assertEquals("foo", map.keySet().iterator().next().value);
    }

    // [databind#3143]
    @Test
    public void testKeyWithCreatorConflicts3143() throws Exception
    {
        try {
            MAPPER.readValue("{\"Foo\":3}",
                new TypeReference<Map<Key3143FactoriesFail,Integer>>() {} );
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Multiple");
            verifyException(e, "Creator factory methods");
        }
    }
}
