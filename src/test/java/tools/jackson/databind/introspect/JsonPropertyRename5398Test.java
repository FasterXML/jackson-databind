package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5398] @JsonProperty on getter with @JsonIgnore on setter
// causes deserialization to fail since 2.18.4
public class JsonPropertyRename5398Test extends DatabindTestUtil
{
    static class Test5398 {
        private String prop = "someValue";

        @JsonProperty(value = "renamedProp")
        public String getProp() {
            return prop;
        }

        @JsonIgnore
        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testRenamedPropertyWithIgnoredSetter5398()
    {
        Test5398 original = new Test5398();
        String json = MAPPER.writeValueAsString(original);

        // Should serialize with renamed property
        assertEquals("{\"renamedProp\":\"someValue\"}", json);

        // Should be able to deserialize back (setter is ignored, so field remains default)
        Test5398 result = MAPPER.readValue(json, Test5398.class);
        assertNotNull(result);
        // Since setter is ignored, the deserialized object should have the default value
        assertEquals("someValue", result.getProp());
    }
}
