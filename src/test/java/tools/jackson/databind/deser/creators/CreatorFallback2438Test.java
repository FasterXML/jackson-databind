package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#2438]
class CreatorFallback2438Test extends DatabindTestUtil
{
    static class Creator2438 {
        String value = "";

        @JsonCreator
        public Creator2438(@JsonProperty("value") int v) {
            value = "Creator:"+ v;
        }

        // Public setter (or field) required to show the issue
        public void setValue(int v) {
            value = "Setter:" + v;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void creator2438() throws Exception {
        // note: by default, duplicate-detection not enabled, so should not
        // throw exception. But should only pass second value via Creator,
        // not setter or field
        Creator2438 bean = MAPPER.readValue(a2q("{'value':1, 'value':2}"),
                Creator2438.class);
        assertEquals("Creator:2", bean.value);
    }
}
