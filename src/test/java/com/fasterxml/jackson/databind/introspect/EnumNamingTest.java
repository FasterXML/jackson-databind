package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.EnumNaming;

public class EnumNamingTest extends BaseMapTest {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
    */

    final ObjectMapper MAPPER = new ObjectMapper();

    /*
    /**********************************************************
    /* Test
    /**********************************************************
    */

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorA {
        PEANUT_BUTTER,
        SALTED_CARAMEL,
        @JsonEnumDefaultValue
        VANILLA;
    }

    public void testEnumNamingWithLowerCamelCaseStrategy() throws Exception {
        EnumFlavorA result = MAPPER.readValue(q("saltedCaramel"), EnumFlavorA.class);
        assertEquals(EnumFlavorA.SALTED_CARAMEL, result);

        String resultString = MAPPER.writeValueAsString(result);
    }

    public void testEnumNamingTranslateUnknownValueToDefault() throws Exception {
        EnumFlavorA result = MAPPER.reader()
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .readValue(q("__salted_caramel"), EnumFlavorA.class);

        assertEquals(EnumFlavorA.VANILLA, result);
    }

    public void testEnumNamingToDefaultNumber() throws Exception {
        EnumFlavorA result = MAPPER.reader()
                .without(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .readValue(q("1"), EnumFlavorA.class);

        assertEquals(EnumFlavorA.SALTED_CARAMEL, result);
    }

    public void testEnumNamingToDefaultEmptyString() throws Exception {
        EnumFlavorA result = MAPPER.reader()
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .readValue(q(""), EnumFlavorA.class);

        assertEquals(EnumFlavorA.VANILLA, result);
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorB {
        PEANUT_BUTTER,
    }

    public void testOriginalEnamValueShouldNotBeFoundWithEnumNamingStrategy() throws Exception {
        EnumFlavorB result = MAPPER.reader()
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .readValue(q("PEANUT_BUTTER"), EnumFlavorB.class);

        assertNull(result);
    }


    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorC {
        CHOCOLATE_CHIPS,
        HOT_CHEETOS;

        @Override
        public String toString() {
            return "HOT_CHOCOLATE_CHEETOS_AND_CHIPS";
        }
    }

    public void testEnumNamingShouldOverrideToStringFeatue() throws Exception {
        String resultStr = MAPPER.writer()
                .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .writeValueAsString(EnumFlavorC.CHOCOLATE_CHIPS);

        assertEquals(q("chocolateChips"), resultStr);
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumSauceA {
        KETCH_UP,
        MAYO_NEZZ;
    }

    public void testEnumNamingStrategySymmetryReadThenWrite() throws Exception {
        EnumSauceA result = MAPPER.readValue(q("ketchUp"), EnumSauceA.class);
        assertEquals(EnumSauceA.KETCH_UP, result);

        String resultString = MAPPER.writeValueAsString(result);
        assertEquals(q("ketchUp"), resultString);
    }

    public void testEnumNamingStrategySymmetryWriteThenRead() throws Exception {
        String resultString = MAPPER.writeValueAsString(EnumSauceA.MAYO_NEZZ);

        EnumSauceA result = MAPPER.readValue(resultString, EnumSauceA.class);

        assertEquals(EnumSauceA.MAYO_NEZZ, result);
    }


    static class EnumFlavorWrapperBean {
        public EnumSauceA sauce;

        @JsonCreator
        public EnumFlavorWrapperBean(@JsonProperty("sce") EnumSauceA sce) {
            this.sauce = sce;
        }
    }

    public void testReadWrapperValueWithEnumNamingStrategy() throws Exception {
        String json = "{\"sauce\": \"ketchUp\"}";

        EnumFlavorWrapperBean wrapper = MAPPER.readValue(json, EnumFlavorWrapperBean.class);

        assertEquals(EnumSauceA.KETCH_UP, wrapper.sauce);
    }

    public void testWriteThenReadWrapperValueWithEnumNamingStrategy() throws Exception {
        EnumFlavorWrapperBean sauceWrapper = new EnumFlavorWrapperBean(EnumSauceA.MAYO_NEZZ);
        String json = MAPPER.writeValueAsString(sauceWrapper);

        EnumFlavorWrapperBean wrapper = MAPPER.readValue(json, EnumFlavorWrapperBean.class);

        assertEquals(EnumSauceA.MAYO_NEZZ, wrapper.sauce);
    }


    @EnumNaming(EnumNamingStrategy.class)
    static enum EnumSauceB {
        BARBEQ_UE,
        SRIRACHA_MAYO;
    }

    public void testEnumNamingStrategyNotApplied() throws Exception {
        String resultString = MAPPER.writeValueAsString(EnumSauceB.SRIRACHA_MAYO);
        assertEquals(q("SRIRACHA_MAYO"), resultString);
    }

    public void testEnumNamingStrategyInterfaceIsNotApplied() throws Exception {
        EnumSauceB sauce = MAPPER.readValue(q("SRIRACHA_MAYO"), EnumSauceB.class);
        assertEquals(EnumSauceB.SRIRACHA_MAYO, sauce);
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorD {
        _PEANUT_BUTTER,
        PEANUT__BUTTER,
        PEANUT_BUTTER
    }

    public void testEnumNamingStrategyStartingUnderscoreBecomesUpperCase() throws Exception {
        String flavor = MAPPER.writeValueAsString(EnumFlavorD._PEANUT_BUTTER);
        assertEquals(q("PeanutButter"), flavor);
    }

    public void testEnumNamingStrategyNonPrefixContiguousUnderscoresBecomeOne() throws Exception {
        String flavor1 = MAPPER.writeValueAsString(EnumFlavorD.PEANUT__BUTTER);
        assertEquals(q("peanutButter"), flavor1);

        String flavor2 = MAPPER.writeValueAsString(EnumFlavorD.PEANUT_BUTTER);
        assertEquals(q("peanutButter"), flavor2);
    }

    public void testEnumNamingStrategyConflictWithUnderScores() throws Exception {
        EnumFlavorD flavor = MAPPER.readValue(q("peanutButter"), EnumFlavorD.class);
        assertEquals(EnumFlavorD.PEANUT__BUTTER, flavor);
    }

}
