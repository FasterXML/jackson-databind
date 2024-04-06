package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4409]: PropertyNamingStrategy should not affect to Enums
class EnumWithNamingStrategy4409Test
    extends DatabindTestUtil
{

    enum ColorMode {
        RGB,
        RGBa,
        RGBA
    }

    static class Bug {
        public ColorMode colorMode;
    }

    @Test
    public void testEnumAndPropertyNamingStrategy() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

        Bug bug = mapper.readValue("{ \"color_mode\": \"RGBa\"}", Bug.class);

        // fails
        assertEquals(ColorMode.RGBa, bug.colorMode);
    }
}