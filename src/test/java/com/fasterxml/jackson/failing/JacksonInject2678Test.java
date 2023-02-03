package com.fasterxml.jackson.failing;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

// [databind#2678]: constructor-passed data overridden via field/setter injection
public class JacksonInject2678Test extends BaseMapTest
{
    // [databind#2678]
    protected static class Some {
        private String field1;

        @JacksonInject(value = "defaultValueForField2", useInput = OptBoolean.TRUE)
        private String field2;

        public Some(@JsonProperty("field1") final String field1,
                @JsonProperty("field2")
                @JacksonInject(value = "defaultValueForField2", useInput = OptBoolean.TRUE)
                final String field2) {
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
    public void testReadValueInjectables() throws Exception {
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
