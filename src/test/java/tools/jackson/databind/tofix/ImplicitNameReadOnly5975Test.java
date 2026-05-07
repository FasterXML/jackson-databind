package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5975] Parameters with an implicit name that matches the name of a
// READ_ONLY property are ignored during deserialization.
class ImplicitNameReadOnly5975Test extends DatabindTestUtil
{
    public static class Bean {
        @JsonProperty(value = "uri", access = JsonProperty.Access.READ_ONLY)
        public final String redactedUri;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Bean(String uri) {
            this.redactedUri = uri;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void testImplicitNameWithReadOnly() throws Exception {
        Bean read = MAPPER.readValue("{\"uri\":\"foo\"}", Bean.class);
        assertEquals("foo", read.redactedUri);
    }
}
