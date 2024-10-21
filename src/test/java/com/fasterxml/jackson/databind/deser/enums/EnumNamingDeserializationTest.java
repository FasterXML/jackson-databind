package com.fasterxml.jackson.databind.deser.enums;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.EnumNaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

class EnumNamingDeserializationTest
{
    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectMapper MAPPER_CI = jsonMapperBuilder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    static enum EnumFlavorA {
        PEANUT_BUTTER,
        SALTED_CARAMEL,
        @JsonEnumDefaultValue
        VANILLA;
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    static enum EnumFlavorB {
        PEANUT_BUTTER,
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    static enum EnumSauceC {
        KETCH_UP,
        MAYO_NEZZ;
    }

    @EnumNaming(EnumNamingStrategy.class)
    static enum EnumSauceD {
        BARBEQ_UE,
        SRIRACHA_MAYO;
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    static enum EnumFlavorE {
        _PEANUT_BUTTER,
        PEANUT__BUTTER,
        PEANUT_BUTTER
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    static enum EnumFlavorF {
        PEANUT_BUTTER,
        @JsonProperty("caramel")
        SALTED_CARAMEL,
        @JsonAlias({"darkChocolate", "milkChocolate", "whiteChocolate"})
        CHOCOLATE;
    }

    static class EnumSauceWrapperBean {
        public EnumSauceC sauce;

        @JsonCreator
        public EnumSauceWrapperBean(@JsonProperty("sce") EnumSauceC sce) {
            this.sauce = sce;
        }
    }

    static class ClassWithEnumMapSauceKey {
        @JsonProperty
        Map<EnumSauceC, String> map;
    }

    static class ClassWithEnumMapSauceValue {
        @JsonProperty
        Map<String, EnumSauceC> map;
    }

    static enum BaseEnum {
        REAL_NAME
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    static enum MixInEnum {
        REAL_NAME
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
    */

    @Test
    void testEnumNamingWithLowerCamelCaseStrategy() throws Exception {
        EnumFlavorA result = MAPPER.readValue(q("saltedCaramel"), EnumFlavorA.class);
        assertEquals(EnumFlavorA.SALTED_CARAMEL, result);

        String resultString = MAPPER.writeValueAsString(result);
        assertEquals(q("saltedCaramel"), resultString);
    }

    @Test
    void testEnumNamingTranslateUnknownValueToDefault() throws Exception {
        EnumFlavorA result = MAPPER.readerFor(EnumFlavorA.class)
            .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .readValue(q("__salted_caramel"));

        assertEquals(EnumFlavorA.VANILLA, result);
    }

    @Test
    void testEnumNamingToDefaultNumber() throws Exception {
        EnumFlavorA result = MAPPER.readerFor(EnumFlavorA.class)
            .without(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .readValue(q("1"));

        assertEquals(EnumFlavorA.SALTED_CARAMEL, result);
    }

    @Test
    void testEnumNamingToDefaultEmptyString() throws Exception {
        EnumFlavorA result = MAPPER.readerFor(EnumFlavorA.class)
            .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .readValue(q(""));

        assertEquals(EnumFlavorA.VANILLA, result);
    }

    @Test
    void testOriginalEnamValueShouldNotBeFoundWithEnumNamingStrategy() throws Exception {
        EnumFlavorB result = MAPPER.readerFor(EnumFlavorB.class)
            .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .readValue(q("PEANUT_BUTTER"));

        assertNull(result);
    }

    @Test
    void testEnumNamingWithAliasOrProperty() throws Exception {
        EnumFlavorF pb = MAPPER.readValue(q("peanutButter"), EnumFlavorF.class);
        assertEquals(EnumFlavorF.PEANUT_BUTTER, pb);

        EnumFlavorF chocolate = MAPPER.readValue(q("chocolate"), EnumFlavorF.class);
        assertEquals(EnumFlavorF.CHOCOLATE, chocolate);

        EnumFlavorF milk = MAPPER.readValue(q("milkChocolate"), EnumFlavorF.class);
        assertEquals(EnumFlavorF.CHOCOLATE, milk);

        EnumFlavorF caramel = MAPPER.readValue(q("caramel"), EnumFlavorF.class);
        assertEquals(EnumFlavorF.SALTED_CARAMEL, caramel);

        EnumFlavorF badCaramel = MAPPER.readerFor(EnumFlavorF.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .readValue(q("saltedCaramel"));
        assertNull(badCaramel);
    }

    @Test
    void testEnumNamingStrategySymmetryReadThenWrite() throws Exception {
        EnumSauceC result = MAPPER.readValue(q("ketchUp"), EnumSauceC.class);
        assertEquals(EnumSauceC.KETCH_UP, result);

        String resultString = MAPPER.writeValueAsString(result);
        assertEquals(q("ketchUp"), resultString);
    }

    @Test
    void testEnumNamingStrategySymmetryWriteThenRead() throws Exception {
        String resultString = MAPPER.writeValueAsString(EnumSauceC.MAYO_NEZZ);

        EnumSauceC result = MAPPER.readValue(resultString, EnumSauceC.class);

        assertEquals(EnumSauceC.MAYO_NEZZ, result);
    }


    @Test
    void testReadWrapperValueWithEnumNamingStrategy() throws Exception {
        String json = "{\"sauce\": \"ketchUp\"}";

        EnumSauceWrapperBean wrapper = MAPPER.readValue(json, EnumSauceWrapperBean.class);

        assertEquals(EnumSauceC.KETCH_UP, wrapper.sauce);
    }

    @Test
    void testReadWrapperValueWithCaseInsensitiveEnumNamingStrategy() throws Exception {
        ObjectReader reader = MAPPER_CI
            .readerFor(EnumSauceWrapperBean.class);

        EnumSauceWrapperBean lowerCase = reader.readValue(a2q("{'sauce': 'ketchup'}"));
        assertEquals(EnumSauceC.KETCH_UP, lowerCase.sauce);

        EnumSauceWrapperBean upperCase = reader.readValue(a2q("{'sauce': 'KETCHUP'}"));
        assertEquals(EnumSauceC.KETCH_UP, upperCase.sauce);

        EnumSauceWrapperBean mixedCase = reader.readValue(a2q("{'sauce': 'kEtChUp'}"));
        assertEquals(EnumSauceC.KETCH_UP, mixedCase.sauce);
    }

    @Test
    void testWriteThenReadWrapperValueWithEnumNamingStrategy() throws Exception {
        EnumSauceWrapperBean sauceWrapper = new EnumSauceWrapperBean(EnumSauceC.MAYO_NEZZ);
        String json = MAPPER.writeValueAsString(sauceWrapper);

        EnumSauceWrapperBean wrapper = MAPPER.readValue(json, EnumSauceWrapperBean.class);

        assertEquals(EnumSauceC.MAYO_NEZZ, wrapper.sauce);
    }

    @Test
    void testEnumNamingStrategyInterfaceIsNotApplied() throws Exception {
        EnumSauceD sauce = MAPPER.readValue(q("SRIRACHA_MAYO"), EnumSauceD.class);
        assertEquals(EnumSauceD.SRIRACHA_MAYO, sauce);
    }

    @Test
    void testEnumNamingStrategyConflictWithUnderScores() throws Exception {
        EnumFlavorE flavor = MAPPER.readValue(q("peanutButter"), EnumFlavorE.class);
        assertEquals(EnumFlavorE.PEANUT__BUTTER, flavor);
    }

    @Test
    void testCaseSensensitiveEnumMapKey() throws Exception {
        String jsonStr = a2q("{'map':{'ketchUp':'val'}}");

        ClassWithEnumMapSauceKey result = MAPPER.readValue(jsonStr, ClassWithEnumMapSauceKey.class);

        assertEquals(1, result.map.size());
        assertEquals("val", result.map.get(EnumSauceC.KETCH_UP));
    }

    @Test
    void testAllowCaseInsensensitiveEnumMapKey() throws Exception {
        ObjectReader reader = MAPPER_CI
            .readerFor(ClassWithEnumMapSauceKey.class);

        ClassWithEnumMapSauceKey result = reader.readValue(a2q("{'map':{'KeTcHuP':'val'}}"));

        assertEquals(1, result.map.size());
        assertEquals("val", result.map.get(EnumSauceC.KETCH_UP));
    }

    @Test
    void testAllowCaseSensensitiveEnumMapValue() throws Exception {
        ObjectReader reader = MAPPER_CI
            .readerFor(ClassWithEnumMapSauceValue.class);

        ClassWithEnumMapSauceValue result = reader.readValue(
            a2q("{'map':{'lowerSauce':'ketchUp', 'upperSauce':'mayoNezz'}}"));

        assertEquals(2, result.map.size());
        assertEquals(EnumSauceC.KETCH_UP, result.map.get("lowerSauce"));
        assertEquals(EnumSauceC.MAYO_NEZZ, result.map.get("upperSauce"));
    }

    @Test
    void testAllowCaseInsensensitiveEnumMapValue() throws Exception {
        ObjectReader reader = MAPPER_CI
            .readerFor(ClassWithEnumMapSauceValue.class);

        ClassWithEnumMapSauceValue result = reader.readValue(
            a2q("{'map':{'lowerSauce':'ketchup', 'upperSauce':'MAYONEZZ'}}"));

        assertEquals(2, result.map.size());
        assertEquals(EnumSauceC.KETCH_UP, result.map.get("lowerSauce"));
        assertEquals(EnumSauceC.MAYO_NEZZ, result.map.get("upperSauce"));
    }

    @Test
    void testEnumMixInDeserializationTest() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(BaseEnum.class, MixInEnum.class)
                .build();

        // serialization
        String ser = mapper.writeValueAsString(BaseEnum.REAL_NAME);
        assertEquals(q("realName"), ser);

        // deserialization
        BaseEnum deser = mapper.readValue(q("realName"), BaseEnum.class);
        assertEquals(BaseEnum.REAL_NAME, deser);
    }

    @Test
    void testUseEnumMappingStrategySetInMapper() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enumNamingStrategy(EnumNamingStrategies.LowerCamelCaseStrategy.INSTANCE)
                .build();

        BaseEnum result = mapper.readValue(q("realName"), BaseEnum.class);
        assertEquals(BaseEnum.REAL_NAME, result);

        String resultString = mapper.writeValueAsString(result);
        assertEquals(q("realName"), resultString);
    }
}
