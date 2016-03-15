package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SuppressWarnings("serial")
public class TestEnumDeserialization
    extends BaseMapTest
{
    enum TestEnum { JACKSON, RULES, OK; }

    /**
     * Alternative version that annotates which deserializer to use
     */
    @JsonDeserialize(using=DummyDeserializer.class)
    enum AnnotatedTestEnum {
        JACKSON, RULES, OK;
    }

    public static class DummyDeserializer extends StdDeserializer<Object>
    {
        public DummyDeserializer() { super(Object.class); }
        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        {
            return AnnotatedTestEnum.OK;
        }
    }

    public static class LcEnumDeserializer extends StdDeserializer<TestEnum>
    {
        public LcEnumDeserializer() { super(TestEnum.class); }
        @Override
        public TestEnum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            return TestEnum.valueOf(jp.getText().toUpperCase());
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

    protected enum EnumWithBDCreator {
        E5, E8;

        @JsonCreator
        public static EnumWithBDCreator create(BigDecimal bd) {
            if (bd.longValue() == 5L) return E5;
            if (bd.longValue() == 8L) return E8;
            return null;
        }
    }
    
    protected enum LowerCaseEnum {
        A, B, C;
        private LowerCaseEnum() { }
        @Override
        public String toString() { return name().toLowerCase(); }
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

    static class ClassWithEnumMapKey {
        @JsonProperty Map<TestEnum, String> map;
    }

    protected enum TestEnumFor834
    {
        ENUM_A(1), ENUM_B(2), ENUM_C(3);
        
        private final int id;
        
        private TestEnumFor834(int id) {
            this.id = id;
        }
        
        @JsonCreator public static TestEnumFor834 fromId(int id) {
            for (TestEnumFor834 e: values()) {
                if (e.id == id) return e;
            }
            return null;
        }
    }

    // [Issue#324]: exception from creator method
    protected enum TestEnum324
    {
        A, B;
        
        @JsonCreator public static TestEnum324 creator(String arg) {
            throw new RuntimeException("Foobar!");
        }
    }

    // [databind#745]
    static class DelegatingDeserializers extends Deserializers.Base
    {
        @Override
        public JsonDeserializer<?> findEnumDeserializer(final Class<?> type, final DeserializationConfig config, final BeanDescription beanDesc) throws JsonMappingException {
            final Collection<AnnotatedMethod> factoryMethods = beanDesc.getFactoryMethods();
            if (factoryMethods != null) {
                for (AnnotatedMethod am : factoryMethods) {
                    final JsonCreator creator = am.getAnnotation(JsonCreator.class);
                    if (creator != null) {
                        return EnumDeserializer.deserializerForCreator(config, type, am);
                    }
                }
            }
            return null;
        }
    }

    // [databind#745]
    static class DelegatingDeserializersModule extends SimpleModule
    {
        @Override
        public void setupModule(final SetupContext context) {
            context.addDeserializers(new DelegatingDeserializers());
        }
    }

    // [databind#677]
    static enum EnumWithPropertyAnno {
        @JsonProperty("a")
        A,

        // For this value, force use of anonymous sub-class, to ensure things still work
        @JsonProperty("b")
        B {
            @Override
            public String toString() {
                return "bb";
            }
        }
        ;
    }

    // [databind#1161]
    enum Enum1161 {
        A, B, C;

        @Override
        public String toString() {
            return name().toLowerCase();
        };
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimple() throws Exception
    {
        // First "good" case with Strings
        String JSON = "\"OK\" \"RULES\"  null";
        // multiple main-level mappings, need explicit parser:
        JsonParser jp = MAPPER.getFactory().createParser(JSON);

        assertEquals(TestEnum.OK, MAPPER.readValue(jp, TestEnum.class));
        assertEquals(TestEnum.RULES, MAPPER.readValue(jp, TestEnum.class));

        /* should be ok; nulls are typeless; handled by mapper, not by
         * deserializer
         */
        assertNull(MAPPER.readValue(jp, TestEnum.class));

        // and no more content beyond that...
        assertFalse(jp.hasCurrentToken());

        /* Then alternative with index (0 means first entry)
         */
        assertEquals(TestEnum.JACKSON, MAPPER.readValue(" 0 ", TestEnum.class));

        /* Then error case: unrecognized value
         */
        try {
            /*Object result =*/ MAPPER.readValue("\"NO-SUCH-VALUE\"", TestEnum.class);
            fail("Expected an exception for bogus enum value...");
        } catch (JsonMappingException jex) {
            verifyException(jex, "value not one of declared");
        }
        jp.close();
    }

    /**
     * Enums are considered complex if they have code (and hence sub-classes)... an
     * example is TimeUnit
     */
    public void testComplexEnum() throws Exception
    {
        String json = MAPPER.writeValueAsString(TimeUnit.SECONDS);
        assertEquals(quote("SECONDS"), json);
        TimeUnit result = MAPPER.readValue(json, TimeUnit.class);
        assertSame(TimeUnit.SECONDS, result);
    }
    
    /**
     * Testing to see that annotation override works
     */
    public void testAnnotated() throws Exception
    {
        AnnotatedTestEnum e = MAPPER.readValue("\"JACKSON\"", AnnotatedTestEnum.class);
        /* dummy deser always returns value OK, independent of input;
         * only works if annotation is used
         */
        assertEquals(AnnotatedTestEnum.OK, e);
    }

    public void testEnumMaps() throws Exception
    {
        EnumMap<TestEnum,String> value = MAPPER.readValue("{\"OK\":\"value\"}",
                new TypeReference<EnumMap<TestEnum,String>>() { });
        assertEquals("value", value.get(TestEnum.OK));
    }

    public void testSubclassedEnums() throws Exception
    {
        EnumWithSubClass value = MAPPER.readValue("\"A\"", EnumWithSubClass.class);
        assertEquals(EnumWithSubClass.A, value);
    }

    public void testCreatorEnums() throws Exception {
        EnumWithCreator value = MAPPER.readValue("\"enumA\"", EnumWithCreator.class);
        assertEquals(EnumWithCreator.A, value);
    }

    public void testCreatorEnumsFromBigDecimal() throws Exception {
        EnumWithBDCreator value = MAPPER.readValue("\"8.0\"", EnumWithBDCreator.class);
        assertEquals(EnumWithBDCreator.E8, value);
    }

    public void testToStringEnums() throws Exception
    {
        // can't reuse global one due to reconfig
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        LowerCaseEnum value = m.readValue("\"c\"", LowerCaseEnum.class);
        assertEquals(LowerCaseEnum.C, value);
    }

    public void testToStringEnumMaps() throws Exception
    {
        // can't reuse global one due to reconfig
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        EnumMap<LowerCaseEnum,String> value = m.readValue("{\"a\":\"value\"}",
                new TypeReference<EnumMap<LowerCaseEnum,String>>() { });
        assertEquals("value", value.get(LowerCaseEnum.A));
    }

    public void testNumbersToEnums() throws Exception
    {
        // by default numbers are fine:
        assertFalse(MAPPER.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS));
        TestEnum value = MAPPER.readValue("1", TestEnum.class);
        assertSame(TestEnum.RULES, value);

        // but can also be changed to errors:
        ObjectReader r = MAPPER.readerFor(TestEnum.class)
                .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        try {
            value = r.readValue("1");
            fail("Expected an error");
        } catch (JsonMappingException e) {
            verifyException(e, "Not allowed to deserialize Enum value out of JSON number");
        }

        // and [databind#684]
        try {
            value = r.readValue(quote("1"));
            fail("Expected an error");
        } catch (JsonMappingException e) {
            verifyException(e, "Not allowed to deserialize Enum value out of JSON number");
        }
    }

    public void testEnumsWithIndex() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.enable(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        String json = m.writeValueAsString(TestEnum.RULES);
        assertEquals(String.valueOf(TestEnum.RULES.ordinal()), json);
        TestEnum result = m.readValue(json, TestEnum.class);
        assertSame(TestEnum.RULES, result);
    }

    public void testEnumsWithJsonValue() throws Exception
    {
        // first, enum as is
        EnumWithJsonValue e = MAPPER.readValue(quote("foo"), EnumWithJsonValue.class);
        assertSame(EnumWithJsonValue.A, e);
        e = MAPPER.readValue(quote("bar"), EnumWithJsonValue.class);
        assertSame(EnumWithJsonValue.B, e);

        // then in EnumSet
        EnumSet<EnumWithJsonValue> set = MAPPER.readValue("[\"bar\"]",
                new TypeReference<EnumSet<EnumWithJsonValue>>() { });
        assertNotNull(set);
        assertEquals(1, set.size());
        assertTrue(set.contains(EnumWithJsonValue.B));
        assertFalse(set.contains(EnumWithJsonValue.A));

        // and finally EnumMap
        EnumMap<EnumWithJsonValue,Integer> map = MAPPER.readValue("{\"foo\":13}",
                new TypeReference<EnumMap<EnumWithJsonValue, Integer>>() { });
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(13), map.get(EnumWithJsonValue.A));
    }

    public void testEnumWithCreatorEnumMaps() throws Exception {
          EnumMap<EnumWithCreator,String> value = MAPPER.readValue("{\"enumA\":\"value\"}",
                  new TypeReference<EnumMap<EnumWithCreator,String>>() {});
          assertEquals("value", value.get(EnumWithCreator.A));
    }

    public void testEnumWithCreatorMaps() throws Exception {
          java.util.HashMap<EnumWithCreator,String> value = MAPPER.readValue("{\"enumA\":\"value\"}",
                  new TypeReference<java.util.HashMap<EnumWithCreator,String>>() {});
          assertEquals("value", value.get(EnumWithCreator.A));
    }

    public void testEnumWithCreatorEnumSets() throws Exception {
          EnumSet<EnumWithCreator> value = MAPPER.readValue("[\"enumA\"]",
                  new TypeReference<EnumSet<EnumWithCreator>>() {});
          assertTrue(value.contains(EnumWithCreator.A));
    }

    // [JACKSON-810], ability to ignore unknown Enum values:

    public void testAllowUnknownEnumValuesReadAsNull() throws Exception
    {
        // can not use shared mapper when changing configs...
        ObjectReader reader = MAPPER.reader(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        assertNull(reader.forType(TestEnum.class).readValue("\"NO-SUCH-VALUE\""));
        assertNull(reader.forType(TestEnum.class).readValue(" 4343 "));
    }

    public void testAllowUnknownEnumValuesForEnumSets() throws Exception
    {
        ObjectReader reader = MAPPER.reader(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        EnumSet<TestEnum> result = reader.forType(new TypeReference<EnumSet<TestEnum>>() { })
                .readValue("[\"NO-SUCH-VALUE\"]");
        assertEquals(0, result.size());
    }
    
    public void testAllowUnknownEnumValuesAsMapKeysReadAsNull() throws Exception
    {
        ObjectReader reader = MAPPER.reader(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        ClassWithEnumMapKey result = reader.forType(ClassWithEnumMapKey.class)
                .readValue("{\"map\":{\"NO-SUCH-VALUE\":\"val\"}}");
        assertTrue(result.map.containsKey(null));
    }
    
    public void testDoNotAllowUnknownEnumValuesAsMapKeysWhenReadAsNullDisabled() throws Exception
    {
        assertFalse(MAPPER.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL));
         try {
             MAPPER.readValue("{\"map\":{\"NO-SUCH-VALUE\":\"val\"}}", ClassWithEnumMapKey.class);
             fail("Expected an exception for bogus enum value...");
         } catch (JsonMappingException jex) {
             verifyException(jex, "Can not construct Map key");
         }
    }

    public void testEnumsFromInts() throws Exception
    {
        Object ob = MAPPER.readValue("1 ", TestEnumFor834.class);
        assertEquals(TestEnumFor834.class, ob.getClass());
        assertSame(TestEnumFor834.ENUM_A, ob);
    }

    // [databind#141]: allow mapping of empty String into null
    public void testEnumsWithEmpty() throws Exception
    {
       final ObjectMapper mapper = new ObjectMapper();
       mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
       TestEnum result = mapper.readValue("\"\"", TestEnum.class);
       assertNull(result);
    }

    public void testGenericEnumDeserialization() throws Exception
    {
       final ObjectMapper mapper = new ObjectMapper();
       SimpleModule module = new SimpleModule("foobar");
       module.addDeserializer(Enum.class, new LcEnumDeserializer());
       mapper.registerModule(module);
       // not sure this is totally safe but...
       assertEquals(TestEnum.JACKSON, mapper.readValue(quote("jackson"), TestEnum.class));
    }
    
    // [databind#324]
    public void testExceptionFromCreator() throws Exception
    {
        try {
            /*TestEnum324 e =*/ MAPPER.readValue(quote("xyz"), TestEnum324.class);
            fail("Should throw exception");
        } catch (JsonMappingException e) {
            verifyException(e, "foobar");
        }
    }
    
    // [databind#381]
    public void testUnwrappedEnum() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        
        assertEquals(TestEnum.JACKSON, mapper.readValue("[" + quote("JACKSON") + "]", TestEnum.class));
    }
    
    public void testUnwrappedEnumException() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        try {
            assertEquals(TestEnum.JACKSON, mapper.readValue("[" + quote("JACKSON") + "]", TestEnum.class));
            fail("Exception was not thrown on deserializing a single array element of type enum");
        } catch (JsonMappingException exp) {
            //exception as thrown correctly
        }
    }

    // [databind#149]: 'stringified' indexes for enums
    public void testIndexAsString() throws Exception
    {
        // first, regular index ought to work fine
        TestEnum en = MAPPER.readValue("2", TestEnum.class);
        assertSame(TestEnum.values()[2], en);

        // but also with quoted Strings
        en = MAPPER.readValue(quote("1"), TestEnum.class);
        assertSame(TestEnum.values()[1], en);
    }

    // [databind#745]
    public void testDeserializerForCreatorWithEnumMaps() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new DelegatingDeserializersModule());
        EnumMap<EnumWithCreator,String> value = mapper.readValue("{\"enumA\":\"value\"}",
            new TypeReference<EnumMap<EnumWithCreator,String>>() {});
        assertEquals("value", value.get(EnumWithCreator.A));
    }

    public void testEnumWithJsonPropertyRename() throws Exception
    {
        String json = MAPPER.writeValueAsString(new EnumWithPropertyAnno[] {
                EnumWithPropertyAnno.B, EnumWithPropertyAnno.A
        });
        assertEquals("[\"b\",\"a\"]", json);

        // and while not really proper place, let's also verify deser while we're at it
        EnumWithPropertyAnno[] result = MAPPER.readValue(json, EnumWithPropertyAnno[].class);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertSame(EnumWithPropertyAnno.B, result[0]);
        assertSame(EnumWithPropertyAnno.A, result[1]);
    }

    // [databind#1161], unable to switch READ_ENUMS_USING_TO_STRING
    public void testDeserWithToString1161() throws Exception
    {
        Enum1161 result = MAPPER.readerFor(Enum1161.class)
                .readValue(quote("A"));
        assertSame(Enum1161.A, result);

        result = MAPPER.readerFor(Enum1161.class)
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .readValue(quote("a"));
        assertSame(Enum1161.A, result);

        // and once again, going back to defaults
        result = MAPPER.readerFor(Enum1161.class)
                .without(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .readValue(quote("A"));
        assertSame(Enum1161.A, result);
    }}
