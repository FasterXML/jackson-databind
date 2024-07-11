package com.fasterxml.jackson.failing;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#2572]: "empty" setter, POJO with no 0-arg constructor
class AsEmptyPOJONoDefaultCtor2572Test extends DatabindTestUtil {
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

    // [databind#2572]
    @Test
    void emptyForTypeThatCannotBeInstantiated() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY))
                .build();
        final String json = mapper.writeValueAsString(new Outer(new Inner("inner")));
        assertNotNull(mapper.readValue(json, Outer.class));
    }
}
