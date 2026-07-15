package tools.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#6091]: with `@JsonTypeInfo` + `defaultImpl`, a mismatch reported while replaying
// buffered content must point at the offending token, not the outer parser that has already
// advanced past the buffered content.
class DefaultImplCurrentToken6091Test extends DatabindTestUtil {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type", defaultImpl = Payload.class)
    static class Payload {
        public int count;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // type id present before the offending value: content is never buffered
    @Test
    void typeIdFirst() throws Exception {
        assertEquals(JsonToken.VALUE_STRING,
                tokenAtMismatch(a2q("{'type':'Payload','count':'x'}")));
    }

    // type id missing: whole object is buffered, then replayed against the defaultImpl
    @Test
    void typeIdMissing() throws Exception {
        assertEquals(JsonToken.VALUE_STRING,
                tokenAtMismatch(a2q("{'count':'x'}")));
    }

    // type id present after the offending value: value is buffered, replayed once id is known
    @Test
    void typeIdLast() throws Exception {
        assertEquals(JsonToken.VALUE_STRING,
                tokenAtMismatch(a2q("{'count':'x','type':'Payload'}")));
    }

    private JsonToken tokenAtMismatch(String json) throws Exception {
        // caller-owned parser: readValue(JsonParser) does not close it, so the token that
        // was current when the mismatch was thrown stays observable via the exception
        try (JsonParser p = MAPPER.createParser(json)) {
            try {
                MAPPER.readValue(p, Payload.class);
                return fail("Should not pass; no exception thrown for: " + json);
            } catch (MismatchedInputException e) {
                return ((JsonParser) e.processor()).currentToken();
            }
        }
    }
}
