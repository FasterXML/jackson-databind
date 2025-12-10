package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.fail;

class UnwrappedWithUnknown650Test extends DatabindTestUtil {
    static class A {
        @JsonUnwrapped
        public B b;
    }

    static class B {
        public String field;
    }


    @Test
    void failOnUnknownPropertyUnwrapped() throws Exception {
        final ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        final String JSON = "{'field': 'value', 'bad':'bad value'}";
        try {
            mapper.readValue(a2q(JSON), A.class);
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }
}
