package com.fasterxml.jackson.databind.jsontype.deftyping;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeserDefaultTypedConcrete2968Test extends DatabindTestUtil {

    static abstract class SimpleBall {
        public int size = 3;
    }

    static class BasketBall extends SimpleBall {
        protected BasketBall() {}

        public BasketBall(int size) {
            super();
            this.size = size;
        }
    }

    @Test
    public void testDeserializationConcreteClassWithDefaultTyping() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(SimpleBall.class)
            .build();
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY)
            .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
            .build();

        final String concreteTypeJson = a2q("{'size': 42}");
        BasketBall basketBall = mapper.readValue(concreteTypeJson, BasketBall.class);
        assertEquals(42, basketBall.size);
    }
}
