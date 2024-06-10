package com.fasterxml.jackson.failing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JacksonInject4218Test
{
    static class Dto {
        @JacksonInject("id")
        String id;

        @JsonCreator
        Dto(@JacksonInject("id")
            @JsonProperty("id")
            String id
        ) {
            this.id = id;
        }
    }

    class MyInjectableValues extends InjectableValues.Std
    {
        private static final long serialVersionUID = 1L;

        int nextId = 1; // count up if injected

        @Override
        public Object findInjectableValue(
                Object valueId,
                DeserializationContext ctxt,
                BeanProperty forProperty,
                Object beanInstance
        ) throws JsonMappingException {
            if (valueId.equals("id")) {
                return "id" + nextId++;
            } else {
                return super.findInjectableValue(valueId, ctxt, forProperty, beanInstance);
            }
        }
    }

    @Test
    void test() throws Exception
    {
        ObjectReader reader = new JsonMapper()
                .readerFor(Dto.class)
                .with(new MyInjectableValues());

        Dto dto = reader.readValue("{}");
        String actual = dto.id;

        Assertions.assertEquals("id1", actual);
    }
}
