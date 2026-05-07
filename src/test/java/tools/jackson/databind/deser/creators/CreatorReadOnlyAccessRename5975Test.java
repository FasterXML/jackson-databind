package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5975]: A creator parameter whose implicit name matches the explicit
// name of a sibling READ_ONLY field/getter must still receive its value during
// deserialization.
class CreatorReadOnlyAccessRename5975Test extends DatabindTestUtil
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

    @Test
    public void testImplicitNameWithReadOnly() throws Exception {
        Bean read = MAPPER.readValue("{\"uri\":\"foo\"}", Bean.class);
        assertEquals("foo", read.redactedUri);
    }
}
