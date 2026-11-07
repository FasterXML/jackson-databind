package tools.jackson.databind.ser;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonValueMapKey3119Test extends DatabindTestUtil
{
    static class PrefixKeySerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("key-" + value);
        }
    }

    static class GetterValue {
        @JsonValue
        @JsonSerialize(keyUsing = PrefixKeySerializer.class)
        public Map<String, Integer> value() {
            return Map.of("a", 1);
        }
    }

    static class FieldValue {
        @JsonValue
        @JsonSerialize(keyUsing = PrefixKeySerializer.class)
        public Map<String, Integer> value = Map.of("a", 1);
    }

    static class PlainValue {
        @JsonValue
        public Map<String, Integer> value() {
            return Map.of("a", 1);
        }
    }

    abstract static class ValueMixin {
        @JsonSerialize(keyUsing = PrefixKeySerializer.class)
        abstract Map<String, Integer> value();
    }

    static class Holder {
        public GetterValue wrapped = new GetterValue();
    }

    static class OrdinaryMap {
        @JsonSerialize(keyUsing = PrefixKeySerializer.class)
        public Map<String, Integer> value = Map.of("a", 1);
    }

    static class OuterKeySerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("outer-" + value);
        }
    }

    static class StringValueSerializer extends ValueSerializer<Integer> {
        @Override
        public void serialize(Integer value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("value-" + value);
        }
    }

    static class KeyOverrideHolder {
        @JsonSerialize(keyUsing = OuterKeySerializer.class)
        public GetterValue wrapped = new GetterValue();
    }

    static class ContentOverrideHolder {
        @JsonSerialize(contentUsing = StringValueSerializer.class)
        public GetterValue wrapped = new GetterValue();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonTypeName("typedMap")
    static class TypedGetterValue extends GetterValue { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonTypeName("typedBean")
    static class TypedBeanValue {
        @JsonValue
        @JsonIgnoreProperties("hidden")
        public Object value() {
            return new Payload();
        }
    }

    static class Payload {
        public String hidden = "kept";
    }

    static class MarkerValue {
        @JsonValue
        public Map<String, Integer> markerValue() {
            return Map.of("a", 1);
        }
    }

    private final ObjectMapper mapper = newJsonMapper();

    @Test
    void keySerializerMarkerRetainsDefaultAndDoesNotOverrideAccessor() {
        ObjectMapper withMarkers = JsonMapper.builder()
                .annotationIntrospector(new JacksonAnnotationIntrospector() {
                    @Override
                    public Object findKeySerializer(MapperConfig<?> config, Annotated annotated) {
                        if ("markerValue".equals(annotated.getName())
                                || "wrapped".equals(annotated.getName())) {
                            return ValueSerializer.None.class;
                        }
                        return super.findKeySerializer(config, annotated);
                    }
                }).build();
        assertEquals("{\"a\":1}", withMarkers.writeValueAsString(new MarkerValue()));
        assertEquals("{\"wrapped\":{\"key-a\":1}}", withMarkers.writeValueAsString(new Holder()));
    }

    @Test
    void typedMapUsesAccessorKeySerializer() {
        assertEquals("{\"typedMap\":{\"key-a\":1}}",
                mapper.writeValueAsString(new TypedGetterValue()));
    }

    @Test
    void typedBeanHonorsAccessorIgnoredProperties() {
        // In 3.x, [databind#4762] contextualizes against the accessor, so its
        // @JsonIgnoreProperties applies to typed serialization as well.
        assertEquals("{\"typedBean\":{}}",
                mapper.writeValueAsString(new TypedBeanValue()));
    }

    @Test
    void getterKeySerializer() {
        assertEquals("{\"key-a\":1}", mapper.writeValueAsString(new GetterValue()));
    }

    @Test
    void fieldKeySerializer() {
        assertEquals("{\"key-a\":1}", mapper.writeValueAsString(new FieldValue()));
    }

    @Test
    void mixinKeySerializer() {
        ObjectMapper withMixin = JsonMapper.builder()
                .addMixIn(PlainValue.class, ValueMixin.class).build();
        assertEquals("{\"key-a\":1}", withMixin.writeValueAsString(new PlainValue()));
    }

    @Test
    void wrappedPropertyKeySerializer() {
        assertEquals("{\"wrapped\":{\"key-a\":1}}", mapper.writeValueAsString(new Holder()));
    }

    @Test
    void ordinaryMapControl() {
        assertEquals("{\"value\":{\"key-a\":1}}", mapper.writeValueAsString(new OrdinaryMap()));
    }

    @Test
    void unannotatedValueControl() {
        assertEquals("{\"a\":1}", mapper.writeValueAsString(new PlainValue()));
    }

    @Test
    void staticTypingKeySerializer() {
        ObjectMapper staticMapper = JsonMapper.builder()
                .enable(MapperFeature.USE_STATIC_TYPING).build();
        assertEquals("{\"key-a\":1}", staticMapper.writeValueAsString(new GetterValue()));
    }

    @Test
    void enclosingKeySerializerTakesPrecedence() {
        assertEquals("{\"wrapped\":{\"outer-a\":1}}",
                mapper.writeValueAsString(new KeyOverrideHolder()));
    }

    @Test
    void enclosingContentSerializerIsRetained() {
        assertEquals("{\"wrapped\":{\"key-a\":\"value-1\"}}",
                mapper.writeValueAsString(new ContentOverrideHolder()));
    }

    @Test
    void cachedSerializersKeepPropertyConfiguration() {
        for (int i = 0; i < 2; ++i) {
            assertEquals("{\"key-a\":1}", mapper.writeValueAsString(new GetterValue()));
            assertEquals("{\"wrapped\":{\"outer-a\":1}}",
                    mapper.writeValueAsString(new KeyOverrideHolder()));
            assertEquals("{\"wrapped\":{\"key-a\":1}}", mapper.writeValueAsString(new Holder()));
        }
    }
}
