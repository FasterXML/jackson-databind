package com.fasterxml.jackson.databind.deser.creators;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.PotentialCreator;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultCreator5045Test {
    static class User {
        private final int age;

        public User(int age) { this.age = age; }

        @JsonCreator
        public User() { this(0); }

        public int getAge() { return age; }
    }

    static class AI extends NopAnnotationIntrospector {
        @Override
        public PotentialCreator findDefaultCreator(MapperConfig<?> config,
                                                   AnnotatedClass valueClass,
                                                   List<PotentialCreator> declaredConstructors,
                                                   List<PotentialCreator> declaredFactories) {
            if (valueClass.getRawType() != User.class) return null;

            return declaredConstructors.stream()
                    .filter(it -> it.paramCount() != 0)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Test
    public void kotlin932() throws JsonProcessingException {
        ObjectMapper mapper = JsonMapper.builder().annotationIntrospector(new AI()).build();
        String json =
                "{\"age\": 25\"}";

        User user = mapper.readValue(json, User.class);

        assertEquals(25, user.getAge());
    }
}

