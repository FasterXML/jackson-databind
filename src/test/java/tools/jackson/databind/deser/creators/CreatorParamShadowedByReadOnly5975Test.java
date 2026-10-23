package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5975]: A creator parameter whose implicit (parameter-name) name
// matches the explicit name of a sibling READ_ONLY accessor must still receive
// its value during deserialization; the READ_ONLY ignoral collected for the
// sibling must not shadow the creator parameter.
class CreatorParamShadowedByReadOnly5975Test extends DatabindTestUtil
{
    // READ_ONLY public field whose explicit name matches the creator param's
    // implicit name.
    static class FieldBean {
        @JsonProperty(value = "uri", access = JsonProperty.Access.READ_ONLY)
        public final String redactedUri;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public FieldBean(String uri) {
            this.redactedUri = uri;
        }
    }

    // READ_ONLY getter (instead of public field) whose explicit name matches
    // the creator param's implicit name.
    static class GetterBean {
        private final String uri;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public GetterBean(String uri) {
            this.uri = uri;
        }

        @JsonProperty(value = "uri", access = JsonProperty.Access.READ_ONLY)
        public String getRedactedUri() {
            return uri;
        }
    }

    // Original issue's shape: WRITE_ONLY annotation on the creator parameter
    // alongside the READ_ONLY field.
    static class WriteOnlyCtorBean {
        @JsonProperty(value = "uri", access = JsonProperty.Access.READ_ONLY)
        public final String redactedUri;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public WriteOnlyCtorBean(@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String uri) {
            this.redactedUri = uri;
        }
    }

    // Round-trip: the READ_ONLY field is serialized (with redaction applied
    // by the constructor) and deserialization populates the creator parameter
    // from the same JSON name.
    static class RedactingRoundTripBean {
        @JsonProperty(value = "uri", access = JsonProperty.Access.READ_ONLY)
        public final String redactedUri;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RedactingRoundTripBean(String uri) {
            this.redactedUri = (uri == null) ? null : uri.replace("password", "***");
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    public void testReadOnlyFieldDoesNotShadowCreator() throws Exception {
        FieldBean read = MAPPER.readValue("{\"uri\":\"foo\"}", FieldBean.class);
        assertEquals("foo", read.redactedUri);
    }

    @Test
    public void testReadOnlyGetterDoesNotShadowCreator() throws Exception {
        GetterBean read = MAPPER.readValue("{\"uri\":\"foo\"}", GetterBean.class);
        assertEquals("foo", read.uri);
    }

    @Test
    public void testWriteOnlyCtorParamWithReadOnlyField() throws Exception {
        WriteOnlyCtorBean read = MAPPER.readValue("{\"uri\":\"foo\"}", WriteOnlyCtorBean.class);
        assertEquals("foo", read.redactedUri);
    }

    @Test
    public void testRoundTripPreservesRedaction() throws Exception {
        RedactingRoundTripBean original = new RedactingRoundTripBean("my-password");
        String json = MAPPER.writeValueAsString(original);
        assertEquals("{\"uri\":\"my-***\"}", json);
        RedactingRoundTripBean parsed = MAPPER.readValue(json, RedactingRoundTripBean.class);
        assertEquals("my-***", parsed.redactedUri);
    }
}
