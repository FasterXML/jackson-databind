package tools.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#4409
public class EnumDeserDupName4409Test extends DatabindTestUtil
{
    // [databind#4409]: PropertyNamingStrategy should not affect Enums
    enum ColorMode {
        RGB,
        RGBa,
        RGBA
    }

    static class Bug {
        public ColorMode colorMode;
    }

    // for [databind#4409]
    enum ColorMode4409Snake {
        // Will become "rgb"
        RGB,
        // Will become "rgba"
        RGBa,
        // Will become "rgba" as well unless overriden, so:
        @JsonProperty("RGBA")
        RGBA
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    // [databind#4409]
    @Test
    public void testDupNameConflict4409() throws Exception
    {
        assertEquals(ColorMode4409Snake.RGBa,
                MAPPER.readValue(q("RGBa"), ColorMode4409Snake.class));

        assertEquals(q("RGBA"),
                MAPPER.writeValueAsString(ColorMode4409Snake.RGBA));
    }

    // [databind#4409]
    @Test
    public void testEnumAndPropertyNamingStrategy4409() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

        Bug bug = mapper.readValue("{ \"color_mode\": \"RGBa\"}", Bug.class);

        assertEquals(ColorMode.RGBa, bug.colorMode);
    }
}
