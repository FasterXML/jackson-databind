package tools.jackson.databind.deser.creators;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4938] Allow JsonCreator factory method to return `null`
// [databind#5401] Do not replace null from JsonCreator factory method
public class JsonCreatorReturningNullTest
    extends DatabindTestUtil
{
    // [databind#4938]
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

    // [databind#5401]
    static class NonEmpty5401 {
        public NonEmptyString5401 nonEmpty;
    }

    static class NonEmptyString5401 {
        public final String value;

        @JsonCreator
        public static NonEmptyString5401 of(String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new NonEmptyString5401(value);
        }

        private NonEmptyString5401(String value) {
            this.value = value;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4938]
    @Test
    void testDeserializeToNullWhenAllPropertiesAreNull()
    {
        Localized3 result = MAPPER.readValue(
                "{ \"en\": null, \"de\": null, \"fr\": null }",
                Localized3.class);

        assertNull(result);
    }

    @Test
    void testDeserializeToNonNullWhenAnyPropertyIsNonNull()
    {
        Localized3 result = MAPPER.readValue(
                "{ \"en\": \"Hello\", \"de\": null, \"fr\": null }",
                Localized3.class);

        assertNotNull(result);
        assertEquals("Hello", result.en);
    }

    @Test
    void testDeserializeReadingAfterCreatorProps()
    {
        // Should all fail...
        ObjectReader enabled = MAPPER.readerFor(Localized4.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // ...with unknown properties in front
        try {
            enabled.readValue("{ \"unknown\": null, \"en\": null, \"de\": null, \"fr\": null, \"unknown2\": \"hello\" }");
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            // We fail with the FIRST unknown property
            verifyException(e, "Unrecognized property \"unknown\"");
        }
    }

    // Test to verify we are reading till the end of the OBJECT
    @Test
    void testDeserializeReadingUntilEndObject()
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
            enabled.readValue( "{ \"en\": null, \"de\": null, \"fr\": null, \"unknown\": null, \"unknown2\": \"hello\" }" +
                    "!!!!!!!!!!!!BOOM!!!!!!!!!!!!!!");
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Unexpected character ('!'");
        }
    }

    @Test
    void testJsonCreatorNullWithAnySetter()
    {
        String JSON = "{ \"en\": null, \"de\": null, \"fr\": null, " +
                // These two properties are unknown
                "\"unknown\": null, \"unknown2\": \"hello\" }";

        MAPPER.readerFor(Localized5.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(JSON);
    }

    // [databind#5401]
    @Test
    void deserializeFieldToNullIfDelegatingCreatorReturnsNull()
    {
        NonEmpty5401 result = MAPPER.readValue(
                "{ \"nonEmpty\": \"\" }",
                NonEmpty5401.class);

        assertNotNull(result);
        assertNull(result.nonEmpty);
    }
}
