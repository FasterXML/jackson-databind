package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonValueDropsSubTerms3777Test extends BaseMapTest {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
     */

    final ObjectMapper MAPPER = new ObjectMapper();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Outer.class),
        @JsonSubTypes.Type(value = Inner.class)
    })
    interface Common {

    }

    static class Inner implements Common {
        public String value;

        public Inner(String value) {
            this.value = value;
        }
    }

    static class Outer implements Common {
        @JsonValue
        private final Common value;

        @JsonCreator
        public Outer(@JsonProperty("value") Common val) {
            this.value = val;
        }
    }

    /*
    /**********************************************************
    /* Test
    /**********************************************************
     */
    public void testInnerBeanRemainsAfterSerAndDeser() throws JsonProcessingException {
        // Arrange
        Inner inner = new Inner("should not be null");
        Outer outer = new Outer(inner);
        String str = MAPPER.writeValueAsString(outer);

        // Act - ***fails here
        Outer actual = MAPPER.readValue(str, Outer.class);

        // Assert
        assertNotNull(actual);
        assertNotNull(actual.value);
    }
}
