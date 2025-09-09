package com.fasterxml.jackson.databind.deser.creators;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.PotentialCreator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NoParamsCreatorDefault5045Test extends DatabindTestUtil
{
    static class User5045 {
        public int age;

        public User5045(@ImplicitName("age") int age) {
            throw new IllegalStateException("Should not be called");
        }

        @JsonCreator
        public User5045() { 
            this.age = -1;
        }

        public int getAge() { return age; }
    }

    @SuppressWarnings("serial")
    static class AI5045 extends ImplicitNameIntrospector {
        @Override
        public PotentialCreator findDefaultCreator(MapperConfig<?> config,
                AnnotatedClass valueClass,
                List<PotentialCreator> declaredConstructors,
                List<PotentialCreator> declaredFactories)
        {
            for (PotentialCreator pc : declaredConstructors) {
                if (pc.paramCount() != 0) {
                    return pc;
                }
            }
            return null;
        }
    }

    @Test
    public void defaultCreator5045() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().annotationIntrospector(new AI5045()).build();
        String json = "{ }";

        User5045 user = mapper.readValue(json, User5045.class);
        assertNotNull(user);
        assertEquals(-1, user.getAge());
    }    
}
