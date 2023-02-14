package com.fasterxml.jackson.failing;

import java.util.Objects;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// Not sure if [databind#2572] is actually a bug, but behavior in 2.9 was
// different from 2.10 in that no exception thrown and databind quietly just
// left `null` for Beans as `null` even if "EMPTY" was indicated by configuration.
public class JsonSetter2572Test extends BaseMapTest
{
    static class Outer {
        @JsonProperty("inner")
        final Inner inner;

        @JsonCreator
        public Outer(@JsonProperty("inner") Inner inner) {
            this.inner = Objects.requireNonNull(inner, "inner");
        }
    }

    static class Inner {
        @JsonProperty("field")
        final String field;

        @JsonCreator
        public Inner(@JsonProperty("field") String field) {
            this.field = field;
        }
    }

    public void testSetterWithEmpty() throws Exception {
        /*
        ObjectMapper mapper = newObjectMapper()
                .setDefaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY));
                */
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY))
                .build();

        String json = mapper.writeValueAsString(new Outer(new Inner("inner")));
        Outer result = mapper.readValue(json, Outer.class);
        assertNotNull(result);
        assertNotNull(result.inner); // converted to "empty" bean

//System.err.println("Final -> "+mapper.writeValueAsString(result));
    }
}
