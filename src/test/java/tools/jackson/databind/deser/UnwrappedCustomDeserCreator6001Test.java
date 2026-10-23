package tools.jackson.databind.deser;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.NameTransformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#6001]: `@JsonUnwrapped` on a record (property-based-creator) component whose
// custom `ValueDeserializer` implements `unwrappingDeserializer(...)` but declares no
// property names. Before the fix, #650 buffered only properties recognized via
// `collectAllPropertyNamesTo(...)`, so such a custom unwrapper received an empty `{}`.
class UnwrappedCustomDeserCreator6001Test extends DatabindTestUtil
{
    record ConsentVersion(
            @JsonProperty String versionId,
            @JsonProperty String version,
            @JsonUnwrapped(prefix = "title")
            @JsonDeserialize(using = CapturingUnwrappedValueDeserializer.class)
            CapturingUnwrappedValue title,
            String scopeClassId) {}

    record CapturingUnwrappedValue(Map<String, Serializable> rawFields) {}

    static final class CapturingUnwrappedValueDeserializer
            extends ValueDeserializer<CapturingUnwrappedValue>
    {
        private final NameTransformer unwrapper;

        CapturingUnwrappedValueDeserializer() {
            this(null);
        }

        private CapturingUnwrappedValueDeserializer(NameTransformer unwrapper) {
            this.unwrapper = unwrapper;
        }

        @Override
        public CapturingUnwrappedValue deserialize(JsonParser p, DeserializationContext ctxt) {
            var node = ctxt.readTree(p);
            if (!(node instanceof ObjectNode objectNode)) {
                return new CapturingUnwrappedValue(Map.of());
            }
            var values = new LinkedHashMap<String, Serializable>();
            for (var entry : objectNode.properties()) {
                var key = (unwrapper != null) ? unwrapper.reverse(entry.getKey()) : entry.getKey();
                if (key == null || key.isBlank()) {
                    continue;
                }
                var value = ctxt.readTreeAsValue(entry.getValue(), Object.class);
                if (value instanceof Serializable serializableValue) {
                    values.put(key, serializableValue);
                }
            }
            return new CapturingUnwrappedValue(values);
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
            return this;
        }

        @Override
        public ValueDeserializer<CapturingUnwrappedValue> unwrappingDeserializer(
                DeserializationContext context, NameTransformer unwrapper) {
            return new CapturingUnwrappedValueDeserializer(unwrapper);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void unwrappedRecordCreatorPropertiesShouldReceiveFlattenedFields()
    {
        String json = """
                {
                  "versionId": "version-id",
                  "version": "1",
                  "titleEn": "title en",
                  "scopeClassId": "scope-class-id"
                }
                """;

        ConsentVersion result = MAPPER.readValue(json, ConsentVersion.class);

        assertTrue(result.title().rawFields().containsKey("En"),
                () -> "Expected title.rawFields() to contain key 'En', but got "
                        + result.title().rawFields());
        assertEquals("title en", result.title().rawFields().get("En"));
        assertEquals("version-id", result.versionId());
        assertEquals("scope-class-id", result.scopeClassId());
    }

    // Guard for [databind#650]: an opaque unwrapper routes all leftover unknowns to itself,
    // so FAIL_ON_UNKNOWN_PROPERTIES does NOT fire here (the unwrapper "owns" them) -- this is
    // the pre-#650 (3.0.x) behavior we are deliberately restoring for opaque unwrappers.
    @Test
    void opaqueUnwrapperAbsorbsUnknownsEvenWithFailOnUnknown()
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        String json = """
                {
                  "versionId": "version-id",
                  "version": "1",
                  "titleEn": "title en",
                  "totallyUnknown": "x"
                }
                """;

        ConsentVersion result = mapper.readValue(json, ConsentVersion.class);
        assertEquals("title en", result.title().rawFields().get("En"));
    }
}
