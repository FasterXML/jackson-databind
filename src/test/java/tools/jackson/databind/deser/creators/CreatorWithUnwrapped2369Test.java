package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#2369] NPE with @JsonCreator factory method and @JsonUnwrapped when
// unknown properties are present and FAIL_ON_UNKNOWN_PROPERTIES is disabled
public class CreatorWithUnwrapped2369Test
    extends DatabindTestUtil
{
    static class Example2369 {
        public String first;
        @JsonUnwrapped
        public Second2369 second;
    }

    static class Second2369 {
        public String field;

        @JsonCreator
        public static Second2369 factory(@JsonProperty("field") String field) {
            if (field != null) {
                Second2369 result = new Second2369();
                result.field = field;
                return result;
            }
            return null;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    // [databind#2369] Should not throw NPE when unknown properties present
    @Test
    void testCreatorWithUnwrappedAndUnknownProperty() throws Exception
    {
        // JSON has unknown property "third"; factory returns null for Second2369
        // because "field" is not present. This triggered NPE before fix.
        Example2369 result = MAPPER.readValue(
                "{\"first\": \"wow\", \"third\": \"WOW, NPE!\"}",
                Example2369.class);
        assertNotNull(result);
        assertEquals("wow", result.first);
        assertNull(result.second);
    }

    // [databind#2369] Baseline: no unknown properties, should work fine
    @Test
    void testCreatorWithUnwrappedNoUnknownProperty() throws Exception
    {
        Example2369 result = MAPPER.readValue(
                "{\"first\": \"wow\"}",
                Example2369.class);
        assertNotNull(result);
        assertEquals("wow", result.first);
        assertNull(result.second);
    }
}
