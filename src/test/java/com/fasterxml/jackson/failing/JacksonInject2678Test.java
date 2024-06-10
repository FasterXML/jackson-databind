package com.fasterxml.jackson.failing;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#2678]: constructor-passed data overridden via field/setter injection
class JacksonInject2678Test extends DatabindTestUtil {
    // [databind#2678]
    protected static class Some {
        private String field1;

        @JacksonInject(value = "defaultValueForField2", useInput = OptBoolean.TRUE)
        private String field2;

        public Some(@JsonProperty("field1") final String field1,
                    @JsonProperty("field2")
                    @JacksonInject(value = "defaultValueForField2", useInput = OptBoolean.TRUE) final String field2) {
//System.err.println("CTOR: setField2 as ["+field2+"]");
            this.field1 = Objects.requireNonNull(field1);
            this.field2 = Objects.requireNonNull(field2);
        }

        public String getField1() {
            return field1;
        }

        public String getField2() {
            return field2;
        }
    }

    // [databind#2678]
    @Test
    void readValueInjectables() throws Exception {
        final InjectableValues injectableValues =
                new InjectableValues.Std().addValue("defaultValueForField2", "somedefaultValue");
        final ObjectMapper mapper = JsonMapper.builder()
                .injectableValues(injectableValues)
                .build();

        final Some actualValueMissing = mapper.readValue("{\"field1\": \"field1value\"}", Some.class);
        assertEquals("field1value", actualValueMissing.getField1());
        assertEquals("somedefaultValue", actualValueMissing.getField2());

        final Some actualValuePresent = mapper.readValue(
                "{\"field1\": \"field1value\", \"field2\": \"field2value\"}", Some.class);
        assertEquals("field1value", actualValuePresent.getField1());

        // if I comment @JacksonInject that is next to the property the valid assert is the correct one:
        assertEquals("field2value", actualValuePresent.getField2());
    }
}
