package com.fasterxml.jackson.databind.deser.enums;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

@SuppressWarnings("serial")
public class EnumDeserializationTest
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

    static enum EnumWithDefaultAnno {
        A, B,

        @JsonEnumDefaultValue
        OTHER;
    }

    static enum EnumWithDefaultAnnoAndConstructor {
        A, B,

        @JsonEnumDefaultValue
        OTHER;

        @JsonCreator public static EnumWithDefaultAnnoAndConstructor fromId(String value) {
            for (EnumWithDefaultAnnoAndConstructor e: values()) {
                if (e.name().toLowerCase().equals(value)) return e;
            }
            return null;
        }
    }

    static enum StrictEnumCreator {
        A, B;

        @JsonCreator public static StrictEnumCreator fromId(String value) {
            for (StrictEnumCreator e: values()) {
                if (e.name().toLowerCase().equals(value)) return e;
            }
            throw new IllegalArgumentException(value);
        }
    }

    public enum AnEnum {
        ZERO,
        ONE
    }

    public static class AnEnumDeserializer extends FromStringDeserializer<AnEnum> {

        public AnEnumDeserializer() {
            super(AnEnum.class);
        }

        @Override
        protected AnEnum _deserialize(String value, DeserializationContext ctxt) throws IOException {
            try {
                return AnEnum.valueOf(value);
            } catch (IllegalArgumentException e) {
                return (AnEnum) ctxt.handleWeirdStringValue(AnEnum.class, value,
                        "Undefined AnEnum code");
            }
        }
    }

    public static class AnEnumKeyDeserializer extends KeyDeserializer {

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            try {
                return AnEnum.valueOf(key);
            } catch (IllegalArgumentException e) {
                return ctxt.handleWeirdKey(AnEnum.class, key, "Undefined AnEnum code");
            }
        }
    }


    @JsonDeserialize(using = AnEnumDeserializer.class, keyUsing = AnEnumKeyDeserializer.class)
    public enum LanguageCodeMixin {
    }

    public static class EnumModule extends SimpleModule {
        @Override
        public void setupModule(SetupContext context) {
            context.setMixInAnnotations(AnEnum.class, LanguageCodeMixin.class);
        }

        public static ObjectMapper setupObjectMapper(ObjectMapper mapper) {
            final EnumModule module = new EnumModule();
            mapper.registerModule(module);
            return mapper;
        }
    }

    // for [databind#2164]
    public enum TestEnum2164 {
        A, B;

        @JsonCreator
        public static TestEnum2164 fromString(String input) {
            throw new IllegalArgumentException("2164");
        }
    }

    // for [databind#2309]
    static enum Enum2309 {
        NON_NULL("NON_NULL"),
        NULL(null),
        OTHER("OTHER")
        ;

        private String value;

        private Enum2309(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    // [databind#3006]
    enum Operation3006 {
        ONE(1L), TWO(2L), THREE(3L);

        private static final Map<Long, Operation3006> mapping = new HashMap<>();
        static {
            for (Operation3006 operation : Operation3006.values()) {
                mapping.put(operation.id, operation);
            }
        }

        final long id;

        Operation3006(final long id) {
            this.id = id;
        }

        @JsonCreator
        public static Operation3006 forValue(final String idStr) {
            Operation3006 candidate = mapping.get(Long.parseLong(idStr));
            if (candidate == null) {
                throw new IllegalArgumentException("Unable to find: " + idStr);
            }
            return candidate;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimple() throws Exception
    {
        // First "good" case with Strings
        String JSON = "\"OK\" \"RULES\"  null";
        // multiple main-level mappings, need explicit parser:
        JsonParser jp = MAPPER.createParser(JSON);

        assertEquals(TestEnum.OK, MAPPER.readValue(jp, TestEnum.class));
        assertEquals(TestEnum.RULES, MAPPER.readValue(jp, TestEnum.class));

        // should be ok; nulls are typeless; handled by mapper, not by deserializer
        assertNull(MAPPER.readValue(jp, TestEnum.class));

        // and no more content beyond that...
        assertFalse(jp.hasCurrentToken());

        // Then alternative with index (0 means first entry)
        assertEquals(TestEnum.JACKSON, MAPPER.readValue(" 0 ", TestEnum.class));

        // Then error case: unrecognized value
        try {
            /*Object result =*/ MAPPER.readValue("\"NO-SUCH-VALUE\"", TestEnum.class);
            fail("Expected an exception for bogus enum value...");
        } catch (MismatchedInputException jex) {
            verifyException(jex, "not one of the values accepted for Enum class");
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
        assertEquals(q("SECONDS"), json);
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

    public void testSubclassedEnums() throws Exception
    {
        EnumWithSubClass value = MAPPER.readValue("\"A\"", EnumWithSubClass.class);
        assertEquals(EnumWithSubClass.A, value);
    }

    public void testToStringEnums() throws Exception
    {
        // can't reuse global one due to reconfig
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        LowerCaseEnum value = m.readValue("\"c\"", LowerCaseEnum.class);
        assertEquals(LowerCaseEnum.C, value);
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
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
            verifyException(e, "not allowed to deserialize Enum value out of number: disable");
        }

        // and [databind#684]
        try {
            value = r.readValue(q("1"));
            fail("Expected an error");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
            // 26-Jan-2017, tatu: as per [databind#1505], should fail bit differently
            verifyException(e, "not one of the values accepted for Enum class");
        }
    }

    public void testEnumsWithIndex() throws Exception
    {
        String json = MAPPER.writer()
                .with(SerializationFeature.WRITE_ENUMS_USING_INDEX)
                .writeValueAsString(TestEnum.RULES);
        assertEquals(String.valueOf(TestEnum.RULES.ordinal()), json);
        TestEnum result = MAPPER.readValue(json, TestEnum.class);
        assertSame(TestEnum.RULES, result);
    }

    public void testEnumsWithJsonValue() throws Exception
    {
        // first, enum as is
        EnumWithJsonValue e = MAPPER.readValue(q("foo"), EnumWithJsonValue.class);
        assertSame(EnumWithJsonValue.A, e);
        e = MAPPER.readValue(q("bar"), EnumWithJsonValue.class);
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

    // Ability to ignore unknown Enum values:

    public void testAllowUnknownEnumValuesReadAsNull() throws Exception
    {
        // cannot use shared mapper when changing configs...
        ObjectReader reader = MAPPER.reader(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        assertNull(reader.forType(TestEnum.class).readValue("\"NO-SUCH-VALUE\""));
        assertNull(reader.forType(TestEnum.class).readValue(" 4343 "));
    }

    // Ability to ignore unknown Enum values:

    // [databind#1642]
    public void testAllowUnknownEnumValuesReadAsNullWithCreatorMethod() throws Exception
    {
        // cannot use shared mapper when changing configs...
        ObjectReader reader = MAPPER.reader(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        assertNull(reader.forType(StrictEnumCreator.class).readValue("\"NO-SUCH-VALUE\""));
        assertNull(reader.forType(StrictEnumCreator.class).readValue(" 4343 "));
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
         } catch (InvalidFormatException jex) {
             verifyException(jex, "Cannot deserialize Map key of type `com.fasterxml.jackson.databind.deser");
             verifyException(jex, "EnumDeserializationTest$TestEnum`");
         }
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
       assertEquals(TestEnum.JACKSON, mapper.readValue(q("jackson"), TestEnum.class));
    }

    // [databind#381]
    public void testUnwrappedEnum() throws Exception {
        assertEquals(TestEnum.JACKSON,
                MAPPER.readerFor(TestEnum.class)
                    .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                    .readValue("[" + q("JACKSON") + "]"));
    }

    public void testUnwrappedEnumException() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        mapper.disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        try {
            Object v = mapper.readValue("[" + q("JACKSON") + "]",
                    TestEnum.class);
            fail("Exception was not thrown on deserializing a single array element of type enum; instead got: "+v);
        } catch (MismatchedInputException exp) {
            //exception as thrown correctly
            verifyException(exp, "Cannot deserialize");
        }
    }

    // [databind#149]: 'stringified' indexes for enums
    public void testIndexAsString() throws Exception
    {
        // first, regular index ought to work fine
        TestEnum en = MAPPER.readValue("2", TestEnum.class);
        assertSame(TestEnum.values()[2], en);

        // but also with qd Strings
        en = MAPPER.readValue(q("1"), TestEnum.class);
        assertSame(TestEnum.values()[1], en);

        // [databind#1690]: unless prevented
        try {
            en = JsonMapper.builder()
                    .configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false)
                    .build()
                    .readerFor(TestEnum.class)
                    .readValue(q("1"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type");
            verifyException(e, "EnumDeserializationTest$TestEnum");
            verifyException(e, "value looks like quoted Enum index");
        }
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
                .readValue(q("A"));
        assertSame(Enum1161.A, result);

        result = MAPPER.readerFor(Enum1161.class)
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .readValue(q("a"));
        assertSame(Enum1161.A, result);

        // and once again, going back to defaults
        result = MAPPER.readerFor(Enum1161.class)
                .without(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .readValue(q("A"));
        assertSame(Enum1161.A, result);
    }

    public void testEnumWithDefaultAnnotation() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumWithDefaultAnno myEnum = mapper.readValue("\"foo\"", EnumWithDefaultAnno.class);
        assertSame(EnumWithDefaultAnno.OTHER, myEnum);
    }

    public void testEnumWithDefaultAnnotationUsingIndexInBound1() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumWithDefaultAnno myEnum = mapper.readValue("1", EnumWithDefaultAnno.class);
        assertSame(EnumWithDefaultAnno.B, myEnum);
    }

    public void testEnumWithDefaultAnnotationUsingIndexInBound2() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumWithDefaultAnno myEnum = mapper.readValue("2", EnumWithDefaultAnno.class);
        assertSame(EnumWithDefaultAnno.OTHER, myEnum);
    }

    public void testEnumWithDefaultAnnotationUsingIndexSameAsLength() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumWithDefaultAnno myEnum = mapper.readValue("3", EnumWithDefaultAnno.class);
        assertSame(EnumWithDefaultAnno.OTHER, myEnum);
    }

    public void testEnumWithDefaultAnnotationUsingIndexOutOfBound() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumWithDefaultAnno myEnum = mapper.readValue("4", EnumWithDefaultAnno.class);
        assertSame(EnumWithDefaultAnno.OTHER, myEnum);
    }

    public void testEnumWithDefaultAnnotationWithConstructor() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumWithDefaultAnnoAndConstructor myEnum = mapper.readValue("\"foo\"", EnumWithDefaultAnnoAndConstructor.class);
        assertNull("When using a constructor, the default value annotation shouldn't be used.", myEnum);
    }

    public void testExceptionFromCustomEnumKeyDeserializer() throws Exception {
        ObjectMapper mapper = newJsonMapper()
                .registerModule(new EnumModule());
        try {
            mapper.readValue("{\"TWO\": \"dumpling\"}",
                    new TypeReference<Map<AnEnum, String>>() {});
            fail("No exception");
        } catch (MismatchedInputException e) {
            assertTrue(e.getMessage().contains("Undefined AnEnum"));
        }
    }

    // [databind#2164]
    public void testWrapExceptions() throws Exception
    {
        // By default, wrap:
        try {
            MAPPER.readerFor(TestEnum2164.class)
                .readValue(q("B"));
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            verifyException(e, "2164");
        }

        // But can disable:
        try {
            MAPPER.readerFor(TestEnum2164.class)
                .without(DeserializationFeature.WRAP_EXCEPTIONS)
                .readValue(q("B"));
            fail("Should not pass");
        } catch (DatabindException e) {
            fail("Wrong exception, should not wrap, got: "+e);
        } catch (IllegalArgumentException e) {
            verifyException(e, "2164");
        }
    }

    // [databind#2309]
    public void testEnumToStringNull2309() throws Exception
    {
        Enum2309 value = MAPPER.readerFor(Enum2309.class)
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .readValue(q("NON_NULL"));
        assertEquals(Enum2309.NON_NULL, value);
    }

    // [databind#2873] -- take case-sensitivity into account for Enum-as-Map-keys too
    public void testEnumValuesCaseSensitivity() throws Exception {
        try {
            MAPPER.readValue("{\"map\":{\"JACkson\":\"val\"}}", ClassWithEnumMapKey.class);
            fail("Should not pass by default");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize Map key of type `com.fasterxml.jackson.databind.deser.");
            verifyException(e, "EnumDeserializationTest$TestEnum");
        }
    }

    // [databind#2873] -- take case-sensitivity into account for Enum-as-Map-keys too
    public void testAllowCaseInsensitiveEnumValues() throws Exception {
        ObjectMapper m = jsonMapperBuilder()
                .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
        ClassWithEnumMapKey result = m.readerFor(ClassWithEnumMapKey.class)
                .readValue("{\"map\":{\"JACkson\":\"val\"}}");
        assertEquals(1, result.map.size());
        assertEquals("val", result.map.get(TestEnum.JACKSON));
    }

    // [databind#3006]
    public void testIssue3006() throws Exception
    {
        assertEquals(Operation3006.ONE, MAPPER.readValue("1", Operation3006.class));
        assertEquals(Operation3006.ONE, MAPPER.readValue(q("1"), Operation3006.class));
        assertEquals(Operation3006.THREE, MAPPER.readValue("3", Operation3006.class));
        assertEquals(Operation3006.THREE, MAPPER.readValue(q("3"), Operation3006.class));
    }

    public void testEnumFeature_EnumIndexAsKey() throws Exception {
        ObjectReader reader = MAPPER.reader()
            .with(EnumFeature.READ_ENUM_KEYS_USING_INDEX);

        ClassWithEnumMapKey result = reader.readValue("{\"map\": {\"0\":\"I AM FOR REAL\"}}", ClassWithEnumMapKey.class);

        assertEquals(result.map.get(TestEnum.JACKSON), "I AM FOR REAL");
    }

    public void testEnumFeature_symmetric_to_writing() throws Exception {
        ClassWithEnumMapKey obj = new ClassWithEnumMapKey();
        Map<TestEnum, String> objMap = new HashMap<>();
        objMap.put(TestEnum.JACKSON, "I AM FOR REAL");
        obj.map = objMap;

        String deserObj = MAPPER.writer()
            .with(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX)
            .writeValueAsString(obj);

        ClassWithEnumMapKey result = MAPPER.reader()
            .with(EnumFeature.READ_ENUM_KEYS_USING_INDEX)
            .readValue(deserObj, ClassWithEnumMapKey.class);

        assertNotSame(obj, result);
        assertNotSame(obj.map, result.map);
        assertEquals(result.map.get(TestEnum.JACKSON), "I AM FOR REAL");
    }


    public void testEnumFeature_READ_ENUM_KEYS_USING_INDEX_isDisabledByDefault() {
        ObjectReader READER = MAPPER.reader();
        assertFalse(READER.isEnabled(EnumFeature.READ_ENUM_KEYS_USING_INDEX));
        assertFalse(READER.without(EnumFeature.READ_ENUM_KEYS_USING_INDEX)
            .isEnabled(EnumFeature.READ_ENUM_KEYS_USING_INDEX));
    }

}
