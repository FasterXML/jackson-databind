package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify [databind#5279]: Deserializing unwrapped value fails
 * if properties are not creator properties.
 */
public class UnwrappedWithNonCreatorProperty5279Test extends DatabindTestUtil
{
    // Wrapper class with @JsonUnwrapped
    static class Wrapper<T> {
        private T wrapped;

        @JsonCreator
        public Wrapper(T wrapped) {
            this.wrapped = wrapped;
        }

        @JsonUnwrapped
        public T getWrapped() {
            return wrapped;
        }
    }

    interface ValueLookup {
        String getSampleValue();
    }

    // This class uses default constructor without setter (non-creator properties)
    // and should work but currently fails in Jackson 3.x
    static class Sample implements ValueLookup {
        private String sampleValue;

        public Sample() {}

        @JsonProperty
        public String getSampleValue() {
            return sampleValue;
        }
    }

    // This class uses @JsonCreator and works correctly
    static class WorkingSample implements ValueLookup {
        private final String sampleValue;

        @JsonCreator
        public WorkingSample(@JsonProperty("sampleValue") String sampleValue) {
            this.sampleValue = sampleValue;
        }

        @JsonProperty
        public String getSampleValue() {
            return sampleValue;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Try with standard visibility to ensure fields are not auto-detected
    private final ObjectMapper MAPPER_NO_FIELD_ACCESS = jsonMapperBuilder()
            .changeDefaultVisibility(vc -> vc
                .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY))
            .build();

    @Test
    public void testUnwrappedWithCreatorProperty() throws Exception
    {
        String json = "{\"sampleValue\": \"SampleValue\"}";

        JavaType type = MAPPER.getTypeFactory()
                .constructParametricType(Wrapper.class, WorkingSample.class);

        @SuppressWarnings("unchecked")
        Wrapper<WorkingSample> result = MAPPER.readValue(json, type);

        assertNotNull(result);
        assertNotNull(result.getWrapped());
        assertEquals("SampleValue", result.getWrapped().getSampleValue());
    }

    @Test
    public void testUnwrappedWithNonCreatorProperty() throws Exception
    {
        String json = "{\"sampleValue\": \"SampleValue\"}";

        JavaType type = MAPPER.getTypeFactory()
                .constructParametricType(Wrapper.class, Sample.class);

        @SuppressWarnings("unchecked")
        Wrapper<Sample> result = MAPPER.readValue(json, type);

        assertNotNull(result);
        assertNotNull(result.getWrapped());
        assertEquals("SampleValue", result.getWrapped().getSampleValue());
    }

    @Test
    public void testUnwrappedWithNonCreatorPropertyNoFieldAccess() throws Exception
    {
        String json = "{\"sampleValue\": \"SampleValue\"}";

        JavaType type = MAPPER_NO_FIELD_ACCESS.getTypeFactory()
                .constructParametricType(Wrapper.class, Sample.class);

        @SuppressWarnings("unchecked")
        Wrapper<Sample> result = MAPPER_NO_FIELD_ACCESS.readValue(json, type);

        assertNotNull(result);
        assertNotNull(result.getWrapped());
        assertEquals("SampleValue", result.getWrapped().getSampleValue());
    }
}
