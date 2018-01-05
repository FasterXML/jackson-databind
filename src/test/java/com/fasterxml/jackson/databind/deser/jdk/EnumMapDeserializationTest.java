package com.fasterxml.jackson.databind.deser.jdk;

import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

@SuppressWarnings("serial")
public class EnumMapDeserializationTest extends BaseMapTest
{
    enum TestEnum { JACKSON, RULES, OK; }

    enum TestEnumWithDefault {
        JACKSON, RULES,
        @JsonEnumDefaultValue
        OK; 
    }
    
    protected enum LowerCaseEnum {
        A, B, C;
        private LowerCaseEnum() { }
        @Override
        public String toString() { return name().toLowerCase(); }
    }

    static class MySimpleEnumMap extends EnumMap<TestEnum,String> { 
        public MySimpleEnumMap() {
            super(TestEnum.class);
        }
    }

    static class FromStringEnumMap extends EnumMap<TestEnum,String> { 
        @JsonCreator
        public FromStringEnumMap(String value) {
            super(TestEnum.class);
            put(TestEnum.JACKSON, value);
        }
    }

    static class FromDelegateEnumMap extends EnumMap<TestEnum,String> { 
        @JsonCreator
        public FromDelegateEnumMap(Map<Object,Object> stuff) {
            super(TestEnum.class);
            put(TestEnum.OK, String.valueOf(stuff));
        }
    }

    static class FromPropertiesEnumMap extends EnumMap<TestEnum,String> { 
        int a0, b0;

        @JsonCreator
        public FromPropertiesEnumMap(@JsonProperty("a") int a,
                @JsonProperty("b") int b) {
            super(TestEnum.class);
            a0 = a;
            b0 = b;
        }
    }

    /*
    /**********************************************************
    /* Test methods, basic
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = new ObjectMapper();

    public void testEnumMaps() throws Exception
    {
        EnumMap<TestEnum,String> value = MAPPER.readValue("{\"OK\":\"value\"}",
                new TypeReference<EnumMap<TestEnum,String>>() { });
        assertEquals("value", value.get(TestEnum.OK));
    }

    public void testToStringEnumMaps() throws Exception
    {
        // can't reuse global one due to reconfig
        ObjectReader r = MAPPER.reader()
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        EnumMap<LowerCaseEnum,String> value = r.forType(
            new TypeReference<EnumMap<LowerCaseEnum,String>>() { })
                .readValue("{\"a\":\"value\"}");
        assertEquals("value", value.get(LowerCaseEnum.A));
    }

    /*
    /**********************************************************
    /* Test methods: custom enum maps
    /**********************************************************
     */

    public void testCustomEnumMapWithDefaultCtor() throws Exception
    {
        MySimpleEnumMap map = MAPPER.readValue(aposToQuotes("{'RULES':'waves'}"),
                MySimpleEnumMap.class);   
        assertEquals(1, map.size());
        assertEquals("waves", map.get(TestEnum.RULES));
    }

    public void testCustomEnumMapFromString() throws Exception
    {
        FromStringEnumMap map = MAPPER.readValue(quote("kewl"), FromStringEnumMap.class);   
        assertEquals(1, map.size());
        assertEquals("kewl", map.get(TestEnum.JACKSON));
    }

    public void testCustomEnumMapWithDelegate() throws Exception
    {
        FromDelegateEnumMap map = MAPPER.readValue(aposToQuotes("{'foo':'bar'}"), FromDelegateEnumMap.class);   
        assertEquals(1, map.size());
        assertEquals("{foo=bar}", map.get(TestEnum.OK));
    }

    public void testCustomEnumMapFromProps() throws Exception
    {
        FromPropertiesEnumMap map = MAPPER.readValue(aposToQuotes(
                "{'a':13,'RULES':'jackson','b':-731,'OK':'yes'}"),
                FromPropertiesEnumMap.class);

        assertEquals(13, map.a0);
        assertEquals(-731, map.b0);

        assertEquals("jackson", map.get(TestEnum.RULES));
        assertEquals("yes", map.get(TestEnum.OK));
        assertEquals(2, map.size());
    }

    /*
    /**********************************************************
    /* Test methods: handling of invalid values
    /**********************************************************
     */

    // [databind#1859]
    public void testUnknownKeyAsDefault() throws Exception
    {
        // first, via EnumMap
        EnumMap<TestEnumWithDefault,String> value = MAPPER
                .readerFor(new TypeReference<EnumMap<TestEnumWithDefault,String>>() { })
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .readValue("{\"unknown\":\"value\"}");
        assertEquals(1, value.size());
        assertEquals("value", value.get(TestEnumWithDefault.OK));

        Map<TestEnumWithDefault,String> value2 = MAPPER
                .readerFor(new TypeReference<Map<TestEnumWithDefault,String>>() { })
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .readValue("{\"unknown\":\"value\"}");
        assertEquals(1, value2.size());
        assertEquals("value", value2.get(TestEnumWithDefault.OK));
    }

    // [databind#1859]
    public void testUnknownKeyAsNull() throws Exception
    {
        // first, via EnumMap
        EnumMap<TestEnumWithDefault,String> value = MAPPER
                .readerFor(new TypeReference<EnumMap<TestEnumWithDefault,String>>() { })
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .readValue("{\"unknown\":\"value\"}");
        assertEquals(0, value.size());

        // then regular Map
        Map<TestEnumWithDefault,String> value2 = MAPPER
                .readerFor(new TypeReference<Map<TestEnumWithDefault,String>>() { })
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .readValue("{\"unknown\":\"value\"}");
        // 04-Jan-2017, tatu: Not sure if this is weird or not, but since `null`s are typically
        //    ok for "regular" JDK Maps...
        assertEquals(1, value2.size());
        assertEquals("value", value2.get(null));
    }
}
