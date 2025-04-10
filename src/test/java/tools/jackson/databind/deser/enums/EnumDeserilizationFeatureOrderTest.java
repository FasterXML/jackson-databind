package tools.jackson.databind.deser.enums;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.EnumFeature;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static tools.jackson.databind.testutil.DatabindTestUtil.q;

public class EnumDeserilizationFeatureOrderTest
{
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

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
    public void testDeserUnknownUsingDefaultBeforeAsNull() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumFruit.class)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumFruit simpleEnumA = reader.readValue(q(""));

        assertEquals(EnumFruit.LEMON, simpleEnumA);
    }

    @Test
    public void testDeserUnknownUsingDefaultBeforeAsNullFlip() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumFruit.class)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumFruit simpleEnumA = reader.readValue(q(""));

        assertEquals(EnumFruit.LEMON, simpleEnumA);
    }

    @Test
    public void testDeserUnknownAsNull() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumFruit.class)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumFruit simpleEnumA = reader.readValue(q(""));

        assertEquals(null, simpleEnumA);
    }

    @Test
    public void testDeserWithAliasUsingDefault() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumLetter.class)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumLetter defaulted = reader.readValue(q("unknownValue"));

        assertEquals(EnumLetter.B, defaulted);
    }

    @Test
    public void testDeserWithAliasAsNull() throws Exception {
        ObjectReader reader = MAPPER
                .readerFor(EnumLetter.class)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumLetter defaulted = reader.readValue(q("unknownValue"));

        assertEquals(null, defaulted);
    }

    @Test
    public void testDeserUnknownEnumMapKeyUsingDefault() throws Exception {
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<EnumMap<EnumFruit, String>>() {})
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        EnumMap<EnumFruit, String> result = reader.readValue(JSON);

        assertTrue(result.containsKey(EnumFruit.LEMON));
        assertEquals("fresh!", result.get(EnumFruit.LEMON));
    }

    @Test
    public void testDeserUnknownEnumMapKeyAsNull() throws Exception {
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<EnumMap<EnumFruit, String>>() {})
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        EnumMap<EnumFruit, String> result = reader.readValue(JSON);

        // EnumMap cannot have null as key
        assertEquals(EnumMap.class, result.getClass());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDeserUnknownMapKeyUsingDefault() throws Exception {
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<Map<EnumFruit, String>>() {})
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        Map<EnumFruit, String> result = reader.readValue(JSON);

        assertTrue(result.containsKey(EnumFruit.LEMON));
        assertEquals("fresh!", result.get(EnumFruit.LEMON));
    }

    @Test
    public void testDeserUnknownMapKeyAsNull() throws Exception {
        // Arrange
        String JSON = a2q("{ 'UNknownWhatEver': 'fresh!'}");
        ObjectReader reader = MAPPER
                .readerFor(new TypeReference<Map<EnumFruit, String>>() {})
                .with(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        Map<EnumFruit, String> result = reader.readValue(JSON);

        assertFalse(result.containsKey(EnumFruit.LEMON));

        // 05-Mar-2023, tatu: Will NOT allow null keys because we produce `EnumMap`
        //    when key is Enum, even if not explicitly requested (in Jackson 3.0)
        //        assertTrue(result.containsKey(null));
        // assertEquals("fresh!", result.get(null));

        assertEquals(0, result.size());
    }
}
