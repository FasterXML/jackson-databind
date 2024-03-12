package tools.jackson.failing;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4356]
public class BeanDeserializerModifier4356Test
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
        return new SimpleModule().setDeserializerModifier(new ValueDeserializerModifier() {
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

    static class ConvertingStringDeserializer extends ValueDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            p.skipChildren();
            return CUSTOM_DESERIALIZER_VALUE;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder().addModule(getSimpleModuleWithDeserializerModifier()).build();

    @Test // passes
    public void testMutableBeanUpdateBuilder() throws Exception {
        MutableBean4356 recreatedBean = MAPPER.readValue("{\"a\": \"Some value\"}",
                MutableBean4356.class);

        assertEquals(CUSTOM_DESERIALIZER_VALUE, recreatedBean.getA());
    }

    @Test // Fails without fix
    public void testImmutableBeanUpdateBuilder() throws Exception {
        ImmutableBean4356 recreatedBean = MAPPER.readValue("{\"a\": \"Some value\"}",
                ImmutableBean4356.class);

        assertEquals(CUSTOM_DESERIALIZER_VALUE, recreatedBean.getA());
    }
}
