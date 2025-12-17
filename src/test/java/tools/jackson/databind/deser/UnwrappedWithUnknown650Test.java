package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

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
    
    @Test
    void failOnUnknownPropertyUnwrapped() throws Exception {
        final String json = a2q("{'field': 'value', 'bad': 'bad value'}");
        try {
            A a = MAPPER.readValue(json, A.class);
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
