package com.fasterxml.jackson.databind.deser;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class TestEnumDeserialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes, enums
    /**********************************************************
     */

    enum TestEnum { JACKSON, RULES, OK; }

    /**
     * Alternative version that annotates which deserializer to use
     */
    @JsonDeserialize(using=DummySerializer.class)
    enum AnnotatedTestEnum {
        JACKSON, RULES, OK;
    }

    public static class DummySerializer extends StdDeserializer<Object>
    {
        public DummySerializer() { super(Object.class); }
        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        {
            return AnnotatedTestEnum.OK;
        }
    }

    protected enum EnumWithCreator {
        A, B;

        @JsonCreator
        public static EnumWithCreator fromEnum(String str) {
            if ("enumA".equals(str)) return A;
            if ("enumB".equals(str)) return B;
            return null;
        }
    }
    
    protected enum LowerCaseEnum {
        A, B, C;
        private LowerCaseEnum() { }
        @Override
        public String toString() { return name().toLowerCase(); }
    }

    // for [JACKSON-749]
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
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    protected final ObjectMapper mapper = new ObjectMapper();

    public void testSimple() throws Exception
    {
        // First "good" case with Strings
        String JSON = "\"OK\" \"RULES\"  null";
        // multiple main-level mappings, need explicit parser:
        JsonParser jp = mapper.getJsonFactory().createJsonParser(JSON);

        assertEquals(TestEnum.OK, mapper.readValue(jp, TestEnum.class));
        assertEquals(TestEnum.RULES, mapper.readValue(jp, TestEnum.class));

        /* should be ok; nulls are typeless; handled by mapper, not by
         * deserializer
         */
        assertNull(mapper.readValue(jp, TestEnum.class));

        // and no more content beyond that...
        assertFalse(jp.hasCurrentToken());

        /* Then alternative with index (0 means first entry)
         */
        assertEquals(TestEnum.JACKSON, mapper.readValue(" 0 ", TestEnum.class));

        /* Then error case: unrecognized value
         */
        try {
            /*Object result =*/ mapper.readValue("\"NO-SUCH-VALUE\"", TestEnum.class);
            fail("Expected an exception for bogus enum value...");
        } catch (JsonMappingException jex) {
            verifyException(jex, "value not one of declared");
        }
    }

    /**
     * Enums are considered complex if they have code (and hence sub-classes)... an
     * example is TimeUnit
     */
    public void testComplexEnum() throws Exception
    {
        String json = mapper.writeValueAsString(TimeUnit.HOURS);
        assertEquals(quote("HOURS"), json);
        TimeUnit result = mapper.readValue(json, TimeUnit.class);
        assertSame(TimeUnit.HOURS, result);
    }
    
    /**
     * Testing to see that annotation override works
     */
    public void testAnnotated() throws Exception
    {
        AnnotatedTestEnum e = mapper.readValue("\"JACKSON\"", AnnotatedTestEnum.class);
        /* dummy deser always returns value OK, independent of input;
         * only works if annotation is used
         */
        assertEquals(AnnotatedTestEnum.OK, e);
    }

    public void testEnumMaps() throws Exception
    {
        EnumMap<TestEnum,String> value = mapper.readValue("{\"OK\":\"value\"}",
                new TypeReference<EnumMap<TestEnum,String>>() { });
        assertEquals("value", value.get(TestEnum.OK));
    }
    
    // Test [JACKSON-214]
    public void testSubclassedEnums() throws Exception
    {
        EnumWithSubClass value = mapper.readValue("\"A\"", EnumWithSubClass.class);
        assertEquals(EnumWithSubClass.A, value);
    }

    // [JACKSON-193]
    public void testCreatorEnums() throws Exception
    {
        EnumWithCreator value = mapper.readValue("\"enumA\"", EnumWithCreator.class);
        assertEquals(EnumWithCreator.A, value);
    }
    
    // [JACKSON-212]
    public void testToStringEnums() throws Exception
    {
        // can't reuse global one due to reconfig
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        LowerCaseEnum value = m.readValue("\"c\"", LowerCaseEnum.class);
        assertEquals(LowerCaseEnum.C, value);
    }

    // [JACKSON-212]
    public void testToStringEnumMaps() throws Exception
    {
        // can't reuse global one due to reconfig
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        EnumMap<LowerCaseEnum,String> value = m.readValue("{\"a\":\"value\"}",
                new TypeReference<EnumMap<LowerCaseEnum,String>>() { });
        assertEquals("value", value.get(LowerCaseEnum.A));
    }

    // [JACKSON-412], disallow use of numbers
    public void testNumbersToEnums() throws Exception
    {
        // by default numbers are fine:
        assertFalse(mapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS));
        TestEnum value = mapper.readValue("1", TestEnum.class);
        assertSame(TestEnum.RULES, value);

        // but can also be changed to errors:
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        try {
            value = m.readValue("1", TestEnum.class);
            fail("Expected an error");
        } catch (JsonMappingException e) {
            verifyException(e, "Not allowed to deserialize Enum value out of JSON number");
        }
    }

    // [JACKSON-684], enums using index
    public void testEnumsWithIndex() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.enable(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        String json = m.writeValueAsString(TestEnum.RULES);
        assertEquals(String.valueOf(TestEnum.RULES.ordinal()), json);
        TestEnum result = m.readValue(json, TestEnum.class);
        assertSame(TestEnum.RULES, result);
    }
    
    // [JACKSON-749]: @JsonValue should be considered as well
    public void testEnumsWithJsonValue() throws Exception
    {
        // first, enum as is
        EnumWithJsonValue e = mapper.readValue(quote("foo"), EnumWithJsonValue.class);
        assertSame(EnumWithJsonValue.A, e);
        e = mapper.readValue(quote("bar"), EnumWithJsonValue.class);
        assertSame(EnumWithJsonValue.B, e);

        // then in EnumSet
        EnumSet<EnumWithJsonValue> set = mapper.readValue("[\"bar\"]",
                new TypeReference<EnumSet<EnumWithJsonValue>>() { });
        assertNotNull(set);
        assertEquals(1, set.size());
        assertTrue(set.contains(EnumWithJsonValue.B));
        assertFalse(set.contains(EnumWithJsonValue.A));

        // and finally EnumMap
        EnumMap<EnumWithJsonValue,Integer> map = mapper.readValue("{\"foo\":13}",
                new TypeReference<EnumMap<EnumWithJsonValue, Integer>>() { });
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(13), map.get(EnumWithJsonValue.A));
    }

    // [JACKSON-756], next three tests

    public void testEnumWithCreatorEnumMaps() throws Exception {
          EnumMap<EnumWithCreator,String> value = mapper.readValue("{\"enumA\":\"value\"}",
                  new TypeReference<EnumMap<EnumWithCreator,String>>() {});
          assertEquals("value", value.get(EnumWithCreator.A));
    }

    public void testEnumWithCreatorMaps() throws Exception {
          java.util.HashMap<EnumWithCreator,String> value = mapper.readValue("{\"enumA\":\"value\"}",
                  new TypeReference<java.util.HashMap<EnumWithCreator,String>>() {});
          assertEquals("value", value.get(EnumWithCreator.A));
    }

    public void testEnumWithCreatorEnumSets() throws Exception {
          EnumSet<EnumWithCreator> value = mapper.readValue("[\"enumA\"]",
                  new TypeReference<EnumSet<EnumWithCreator>>() {});
          assertTrue(value.contains(EnumWithCreator.A));
    }
}
