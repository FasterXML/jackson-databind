package tools.jackson.databind.ser;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonValueDynamicSerializer6392Test extends DatabindTestUtil
{
    private static final String LABEL = "label";

    static class Wrapper {
        private final Object _value;

        Wrapper(Object value) {
            _value = value;
        }

        @JsonValue
        public Object value() {
            return _value;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonTypeName("value")
    static class TypedWrapper extends Wrapper {
        TypedWrapper(Object value) {
            super(value);
        }
    }

    static class First { }
    static class Second { }

    static class LabelSerializer extends StdSerializer<First> {
        private final String _label;

        LabelSerializer(String label) {
            super(First.class);
            _label = label;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt,
                BeanProperty property) {
            return new LabelSerializer((String) ctxt.getAttribute(LABEL));
        }

        @Override
        public void serialize(First value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(_label);
        }
    }

    static class ChangingLabelSequence { }

    static class ChangingViewSequence { }

    static class ViewSerializer extends StdSerializer<First> {
        private final boolean _activeView;

        ViewSerializer(boolean activeView) {
            super(First.class);
            _activeView = activeView;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt,
                BeanProperty property) {
            return new ViewSerializer(ctxt.getActiveView() != null);
        }

        @Override
        public void serialize(First value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeBoolean(_activeView);
        }
    }

    static class ChangingViewSerializer extends StdSerializer<ChangingViewSequence> {
        ChangingViewSerializer() {
            super(ChangingViewSequence.class);
        }

        @Override
        public void serialize(ChangingViewSequence value, JsonGenerator gen,
                SerializationContext ctxt) {
            Wrapper wrapped = new Wrapper(new First());
            ValueSerializer<Object> serializer = ctxt.findTypedValueSerializer(Wrapper.class, true);
            gen.writeStartArray();
            serializer.serialize(wrapped, gen, ctxt);
            ctxt.withActiveView(First.class, () -> serializer.serialize(wrapped, gen, ctxt));
            serializer.serialize(wrapped, gen, ctxt);
            gen.writeEndArray();
        }
    }

    static class ChangingLabelSerializer extends StdSerializer<ChangingLabelSequence> {
        ChangingLabelSerializer() {
            super(ChangingLabelSequence.class);
        }

        @Override
        public void serialize(ChangingLabelSequence value, JsonGenerator gen,
                SerializationContext ctxt) {
            Wrapper wrapped = new Wrapper(new First());
            ValueSerializer<Object> serializer = ctxt.findTypedValueSerializer(Wrapper.class, true);
            gen.writeStartArray();
            serializer.serialize(wrapped, gen, ctxt);
            ctxt.setAttribute(LABEL, "second");
            serializer.serialize(wrapped, gen, ctxt);
            gen.writeEndArray();
        }
    }

    static class CountingSerializer<T> extends StdSerializer<T> {
        private final Map<Class<?>, Integer> _counts;

        CountingSerializer(Class<T> type, Map<Class<?>, Integer> counts) {
            super(type);
            _counts = counts;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt,
                BeanProperty property) {
            _counts.merge(handledType(), 1, Integer::sum);
            return this;
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(handledType().getSimpleName());
        }
    }

    private ObjectMapper _mapperWithCounters(Map<Class<?>, Integer> counts) {
        return jsonMapperBuilder().addModule(new SimpleModule()
                .addSerializer(First.class, new CountingSerializer<>(First.class, counts))
                .addSerializer(Second.class, new CountingSerializer<>(Second.class, counts)))
                .build();
    }

    @Test
    void reusesDynamicSerializers() {
        Map<Class<?>, Integer> counts = new HashMap<>();
        ObjectMapper mapper = _mapperWithCounters(counts);

        Wrapper[] values = { new Wrapper(new First()), new Wrapper(new Second()),
                new Wrapper(new First()), new Wrapper(new Second()) };
        assertEquals(a2q("['First','Second','First','Second']"),
                mapper.writeValueAsString(values));
        assertEquals(Map.of(First.class, 1, Second.class, 1), counts);
    }

    @Test
    void reusesDynamicSerializersWhenWritingTypeIds() {
        Map<Class<?>, Integer> counts = new HashMap<>();
        ObjectMapper mapper = _mapperWithCounters(counts);

        TypedWrapper[] values = { new TypedWrapper(new First()), new TypedWrapper(new First()) };
        assertEquals(a2q("[['value','First'],['value','First']]"),
                mapper.writeValueAsString(values));
        assertEquals(Map.of(First.class, 1), counts);
    }

    @Test
    void contextualizesSeparatelyForDifferentWriters() {
        ObjectMapper mapper = jsonMapperBuilder().addModule(new SimpleModule()
                .addSerializer(First.class, new LabelSerializer(null)))
                .build();
        Wrapper value = new Wrapper(new First());
        ObjectWriter first = mapper.writer().withAttribute(LABEL, "first");
        ObjectWriter second = mapper.writer().withAttribute(LABEL, "second");

        assertEquals(q("first"), first.writeValueAsString(value));
        assertEquals(q("second"), second.writeValueAsString(value));
        assertEquals(q("first"), first.writeValueAsString(value));
    }

    @Test
    void honorsAttributesChangedDuringSerialization() {
        ObjectMapper mapper = jsonMapperBuilder().addModule(new SimpleModule()
                .addSerializer(First.class, new LabelSerializer(null))
                .addSerializer(ChangingLabelSequence.class, new ChangingLabelSerializer()))
                .build();
        ObjectWriter writer = mapper.writer().withAttribute(LABEL, "first");

        assertEquals(a2q("['first','second']"),
                writer.writeValueAsString(new ChangingLabelSequence()));
        assertEquals(q("first"), writer.writeValueAsString(new Wrapper(new First())));
    }

    @Test
    void contextualizesSeparatelyWhileOverridingActiveView() {
        ObjectMapper mapper = jsonMapperBuilder().addModule(new SimpleModule()
                .addSerializer(First.class, new ViewSerializer(false))
                .addSerializer(ChangingViewSequence.class, new ChangingViewSerializer()))
                .build();

        assertEquals("[false,true,false]", mapper.writeValueAsString(new ChangingViewSequence()));
    }
}
