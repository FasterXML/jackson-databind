package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4938] Allow JsonCreator factory method to return `null`
public class JsonCreatorReturningNull4938Test
    extends DatabindTestUtil
{
    static class Localized3 {
        public final String en;
        public final String de;
        public final String fr;

        @JsonCreator
        public static Localized3 of(@JsonProperty("en") String en,
            @JsonProperty("de") String de, @JsonProperty("fr") String fr) {
            if (en == null && de == null && fr == null) {
                return null; // Explicitly return null when all arguments are null
            }
            return new Localized3(en, de, fr);
        }

        // This is how users would normally create instances, I think...?
        private Localized3(String en, String de, String fr) {
            this.en = en;
            this.de = de;
            this.fr = fr;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testDeserializeToNullWhenAllPropertiesAreNull()
            throws Exception
    {
        Localized3 result = MAPPER.readValue(
                "{ \"en\": null, \"de\": null, \"fr\": null }",
                Localized3.class);

        assertNull(result);
    }

    @Test
    void testDeserializeToNonNullWhenAnyPropertyIsNonNull()
            throws Exception
    {
        Localized3 result = MAPPER.readValue(
                "{ \"en\": \"Hello\", \"de\": null, \"fr\": null }",
                Localized3.class);

        assertNotNull(result);
        assertEquals("Hello", result.en);
    }

}
