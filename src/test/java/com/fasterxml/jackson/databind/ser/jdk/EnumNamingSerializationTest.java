package com.fasterxml.jackson.databind.ser.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import java.util.EnumMap;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EnumNamingSerializationTest extends DatabindTestUtil {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
    */

    final ObjectMapper MAPPER = newJsonMapper();

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorA {
        CHOCOLATE_CHIPS,
        HOT_CHEETOS;

        @Override
        public String toString() {
            return "HOT_CHOCOLATE_CHEETOS_AND_CHIPS";
        }
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumSauceB {
        KETCH_UP,
        MAYO_NEZZ;
    }

    @EnumNaming(EnumNamingStrategy.class)
    static enum EnumSauceC {
        BARBEQ_UE,
        SRIRACHA_MAYO;
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorD {
        _PEANUT_BUTTER,
        PEANUT__BUTTER,
        PEANUT_BUTTER
    }

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    static enum EnumFlavorE {
        PEANUT_BUTTER,
        @JsonProperty("almond")
        ALMOND_BUTTER
    }

    static class EnumFlavorWrapperBean {
        public EnumSauceB sauce;

        @JsonCreator
        public EnumFlavorWrapperBean(@JsonProperty("sce") EnumSauceB sce) {
            this.sauce = sce;
        }
    }

    /*
    /**********************************************************
    /* Test
    /**********************************************************
    */

    @Test
    public void testEnumNamingShouldOverrideToStringFeatue() throws Exception {
        String resultStr = MAPPER.writer()
            .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(EnumFlavorA.CHOCOLATE_CHIPS);

        assertEquals(q("chocolateChips"), resultStr);
    }

    @Test
    public void testEnumNamingStrategyNotApplied() throws Exception {
        String resultString = MAPPER.writeValueAsString(EnumSauceC.SRIRACHA_MAYO);
        assertEquals(q("SRIRACHA_MAYO"), resultString);
    }

    @Test
    public void testEnumNamingStrategyStartingUnderscoreBecomesUpperCase() throws Exception {
        String flavor = MAPPER.writeValueAsString(EnumFlavorD._PEANUT_BUTTER);
        assertEquals(q("PeanutButter"), flavor);
    }

    @Test
    public void testEnumNamingStrategyNonPrefixContiguousUnderscoresBecomeOne() throws Exception {
        String flavor1 = MAPPER.writeValueAsString(EnumFlavorD.PEANUT__BUTTER);
        assertEquals(q("peanutButter"), flavor1);

        String flavor2 = MAPPER.writeValueAsString(EnumFlavorD.PEANUT_BUTTER);
        assertEquals(q("peanutButter"), flavor2);
    }

    @Test
    public void testEnumSet() throws Exception {
        final EnumSet<EnumSauceB> value = EnumSet.of(EnumSauceB.KETCH_UP);
        assertEquals("[\"ketchUp\"]", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testDesrEnumWithEnumMap() throws Exception {
        EnumMap<EnumSauceB, String> enums = new EnumMap<EnumSauceB, String>(EnumSauceB.class);
        enums.put(EnumSauceB.MAYO_NEZZ, "value");

        String str = MAPPER.writeValueAsString(enums);

        assertEquals(a2q("{'mayoNezz':'value'}"), str);
    }

    @Test
    public void testEnumNamingStrategyWithOverride() throws Exception {
        String almond = MAPPER.writeValueAsString(EnumFlavorE.ALMOND_BUTTER);
        assertEquals(q("almond"), almond);

        String peanut = MAPPER.writeValueAsString(EnumFlavorE.PEANUT_BUTTER);
        assertEquals(q("peanutButter"), peanut);
    }
}
