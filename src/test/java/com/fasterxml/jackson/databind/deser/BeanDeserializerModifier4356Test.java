package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4356]
class BeanDeserializerModifier4356Test
        extends DatabindTestUtil
{
    static class MutableBean4356 {
        String a;

        public String getA() {
            return a;
        }
        public void setA(String a) {
            this.a = a;
        }
    }

    static class ImmutableBean4356 {
        final String a;

        @JsonCreator
        public ImmutableBean4356(@JsonProperty("a") String a) {
            this.a = a;
        }

        public String getA() {
            return a;
        }
    }

    @SuppressWarnings("serial")
    static SimpleModule getSimpleModuleWithDeserializerModifier() {
        return new SimpleModule().setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
                for (Iterator<SettableBeanProperty> properties = builder.getProperties(); properties.hasNext();) {
                    SettableBeanProperty property = properties.next();
                    if (property.getName().equals("a")) {
                        builder.addOrReplaceProperty(property.withValueDeserializer(new ConvertingStringDeserializer()), true);
                    }
                }
                return builder;
            }
        });
    }


    static final String CUSTOM_DESERIALIZER_VALUE = "Custom deserializer value";

    static class ConvertingStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.skipChildren();
            return CUSTOM_DESERIALIZER_VALUE;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .addModule(getSimpleModuleWithDeserializerModifier())
            .build();

    // passes
    @Test
    void mutableBeanUpdateBuilder() throws JsonProcessingException {
        MutableBean4356 recreatedBean = MAPPER.readValue("{\"a\": \"Some value\"}",
                MutableBean4356.class);

        assertEquals(CUSTOM_DESERIALIZER_VALUE, recreatedBean.getA());
    }

    // Fails without fix
    @Test
    void immutableBeanUpdateBuilder() throws JsonProcessingException {
        ImmutableBean4356 recreatedBean = MAPPER.readValue("{\"a\": \"Some value\"}",
                ImmutableBean4356.class);

        assertEquals(CUSTOM_DESERIALIZER_VALUE, recreatedBean.getA());
    }
}
