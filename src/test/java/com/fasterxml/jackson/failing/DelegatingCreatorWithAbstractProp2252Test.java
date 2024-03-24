package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DelegatingCreatorWithAbstractProp2252Test extends DatabindTestUtil {
    static class DelegatingWithAbstractSetter {
        Map<String, Object> _stuff;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public DelegatingWithAbstractSetter(Map<String, Object> stuff) {
            _stuff = stuff;
        }

        public void setNeverUsed(MyAbstractList bogus) {
        }
    }

    @SuppressWarnings("serial")
    static abstract class MyAbstractList extends ArrayList<String> {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // loosely based on [databind#2251], in which delegating creator is used, but
    // theoretically necessary type for setter can cause issues -- shouldn't, as no
    // setters (or fields, getter-as-setter) are ever needed due to delegation
    @Test
    void delegatingWithUnsupportedSetterType() throws Exception {
        DelegatingWithAbstractSetter result = MAPPER.readValue("{ \"bogus\": 3 }", DelegatingWithAbstractSetter.class);
        assertNotNull(result);
    }
}
