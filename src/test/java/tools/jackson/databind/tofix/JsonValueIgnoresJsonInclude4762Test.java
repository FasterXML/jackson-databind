package tools.jackson.databind.tofix;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4762]: `@JsonValue` ignores `@JsonInclude` declared on the annotated field
class JsonValueIgnoresJsonInclude4762Test extends DatabindTestUtil
{
    static class JsonValueWithInclude {
        @JsonValue
        @JsonInclude(value = JsonInclude.Include.NON_NULL,
                content = JsonInclude.Include.NON_NULL)
        public final Map<String, Object> value;

        JsonValueWithInclude(Map<String, Object> value) {
            this.value = value;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    void jsonValueShouldHonorJsonIncludeOnField() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("present", "x");
        map.put("missing", null);

        String json = MAPPER.writeValueAsString(new JsonValueWithInclude(map));

        // Expected: `@JsonInclude(content = NON_NULL)` on the `@JsonValue` field
        // should drop null map values.
        assertEquals("{\"present\":\"x\"}", json);
    }
}
