package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UnwrappedPropertyBasedCreatorWithPrefixTest extends DatabindTestUtil
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

    @Test
    public void testUnwrappedWithJsonCreatorWithExplicitWithoutName() throws Exception
    {
        String json = "{\"inner-property\": \"value\"}";
        Outer outer = MAPPER.readValue(json, Outer.class);

        assertEquals("value", outer.inner.getProperty());
    }
}
