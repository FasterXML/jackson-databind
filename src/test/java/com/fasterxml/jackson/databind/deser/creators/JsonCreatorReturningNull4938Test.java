package com.fasterxml.jackson.databind.deser.creators;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.*;
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

    static class Localized4 {
        public final String en;
        public final String de;
        public final String fr;

        @JsonCreator
        public static Localized4 of(@JsonProperty("en") String en,
                                    @JsonProperty("de") String de, @JsonProperty("fr") String fr) {
            if (en == null && de == null && fr == null) {
                return null; // Explicitly return null when all arguments are null
            }
            throw new IllegalStateException("Should not be called");
        }

        // This is how users would normally create instances, I think...?
        private Localized4(String en, String de, String fr) {
            this.en = en;
            this.de = de;
            this.fr = fr;
        }
    }

    // Test with AnySetter when creator returns null
    static class Localized5 {
        public final String en;
        public final String de;
        public final String fr;
        public final Map<String, Object> props = new HashMap<>();

        @JsonCreator
        public static Localized5 of(@JsonProperty("en") String en,
                                    @JsonProperty("de") String de, @JsonProperty("fr") String fr) {
            if (en == null && de == null && fr == null) {
                return null; // Explicitly return null when all arguments are null
            }
            throw new IllegalStateException("Should not be called");
        }

        // This is how users would normally create instances, I think...?
        private Localized5(String en, String de, String fr) {
            this.en = en;
            this.de = de;
            this.fr = fr;
        }

        @JsonAnySetter
        public void addProperty(String key, Object value) {
            props.put(key, value);
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

    @Test
    void testDeserializeReadingAfterCreatorProps()
            throws Exception
    {
        // Should all fail...
        ObjectReader enabled = MAPPER.readerFor(Localized4.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // ...with unknown properties in front
        _testDeserialize(enabled, "{ \"en\": null, \"de\": null, \"fr\": null, \"unknown\": null, \"unknown2\": \"hello\" }");
    }

    // Test to verify we are reading til the end of the OBJECT
    @Test
    void testDeserializeReadingUntilEndObject()
            throws Exception
    {
        // Should all fail...
        ObjectReader enabled = MAPPER.readerFor(Localized4.class)
                // We don't stop in the middle
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // This will trigger after...
                // ONLY AFTER we have read the whole object
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        // ...with unknown properties in front
        try {
            _testDeserialize(enabled, "{ \"en\": null, \"de\": null, \"fr\": null, \"unknown\": null, \"unknown2\": \"hello\" }" +
                    "!!!!!!!!!!!!BOOM!!!!!!!!!!!!!!");
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('!'");
        }
    }

    private void _testDeserialize(ObjectReader reader, String JSON)
        throws Exception
    {
        Localized4 bean = reader.readValue(JSON);
        assertNull(bean);
    }

    @Test
    void testJsonCreatorNullWithAnySetter()
            throws Exception
    {
        String JSON = "{ \"en\": null, \"de\": null, \"fr\": null, " +
                // These two properties are unknown
                "\"unknown\": null, \"unknown2\": \"hello\" }";

        MAPPER.readerFor(Localized5.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(JSON);
    }
}
