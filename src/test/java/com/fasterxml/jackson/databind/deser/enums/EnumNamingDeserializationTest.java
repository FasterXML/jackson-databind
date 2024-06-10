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

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class EnumNamingDeserializationTest
{
    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectMapper MAPPER_CI = jsonMapperBuilder()
            .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorA {
        PEANUT_BUTTER,
        SALTED_CARAMEL,
        @JsonEnumDefaultValue
        VANILLA;
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorB {
        PEANUT_BUTTER,
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumSauceC {
        KETCH_UP,
        MAYO_NEZZ;
    }

    @EnumNaming(EnumNamingStrategy.class)
    static enum EnumSauceD {
        BARBEQ_UE,
        SRIRACHA_MAYO;
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorE {
        _PEANUT_BUTTER,
        PEANUT__BUTTER,
        PEANUT_BUTTER
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
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

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum MixInEnum {
        REAL_NAME
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
    */

    @Test
    public void testEnumNamingWithLowerCamelCaseStrategy() throws Exception {
        EnumFlavorA result = MAPPER.readValue(q("saltedCaramel"), EnumFlavorA.class);
        assertEquals(EnumFlavorA.SALTED_CARAMEL, result);

        String resultString = MAPPER.writeValueAsString(result);
        assertEquals(q("saltedCaramel"), resultString);
    }

    @Test
    public void testEnumNamingTranslateUnknownValueToDefault() throws Exception {
        EnumFlavorA result = MAPPER.readerFor(EnumFlavorA.class)
            .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .readValue(q("__salted_caramel"));

        assertEquals(EnumFlavorA.VANILLA, result);
    }

    @Test
    public void testEnumNamingToDefaultNumber() throws Exception {
        EnumFlavorA result = MAPPER.readerFor(EnumFlavorA.class)
            .without(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .readValue(q("1"));

        assertEquals(EnumFlavorA.SALTED_CARAMEL, result);
    }

    @Test
    public void testEnumNamingToDefaultEmptyString() throws Exception {
        EnumFlavorA result = MAPPER.readerFor(EnumFlavorA.class)
            .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .readValue(q(""));

        assertEquals(EnumFlavorA.VANILLA, result);
    }

    @Test
    public void testOriginalEnamValueShouldNotBeFoundWithEnumNamingStrategy() throws Exception {
        EnumFlavorB result = MAPPER.readerFor(EnumFlavorB.class)
            .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .readValue(q("PEANUT_BUTTER"));

        assertNull(result);
    }

    @Test
    public void testEnumNamingWithAliasOrProperty() throws Exception {
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
    public void testEnumNamingStrategySymmetryReadThenWrite() throws Exception {
        EnumSauceC result = MAPPER.readValue(q("ketchUp"), EnumSauceC.class);
        assertEquals(EnumSauceC.KETCH_UP, result);

        String resultString = MAPPER.writeValueAsString(result);
        assertEquals(q("ketchUp"), resultString);
    }

    @Test
    public void testEnumNamingStrategySymmetryWriteThenRead() throws Exception {
        String resultString = MAPPER.writeValueAsString(EnumSauceC.MAYO_NEZZ);

        EnumSauceC result = MAPPER.readValue(resultString, EnumSauceC.class);

        assertEquals(EnumSauceC.MAYO_NEZZ, result);
    }


    @Test
    public void testReadWrapperValueWithEnumNamingStrategy() throws Exception {
        String json = "{\"sauce\": \"ketchUp\"}";

        EnumSauceWrapperBean wrapper = MAPPER.readValue(json, EnumSauceWrapperBean.class);

        assertEquals(EnumSauceC.KETCH_UP, wrapper.sauce);
    }

    @Test
    public void testReadWrapperValueWithCaseInsensitiveEnumNamingStrategy() throws Exception {
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
    public void testWriteThenReadWrapperValueWithEnumNamingStrategy() throws Exception {
        EnumSauceWrapperBean sauceWrapper = new EnumSauceWrapperBean(EnumSauceC.MAYO_NEZZ);
        String json = MAPPER.writeValueAsString(sauceWrapper);

        EnumSauceWrapperBean wrapper = MAPPER.readValue(json, EnumSauceWrapperBean.class);

        assertEquals(EnumSauceC.MAYO_NEZZ, wrapper.sauce);
    }

    @Test
    public void testEnumNamingStrategyInterfaceIsNotApplied() throws Exception {
        EnumSauceD sauce = MAPPER.readValue(q("SRIRACHA_MAYO"), EnumSauceD.class);
        assertEquals(EnumSauceD.SRIRACHA_MAYO, sauce);
    }

    @Test
    public void testEnumNamingStrategyConflictWithUnderScores() throws Exception {
        EnumFlavorE flavor = MAPPER.readValue(q("peanutButter"), EnumFlavorE.class);
        assertEquals(EnumFlavorE.PEANUT__BUTTER, flavor);
    }

    @Test
    public void testCaseSensensitiveEnumMapKey() throws Exception {
        String jsonStr = a2q("{'map':{'ketchUp':'val'}}");

        ClassWithEnumMapSauceKey result = MAPPER.readValue(jsonStr, ClassWithEnumMapSauceKey.class);

        assertEquals(1, result.map.size());
        assertEquals("val", result.map.get(EnumSauceC.KETCH_UP));
    }

    @Test
    public void testAllowCaseInsensensitiveEnumMapKey() throws Exception {
        ObjectReader reader = MAPPER_CI
            .readerFor(ClassWithEnumMapSauceKey.class);

        ClassWithEnumMapSauceKey result = reader.readValue(a2q("{'map':{'KeTcHuP':'val'}}"));

        assertEquals(1, result.map.size());
        assertEquals("val", result.map.get(EnumSauceC.KETCH_UP));
    }

    @Test
    public void testAllowCaseSensensitiveEnumMapValue() throws Exception {
        ObjectReader reader = MAPPER_CI
            .readerFor(ClassWithEnumMapSauceValue.class);

        ClassWithEnumMapSauceValue result = reader.readValue(
            a2q("{'map':{'lowerSauce':'ketchUp', 'upperSauce':'mayoNezz'}}"));

        assertEquals(2, result.map.size());
        assertEquals(EnumSauceC.KETCH_UP, result.map.get("lowerSauce"));
        assertEquals(EnumSauceC.MAYO_NEZZ, result.map.get("upperSauce"));
    }

    @Test
    public void testAllowCaseInsensensitiveEnumMapValue() throws Exception {
        ObjectReader reader = MAPPER_CI
            .readerFor(ClassWithEnumMapSauceValue.class);

        ClassWithEnumMapSauceValue result = reader.readValue(
            a2q("{'map':{'lowerSauce':'ketchup', 'upperSauce':'MAYONEZZ'}}"));

        assertEquals(2, result.map.size());
        assertEquals(EnumSauceC.KETCH_UP, result.map.get("lowerSauce"));
        assertEquals(EnumSauceC.MAYO_NEZZ, result.map.get("upperSauce"));
    }

    @Test
    public void testEnumMixInDeserializationTest() throws Exception {
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
}
