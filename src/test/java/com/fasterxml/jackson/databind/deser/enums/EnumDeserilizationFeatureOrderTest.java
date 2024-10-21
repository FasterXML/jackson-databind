package com.fasterxml.jackson.databind.deser.enums;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.q;

class EnumDeserilizationFeatureOrderTest
{
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    enum EnumFruit
    {
        APPLE,
        BANANA,
        @JsonEnumDefaultValue
        LEMON
    }

    enum EnumLetter
    {
        A,
        @JsonEnumDefaultValue
        @JsonAlias({"singleAlias"})
        B,
        @JsonAlias({"multipleAliases1", "multipleAliases2"})
        C
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */


    @Test
    void testDeserUnknownUsingDefaultBeforeAsNull() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumFruit.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumFruit simpleEnumA = reader.readValue(q(""));

        assertEquals(EnumFruit.LEMON, simpleEnumA);
    }

    @Test
    void testDeserUnknownUsingDefaultBeforeAsNullFlip() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumFruit.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumFruit simpleEnumA = reader.readValue(q(""));

        assertEquals(EnumFruit.LEMON, simpleEnumA);
    }

    @Test
    void testDeserUnknownAsNull() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumFruit.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumFruit simpleEnumA = reader.readValue(q(""));

        assertEquals(null, simpleEnumA);
    }

    @Test
    void testDeserWithAliasUsingDefault() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumLetter.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumLetter defaulted = reader.readValue(q("unknownValue"));

        assertEquals(EnumLetter.B, defaulted);
    }

    @Test
    void testDeserWithAliasAsNull() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumLetter.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumLetter defaulted = reader.readValue(q("unknownValue"));

        assertEquals(null, defaulted);
    }

    @Test
    void testDeserUnknownEnumMapKeyUsingDefault() throws Exception {
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<EnumMap<EnumFruit, String>>() {})
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumMap<EnumFruit, String> result = reader.readValue(JSON);

        assertTrue(result.containsKey(EnumFruit.LEMON));
        assertEquals("fresh!", result.get(EnumFruit.LEMON));
    }

    @Test
    void testDeserUnknownEnumMapKeyAsNull() throws Exception {
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<EnumMap<EnumFruit, String>>() {})
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumMap<EnumFruit, String> result = reader.readValue(JSON);

        // EnumMap cannot have null as key
        assertEquals(EnumMap.class, result.getClass());
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeserUnknownMapKeyUsingDefault() throws Exception {
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<Map<EnumFruit, String>>() {})
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        Map<EnumFruit, String> result = reader.readValue(JSON);

        assertTrue(result.containsKey(EnumFruit.LEMON));
        assertEquals("fresh!", result.get(EnumFruit.LEMON));
    }

    @Test
    void testDeserUnknownMapKeyAsNull() throws Exception {
        // Arrange
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<Map<EnumFruit, String>>() {})
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        Map<EnumFruit, String> result = reader.readValue(JSON);

        assertFalse(result.containsKey(EnumFruit.LEMON));
        assertTrue(result.containsKey(null));
        assertEquals("fresh!", result.get(null));
    }
}
