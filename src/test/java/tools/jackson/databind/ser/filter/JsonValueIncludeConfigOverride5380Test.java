package tools.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5380]: @JsonValue should respect config override for serialized-as type
public class JsonValueIncludeConfigOverride5380Test
    extends DatabindTestUtil
{
    static class JsonValueWrapper {
        @JsonValue
        public String value() { return _value; }

        private final String _value;

        JsonValueWrapper(String v) { _value = v; }
    }

    static class Pojo {
        public JsonValueWrapper wrapped;
        public String direct;

        Pojo(JsonValueWrapper w, String d) {
            wrapped = w;
            direct = d;
        }
    }

    // Default: NON_EMPTY globally, NON_NULL override for String as property type.
    // Empty string should NOT be suppressed because String override is NON_NULL (not NON_EMPTY).
    // Before fix, "wrapped" was excluded because inclusion was looked up for JsonValueWrapper
    // (which has no override), falling back to the NON_EMPTY default.
    @Test
    public void testJsonValueRespectsStringConfigOverride() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(v -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
                .withConfigOverride(String.class, o ->
                        o.setIncludeAsProperty(JsonInclude.Value.construct(
                                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)))
                .build();

        // Empty string with NON_NULL override for String => should be included
        Pojo pojo = new Pojo(new JsonValueWrapper(""), "");
        String json = mapper.writeValueAsString(pojo);
        assertEquals(a2q("{'direct':'','wrapped':''}"), json);
    }

    // Null wrapper objects should still be suppressed by NON_EMPTY default
    @Test
    public void testNullWrapperSuppressed() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(v -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
                .withConfigOverride(String.class, o ->
                        o.setIncludeAsProperty(JsonInclude.Value.construct(
                                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)))
                .build();

        // null wrapper with NON_NULL for String property => wrapper itself is null, suppressed
        Pojo pojo = new Pojo(null, null);
        String json = mapper.writeValueAsString(pojo);
        assertEquals("{}", json);
    }

    // Without config override, default NON_EMPTY should suppress empty strings
    @Test
    public void testJsonValueDefaultNonEmptyApplies() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(v -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
                .build();

        Pojo pojo = new Pojo(new JsonValueWrapper(""), "");
        String json = mapper.writeValueAsString(pojo);
        assertEquals("{}", json);
    }
}
