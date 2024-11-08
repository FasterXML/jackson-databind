package com.fasterxml.jackson.databind.ser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4788] 2.18
public class EnumsToLCvsJsonProperty4788Test
    extends DatabindTestUtil
{

    public enum SauceA {
        @JsonProperty("Ketchup")
        KETCHUP,
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    public enum SauceB {
        @JsonProperty("PROPERTY_MAYO_NAIZZZZ")
        MAYO_NAIZZZZ
    }

    public enum SauceC {
        @JsonProperty("Is-A-Prop")
        IS_MY_NAME
    }

    ObjectMapper objectMapper = jsonMapperBuilder()
            .configure(EnumFeature.WRITE_ENUMS_TO_LOWERCASE, true)
            .build();

    @Test
    void shouldUseJsonPropertySimple()
            throws Exception
    {
        assertEquals(
                "\"Ketchup\"",
                objectMapper.writeValueAsString(SauceA.KETCHUP)
        );

    }

    @Test
    void shouldUseJsonPropertyOverEnumNaming()
        throws Exception
    {
        assertEquals(
                "\"PROPERTY_MAYO_NAIZZZZ\"",
                objectMapper.writeValueAsString(SauceB.MAYO_NAIZZZZ)
        );
    }

    @Test
    void shouldUseJsonPropertyOverToString()
        throws Exception
    {
        assertEquals(
                "\"Is-A-Prop\"",
                objectMapper.writer().with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                        .writeValueAsString(SauceC.IS_MY_NAME)
        );
    }
}
