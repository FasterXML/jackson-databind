package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnwrappedWithIgnore1075Test extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1075]
    static class Outer {
        @JsonUnwrapped
        private Inner inner;

        @JsonIgnore
        public Long getId() {
            return inner.id;
        }
    }

    static class Inner {
        @JsonProperty
        Long id;

        @JsonProperty
        String name;
    }

    // [databind#1075]
    @JacksonTestFailureExpected
    @Test
    public void jsonUnwrappedShouldDeserializeFieldsWithGetterInOuterClass() throws Exception
    {
        final String JSON = "{\"id\": 1, \"name\": \"John\"}";
        Outer outer = MAPPER.readValue(JSON, Outer.class);
        assertEquals(Long.valueOf(1), outer.getId());
    }
}
