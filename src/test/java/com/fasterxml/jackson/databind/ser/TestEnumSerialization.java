package com.fasterxml.jackson.databind.ser;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestEnumSerialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper enums
    /**********************************************************
     */

    /**
     * Test enumeration for verifying Enum serialization functionality.
     */
    protected enum TestEnum {
        A, B, C;
        private TestEnum() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    /**
     * Alternative version that forces use of "toString-serializer".
     */
    @JsonSerialize(using=ToStringSerializer.class)
    protected enum AnnotatedTestEnum {
        A2, B2, C2;
        private AnnotatedTestEnum() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    protected enum EnumWithJsonValue {
        A("foo"), B("bar");
        private final String name;
        private EnumWithJsonValue(String n) {
            name = n;
        }
        @JsonValue
        @Override
        public String toString() { return name; }
    }
    
    protected static interface ToStringMixin {
        @Override
        @JsonValue public String toString();
    }

    protected enum SerializableEnum implements JsonSerializable
    {
        A, B, C;

        private SerializableEnum() { }
        
        @Override
        public void serializeWithType(JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
                throws IOException, JsonProcessingException
        {
            serialize(jgen, provider);
        }

        @Override
        public void serialize(JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException
        {
            jgen.writeString("foo");
        }
    }

    protected enum LowerCaseEnum {
        A, B, C;
        private LowerCaseEnum() { }
        @Override
        public String toString() { return name().toLowerCase(); }
    }

    static class MapBean {
        public Map<TestEnum,Integer> map = new HashMap<TestEnum,Integer>();
        
        public void add(TestEnum key, int value) {
            map.put(key, Integer.valueOf(value));
        }
    }

    // [JACKSON-757]
    static enum NOT_OK {
        V1("v1"); 
        protected String key;
        // any runtime-persistent annotation is fine
        NOT_OK(@JsonProperty String key) { this.key = key; }
    }

    static enum OK {
        V1("v1");
        protected String key;
        OK(String key) { this.key = key; }
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum.B);
        assertEquals("\"B\"", sw.toString());
    }

    public void testEnumSet() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        EnumSet<TestEnum> value = EnumSet.of(TestEnum.B);
        mapper.writeValue(sw, value);
        assertEquals("[\"B\"]", sw.toString());
    }

    /**
     * Whereas regular Enum serializer uses enum names, some users
     * prefer calling toString() instead. So let's verify that
     * this can be done using annotation for enum class.
     */
    public void testEnumUsingToString() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, AnnotatedTestEnum.C2);
        assertEquals("\"c2\"", sw.toString());
    }

    // Test [JACKSON-214]
    public void testSubclassedEnums() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("\"B\"", mapper.writeValueAsString(EnumWithSubClass.B));
    }

    // [JACKSON-193]
    public void testEnumsWithJsonValue() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("\"bar\"", mapper.writeValueAsString(EnumWithJsonValue.B));
    }

    // also, for [JACKSON-193], needs to work via mix-ins
    public void testEnumsWithJsonValueUsingMixin() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixInAnnotations(TestEnum.class, ToStringMixin.class);
        assertEquals("\"b\"", mapper.writeValueAsString(TestEnum.B));
    }

    /**
     * Test for ensuring that @JsonSerializable is used with Enum types as well
     * as with any other types.
     */
    public void testSerializableEnum() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("\"foo\"", mapper.writeValueAsString(SerializableEnum.A));
    }

    // [JACKSON-212]
    public void testToStringEnum() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
        assertEquals("\"b\"", mapper.writeValueAsString(LowerCaseEnum.B));
    }

    // [JACKSON-212]
    public void testToStringEnumWithEnumMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        EnumMap<LowerCaseEnum,String> m = new EnumMap<LowerCaseEnum,String>(LowerCaseEnum.class);
        m.put(LowerCaseEnum.C, "value");
        mapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
        assertEquals("{\"c\":\"value\"}", mapper.writeValueAsString(m));
    }

    // [JACKSON-576]
    public void testMapWithEnumKeys() throws Exception
    {
        MapBean bean = new MapBean();
        bean.add(TestEnum.B, 3);
        String json = new ObjectMapper().writeValueAsString(bean);
        assertEquals("{\"map\":{\"b\":3}}", json);
    }
    
    // [JACKSON-684]
    public void testAsIndex() throws Exception
    {
        // By default, serialize using name
        ObjectMapper mapper = new ObjectMapper();
        assertFalse(mapper.isEnabled(SerializationConfig.Feature.WRITE_ENUMS_USING_INDEX));
        assertEquals(quote("B"), mapper.writeValueAsString(TestEnum.B));

        // but we can change (dynamically, too!) it to be number-based
        mapper.enable(SerializationConfig.Feature.WRITE_ENUMS_USING_INDEX);
        assertEquals("1", mapper.writeValueAsString(TestEnum.B));
    }

    // [JACKSON-757]
    public void testAnnotationsOnEnumCtor() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValueAsString(OK.V1);
        mapper.writeValueAsString(NOT_OK.V1);
    }
}
