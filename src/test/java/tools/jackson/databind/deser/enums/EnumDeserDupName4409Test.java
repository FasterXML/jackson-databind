package tools.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#4409
public class EnumDeserDupName4409Test extends DatabindTestUtil
{
    // for [databind#4409
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

    // for [databind#4409
    @Test
    public void dupNameConflict4409() throws Exception
    {
        assertEquals(ColorMode4409Snake.RGBa,
                MAPPER.readValue(q("RGBa"), ColorMode4409Snake.class));

        assertEquals(q("RGBA"),
                MAPPER.writeValueAsString(ColorMode4409Snake.RGBA));
    }
}
