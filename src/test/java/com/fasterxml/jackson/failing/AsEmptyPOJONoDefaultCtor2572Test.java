package com.fasterxml.jackson.failing;

import java.util.Objects;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// [databind#2572]: "empty" setter, POJO with no 0-arg constructor
public class AsEmptyPOJONoDefaultCtor2572Test extends BaseMapTest
{
    static class Outer {
        @JsonProperty("inner")
        private final Inner inner;

        @JsonCreator
        public Outer(@JsonProperty("inner") Inner inner) {
            this.inner = Objects.requireNonNull(inner, "inner");
        }
    }

    static class Inner {
        @JsonProperty("field")
        private final String field;

        @JsonCreator
        public Inner(@JsonProperty("field") String field) {
            this.field = field;
        }
    }

    public void testJackson() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .setDefaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY));
        final String json = mapper.writeValueAsString(new Outer(new Inner("inner")));
        mapper.readValue(json, Outer.class);
    }
}
