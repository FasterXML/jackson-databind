package com.fasterxml.jackson.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ObjectIdWithInjectable639Test extends DatabindTestUtil {
    // for [databind#639]
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static final class Parent2 {
        @JsonProperty
        public Child2 child;

        @JsonCreator
        public Parent2(@JacksonInject("context") String context) {
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static final class Child2 {
        @JsonProperty
        private final Parent2 parent;

        @JsonCreator
        public Child2(@JsonProperty("parent") Parent2 parent) {
            this.parent = parent;
        }
    }

    // for [databind#639]
    @Test
    void objectIdWithInjectable() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .setInjectableValues(new InjectableValues.Std().
                        addValue("context", "Stuff"));
        Parent2 parent2 = new Parent2("foo");
        Child2 child2 = new Child2(parent2);
        parent2.child = child2;

        String json2 = mapper.writeValueAsString(parent2);
        parent2 = mapper.readValue(json2, Parent2.class);
        assertNotNull(parent2);
        assertNotNull(parent2.child);
    }
}
