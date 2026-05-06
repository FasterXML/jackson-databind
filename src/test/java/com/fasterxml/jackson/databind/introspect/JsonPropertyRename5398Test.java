package com.fasterxml.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5398] @JsonProperty on getter with @JsonIgnore on setter
// causes deserialization to fail since 2.18.4
public class JsonPropertyRename5398Test extends DatabindTestUtil
{
    static class TestRename5398 {
        private String prop;

        @JsonProperty(value = "renamedProp")
        public String getProp() {
            return prop;
        }

        @JsonIgnore
        public void setProp(String prop) {
            this.prop = prop;
        }
    }

	static class TestStd5398 {
        private String prop;

        @JsonProperty
        public String getProp() {
            return prop;
        }

        @JsonIgnore
        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testRenamedPropertyWithIgnoredSetter5398() throws Exception
    {
        TestRename5398 original = new TestRename5398();
		original.setProp("someValue");

        String json = MAPPER.writeValueAsString(original);

        // Should serialize with renamed property
        assertEquals("{\"renamedProp\":\"someValue\"}", json);

        // @JsonIgnore on the setter prevents write access to the backing field
        // ([databind#5967] fix: inferred non-visible field mutator stripped when setter is @JsonIgnore).
        // Deserialization of "renamedProp" is blocked; field stays at its default (null).
        TestRename5398 result = MAPPER.readerFor(TestRename5398.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertNotNull(result);
        assertNull(result.getProp());
    }

    @Test
    public void testStandardPropertyWithIgnoredSetter5398() throws Exception
    {
        TestStd5398 original = new TestStd5398();
		original.setProp("someValue");

        String json = MAPPER.writeValueAsString(original);

        // Should serialize under the implicit property name
        assertEquals("{\"prop\":\"someValue\"}", json);

        // @JsonIgnore on the setter prevents write access to the backing field.
        // Field stays at its default (null) after deserialization.
        TestStd5398 result = MAPPER.readerFor(TestStd5398.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertNotNull(result);
        assertNull(result.getProp());
    }
}
