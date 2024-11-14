package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// [databind#1467]: works for 2.19+, fails for 3.0 for some reason
public class UnwrappedWithPrefixCreator1467Test extends DatabindTestUtil
{
    static class Outer {
        @JsonUnwrapped(prefix = "inner-")
        Inner inner;
    }

    static class Inner {
        private final String _property;

        public Inner(@JsonProperty("property") String property) {
            _property = property;
        }

        public String getProperty() {
            return _property;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void testUnwrappedWithJsonCreatorWithExplicitWithoutName() throws Exception
    {
        String json = "{\"inner-property\": \"value\"}";
        Outer outer = MAPPER.readValue(json, Outer.class);

        assertEquals("value", outer.inner.getProperty());
    }
}
