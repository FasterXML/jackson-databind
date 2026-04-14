package tools.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3185] @JsonIdentityInfo with @JsonCreator must not re-assign the id
// field after construction (was overwriting constructor-computed value and
// requiring reflective final-field access on GraalVM native image).
public class ObjectIdWithCreator3185Test extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class PojoWithIdentityInfo {
        private final String fieldForId;

        @JsonCreator
        public PojoWithIdentityInfo(@JsonProperty("id") String fieldForId) {
            this.fieldForId = fieldForId + "-from-constructor";
        }

        @JsonGetter("id")
        public String getFieldForId() {
            return fieldForId;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testCreatorValuePreservedWithIdentityInfo() throws Exception
    {
        PojoWithIdentityInfo result = MAPPER.readValue(
                "{\"id\": \"valueFromJson\"}", PojoWithIdentityInfo.class);
        assertEquals("valueFromJson-from-constructor", result.getFieldForId());
    }
}
