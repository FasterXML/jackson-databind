package com.fasterxml.jackson.databind.module;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

public class DeserializerUpdateBuilderTest extends BaseMapTest
{
    static class MutableBean {

        private String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }

    static class ImmutableBean {

        private final String a;

        @JsonCreator
        public ImmutableBean(@JsonProperty("a") String a) {
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
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            p.skipChildren();
            return CUSTOM_DESERIALIZER_VALUE;
        }
    }

    final ObjectMapper objectMapper = jsonMapperBuilder().addModules(getSimpleModuleWithDeserializerModifier()).build();

    // Succeeds
    public void testMutableBeanUpdateBuilder() throws JsonProcessingException {
        MutableBean recreatedBean = objectMapper.readValue("{\"a\": \"Some value\"}", MutableBean.class);

        assertEquals(CUSTOM_DESERIALIZER_VALUE, recreatedBean.getA());
    }

    // Fails without fix
    public void testImmutableBeanUpdateBuilder() throws JsonProcessingException {
        ImmutableBean recreatedBean = objectMapper.readValue("{\"a\": \"Some value\"}", ImmutableBean.class);

        assertEquals(CUSTOM_DESERIALIZER_VALUE, recreatedBean.getA());
    }
}
