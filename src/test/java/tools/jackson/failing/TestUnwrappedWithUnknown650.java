package tools.jackson.failing;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

public class TestUnwrappedWithUnknown650 extends BaseMapTest
{
    static class A {
        @JsonUnwrapped
        public B b;
    }

    static class B {
        public String field;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testFailOnUnknownPropertyUnwrapped() throws Exception
    {
        assertTrue(MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        final String JSON = "{'field': 'value', 'bad':'bad value'}";
        try {
            MAPPER.readValue(a2q(JSON), A.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }
}
