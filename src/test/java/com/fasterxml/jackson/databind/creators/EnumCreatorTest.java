package com.fasterxml.jackson.databind.creators;

import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
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
        public JsonDeserializer<?> findEnumDeserializer(final Class<?> type, final DeserializationConfig config, final BeanDescription beanDesc) throws JsonMappingException {
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = new ObjectMapper();

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
            /*TestEnum324 e =*/ MAPPER.readValue(quote("xyz"), TestEnum324.class);
            fail("Should throw exception");
        } catch (JsonMappingException e) {
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

    // for [databind#960]
    public void testNoArgEnumCreator() throws Exception
    {
        MyEnum960 v = MAPPER.readValue("{\"value\":\"bogus\"}", MyEnum960.class);
        assertEquals(MyEnum960.VALUE, v);
    }
}
