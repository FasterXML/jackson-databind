package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import static org.junit.jupiter.api.Assertions.*;

class ExternalTypeIdWithUnwrapped2039Test extends DatabindTestUtil
{
    static class MainType2039 {
        public String text;

        @JsonUnwrapped
        public Wrapped2039 wrapped;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "subtype")
        @JsonSubTypes({@JsonSubTypes.Type(value = SubA2039.class, name = "SubA")})
        public SubType2039 sub;

        public void setSub(SubType2039 s) {
            sub = s;
        }

        public void setWrapped(Wrapped2039 w) {
            wrapped = w;
        }
    }

    static class Wrapped2039 {
        public String wrapped;
    }

    public static class SubType2039 {
    }

    public static class SubA2039 extends SubType2039 {
        @JsonProperty
        public boolean bool;
    }

    // [databind#2039]: @JsonUnwrapped and EXTERNAL_PROPERTY not yet supported together
    @Test
    void externalWithUnwrapped2039() throws Exception {
        final ObjectMapper mapper = newJsonMapper();

        final String json = """
                {
                "text": "this is A",
                "wrapped": "yes",
                "subtype": "SubA",
                "sub": {
                  "bool": true
                }
                }
                """;

        // Should fail with informative message, not silently produce wrong result
        DatabindException ex = assertThrows(DatabindException.class,
                () -> mapper.readValue(json, MainType2039.class));
        verifyException(ex, "Cannot (yet) use @JsonUnwrapped");
        verifyException(ex, "EXTERNAL_PROPERTY");
    }
}
