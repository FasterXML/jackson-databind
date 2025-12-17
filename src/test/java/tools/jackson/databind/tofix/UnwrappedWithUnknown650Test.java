package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UnwrappedWithUnknown650Test extends DatabindTestUtil {
    static class A {
        @JsonUnwrapped
        public B b;
    }

    static class B {
        public String field;
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    
    @JacksonTestFailureExpected
    @Test
    void failOnUnknownPropertyUnwrapped() throws Exception {

        final String JSON = "{'field': 'value', 'bad': 'bad value'}";
        try {
            MAPPER.readValue(a2q(JSON), A.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    // Passing case, regular usage
    @Test
    void worksOnRegularPropertyUnwrapped() throws Exception {
        A value = MAPPER.readValue(a2q("{'field': 'value'}"), A.class);
        assertEquals("value", value.b.field);
    }
}
