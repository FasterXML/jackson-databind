package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    }


    public void testEnumNamingToDefaultUnknownValue() throws Exception {
        EnumFlavorA result = MAPPER.reader()
            .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .readValue(q("0.12321"), EnumFlavorA.class);

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

}
