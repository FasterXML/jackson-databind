package com.fasterxml.jackson.databind.deser.creators;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class EnumCreatorTest extends BaseMapTest
{
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

    protected enum TestEnumFromInt
    {
        ENUM_A(1), ENUM_B(2), ENUM_C(3);

        private final int id;

        private TestEnumFromInt(int id) {
            this.id = id;
        }

        @JsonCreator public static TestEnumFromInt fromId(int id) {
            for (TestEnumFromInt e: values()) {
                if (e.id == id) return e;
            }
            return null;
        }
    }

    protected enum TestEnumFromString
    {
        ENUM_A, ENUM_B, ENUM_C;

        @JsonCreator public static TestEnumFromString fromId(String id) {
            return valueOf(id);
        }
    }

    static enum EnumWithPropertiesModeJsonCreator {
        TEST1,
        TEST2,
        TEST3;

        @JsonGetter("name")
        public String getName() {
            return name();
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static EnumWithPropertiesModeJsonCreator create(@JsonProperty("name") String name) {
            return EnumWithPropertiesModeJsonCreator.valueOf(name);
        }
    }

    static enum EnumWithDelegateModeJsonCreator {
        TEST1,
        TEST2,
        TEST3;

        @JsonGetter("name")
        public String getName() {
            return name();
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static EnumWithDelegateModeJsonCreator create(JsonNode json) {
            return EnumWithDelegateModeJsonCreator.valueOf(json.get("name").asText());
        }
    }

    // [databind#324]: exception from creator method
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
        public JsonDeserializer<?> findEnumDeserializer(final Class<?> type, final DeserializationConfig config, final BeanDescription beanDesc)
        {
            final Collection<AnnotatedMethod> factoryMethods = beanDesc.getFactoryMethods();
            if (factoryMethods != null) {
                for (AnnotatedMethod am : factoryMethods) {
                    final JsonCreator creator = am.getAnnotation(JsonCreator.class);
                    if (creator != null) {
                        return EnumDeserializer.deserializerForCreator(config, type, am, null, null);
                    }
                }
            }
            return null;
        }
    }

    // [databind#745]
    static class DelegatingDeserializersModule extends SimpleModule
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void setupModule(final SetupContext context) {
            context.addDeserializers(new DelegatingDeserializers());
        }
    }

    // [databind#929]: support multi-arg enum creator
    static enum Enum929
    {
        A, B, C;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static Enum929 forValues(@JsonProperty("id") int intProp,
                                 @JsonProperty("name") String name)
        {
            return Enum929.valueOf(name);
        }
    }

    static enum MyEnum960
    {
        VALUE, BOGUS;

        @JsonCreator
        public static MyEnum960 getInstance() {
            return VALUE;
        }
    }

    static class MyEnum960Wrapper {
        public MyEnum960 value;
    }

    static enum Enum1291 {

        V1("val1"),
        V2("val2"),
        V3("val3"),
        V4("val4"),
        V5("val5"),
        V6("val6");

        private final String name;

        Enum1291(String name) {
            this.name = name;
        }

        public static Enum1291 fromString(String name) {
            for (Enum1291 type : Enum1291.values()) {
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return Enum1291.valueOf(name.toUpperCase());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // [databind#3280]
    static enum Enum3280 {
        x("x"),
        y("y"),
        z("z");
        private final String value;
        Enum3280(String value) {
            this.value = value;
        }
        @JsonCreator
        public static Enum3280 getByValue(@JsonProperty("b") String value) {
            for (Enum3280 e : Enum3280.values()) {
                if (e.value.equals(value)) {
                    return e;
                }
            }
            return null;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = newJsonMapper();

    public void testCreatorEnums() throws Exception {
        EnumWithCreator value = MAPPER.readValue("\"enumA\"", EnumWithCreator.class);
        assertEquals(EnumWithCreator.A, value);
    }

    public void testCreatorEnumsFromBigDecimal() throws Exception {
        EnumWithBDCreator value = MAPPER.readValue("\"8.0\"", EnumWithBDCreator.class);
        assertEquals(EnumWithBDCreator.E8, value);
    }

    public void testEnumWithCreatorEnumMaps() throws Exception {
        EnumMap<EnumWithCreator,String> value = MAPPER.readValue("{\"enumA\":\"value\"}",
                new TypeReference<EnumMap<EnumWithCreator,String>>() {});
        assertEquals("value", value.get(EnumWithCreator.A));
    }

    public void testEnumWithCreatorMaps() throws Exception {
        HashMap<EnumWithCreator,String> value = MAPPER.readValue("{\"enumA\":\"value\"}",
                new TypeReference<java.util.HashMap<EnumWithCreator,String>>() {});
        assertEquals("value", value.get(EnumWithCreator.A));
    }

    public void testEnumWithCreatorEnumSets() throws Exception {
        EnumSet<EnumWithCreator> value = MAPPER.readValue("[\"enumA\"]",
                new TypeReference<EnumSet<EnumWithCreator>>() {});
        assertTrue(value.contains(EnumWithCreator.A));
    }

    public void testJsonCreatorPropertiesWithEnum() throws Exception
    {
        EnumWithPropertiesModeJsonCreator type1 = MAPPER.readValue("{\"name\":\"TEST1\", \"description\":\"TEST\"}", EnumWithPropertiesModeJsonCreator.class);
        assertSame(EnumWithPropertiesModeJsonCreator.TEST1, type1);

        EnumWithPropertiesModeJsonCreator type2 = MAPPER.readValue("{\"name\":\"TEST3\", \"description\":\"TEST\"}", EnumWithPropertiesModeJsonCreator.class);
        assertSame(EnumWithPropertiesModeJsonCreator.TEST3, type2);

    }

    public void testJsonCreatorDelagateWithEnum() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();

        EnumWithDelegateModeJsonCreator type1 = mapper.readValue("{\"name\":\"TEST1\", \"description\":\"TEST\"}", EnumWithDelegateModeJsonCreator.class);
        assertSame(EnumWithDelegateModeJsonCreator.TEST1, type1);

        EnumWithDelegateModeJsonCreator type2 = mapper.readValue("{\"name\":\"TEST3\", \"description\":\"TEST\"}", EnumWithDelegateModeJsonCreator.class);
        assertSame(EnumWithDelegateModeJsonCreator.TEST3, type2);

    }

    public void testEnumsFromInts() throws Exception
    {
        Object ob = MAPPER.readValue("1 ", TestEnumFromInt.class);
        assertEquals(TestEnumFromInt.class, ob.getClass());
        assertSame(TestEnumFromInt.ENUM_A, ob);
    }

    // [databind#324]
    public void testExceptionFromCreator() throws Exception
    {
        try {
            /*TestEnum324 e =*/ MAPPER.readValue(q("xyz"), TestEnum324.class);
            fail("Should throw exception");
        } catch (ValueInstantiationException e) {
            verifyException(e, "foobar");
        }
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

    // for [databind#929]
    public void testMultiArgEnumCreator() throws Exception
    {
        Enum929 v = MAPPER.readValue("{\"id\":3,\"name\":\"B\"}", Enum929.class);
        assertEquals(Enum929.B, v);
    }

    // for [databind#960]
    public void testNoArgEnumCreator() throws Exception
    {
        MyEnum960 v = MAPPER.readValue("{\"value\":\"bogus\"}", MyEnum960.class);
        assertEquals(MyEnum960.VALUE, v);
    }

    // for [databind#1291]
    public void testEnumCreators1291() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(Enum1291.V2);
        Enum1291 result = mapper.readValue(json, Enum1291.class);
        assertSame(Enum1291.V2, result);
    }

    // for [databind#1389]
    public void testMultiArgEnumInCollections() throws Exception
    {
        EnumSet<Enum929> valueEnumSet = MAPPER.readValue("[{\"id\":3,\"name\":\"B\"}, {\"id\":3,\"name\":\"A\"}]",
                new TypeReference<EnumSet<Enum929>>() {});
        assertEquals(2, valueEnumSet.size());
        assertTrue(valueEnumSet.contains(Enum929.A));
        assertTrue(valueEnumSet.contains(Enum929.B));
        List<Enum929> valueList = MAPPER.readValue("[{\"id\":3,\"name\":\"B\"}, {\"id\":3,\"name\":\"A\"}, {\"id\":3,\"name\":\"B\"}]",
                new TypeReference<List<Enum929>>() {});
        assertEquals(3, valueList.size());
        assertEquals(Enum929.B, valueList.get(2));
    }

    // for [databind#3280]
    public void testPropertyCreatorEnum3280() throws Exception
    {
        final ObjectReader r = MAPPER.readerFor(Enum3280.class);
        assertEquals(Enum3280.x, r.readValue("{\"b\":\"x\"}"));
        assertEquals(Enum3280.x, r.readValue("{\"a\":\"1\", \"b\":\"x\"}"));
        assertEquals(Enum3280.y, r.readValue("{\"b\":\"y\", \"a\":{}}"));
        assertEquals(Enum3280.y, r.readValue("{\"b\":\"y\", \"a\":{}}"));
        assertEquals(Enum3280.x, r.readValue("{\"a\":[], \"b\":\"x\"}"));
        assertEquals(Enum3280.x, r.readValue("{\"a\":{}, \"b\":\"x\"}"));
    }

    // for [databind#3655]
    public void testEnumsFromIntsUnwrapped() throws Exception
    {
        Object ob = MAPPER
                .readerFor(TestEnumFromInt.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[1]");
        assertEquals(TestEnumFromInt.class, ob.getClass());
        assertSame(TestEnumFromInt.ENUM_A, ob);
    }

    // for [databind#3655]
    public void testEnumsFromStringUnwrapped() throws Exception
    {
        Object ob = MAPPER
                .readerFor(TestEnumFromString.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[\"ENUM_A\"]");
        assertEquals(TestEnumFromString.class, ob.getClass());
        assertSame(TestEnumFromString.ENUM_A, ob);
    }
}
