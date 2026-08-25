package tools.jackson.databind.ser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PropertySerializerModifier4385Test
{
    static class Value {
        public String value;

        public Value(String value) {
            this.value = value;
        }
    }

    @JsonPropertyOrder({"selected", "annotatedOther", "plainOther"})
    static class TargetBean {
        @JsonSerialize(using = AnnotationValueSerializer.class,
                nullsUsing = AnnotationNullSerializer.class)
        public Value selected;

        @JsonSerialize(using = AnnotationValueSerializer.class)
        public Value annotatedOther;

        public Value plainOther;

        public TargetBean(Value selected) {
            this.selected = selected;
            annotatedOther = new Value("annotated");
            plainOther = new Value("plain");
        }
    }

    static class OtherBean {
        public Value value = new Value("other");
    }

    static class AnnotationValueSerializer extends StdSerializer<Value> {
        public AnnotationValueSerializer() {
            super(Value.class);
        }

        @Override
        public void serialize(Value value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("annotation:" + value.value);
        }
    }

    static class ModifierValueSerializer extends StdSerializer<Object> {
        public ModifierValueSerializer() {
            super(Object.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            Value typedValue = (Value) value;
            gen.writeString("modifier:" + typedValue.value);
        }
    }

    static class GlobalValueSerializer extends StdSerializer<Value> {
        public GlobalValueSerializer() {
            super(Value.class);
        }

        @Override
        public void serialize(Value value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("global:" + value.value);
        }
    }

    static class AnnotationNullSerializer extends StdSerializer<Object> {
        public AnnotationNullSerializer() {
            super(Object.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("annotation-null");
        }
    }

    static class PropertySerializerModifier extends ValueSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
            if (beanDesc.getBeanClass() == TargetBean.class) {
                for (BeanPropertyWriter property : beanProperties) {
                    if ("selected".equals(property.getName())) {
                        property.assignSerializer(new ModifierValueSerializer());
                    }
                }
            }
            return beanProperties;
        }
    }

    static class NullingPropertySerializerModifier extends ValueSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
            if (beanDesc.getBeanClass() == TargetBean.class) {
                for (BeanPropertyWriter property : beanProperties) {
                    if ("selected".equals(property.getName())) {
                        property.assignSerializer(null);
                    }
                }
            }
            return beanProperties;
        }
    }

    private final JsonMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addSerializer(Value.class, new GlobalValueSerializer())
                    .setSerializerModifier(new PropertySerializerModifier()))
            .build();

    private final JsonMapper NULLING_MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .setSerializerModifier(new NullingPropertySerializerModifier()))
            .build();

    @Test
    public void testOnlySelectedPropertyIsReplaced() throws Exception {
        assertEquals("{\"selected\":\"modifier:selected\","
                + "\"annotatedOther\":\"annotation:annotated\","
                + "\"plainOther\":\"global:plain\"}",
                MAPPER.writeValueAsString(new TargetBean(new Value("selected"))));
        assertEquals("{\"value\":\"global:other\"}",
                MAPPER.writeValueAsString(new OtherBean()));
    }

    @Test
    public void testNullSerializerIsNotReplaced() throws Exception {
        assertEquals("{\"selected\":\"annotation-null\","
                + "\"annotatedOther\":\"annotation:annotated\","
                + "\"plainOther\":\"global:plain\"}",
                MAPPER.writeValueAsString(new TargetBean(null)));
    }

    @Test
    public void testNullAssignmentIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> NULLING_MAPPER.writeValueAsString(new TargetBean(new Value("selected"))));
    }
}
