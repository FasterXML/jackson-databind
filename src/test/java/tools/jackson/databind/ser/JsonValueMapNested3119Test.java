package tools.jackson.databind.ser;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonValueMapNested3119Test
{
    static final UUID ID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    public static class StringKey extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("outer-" + value);
        }
    }

    public static class AccessorKey extends ValueSerializer<UUID> {
        @Override
        public void serialize(UUID value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("uuid-" + value);
        }
    }

    public static class ObjectKey extends ValueSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("outer-" + value);
        }
    }

    public static class Wrapper {
        @JsonValue
        public Map<UUID, Integer> value() {
            return Map.of(ID, 7);
        }
    }

    public static class AccessorWrapper {
        @JsonValue
        @JsonSerialize(keyUsing = AccessorKey.class)
        public Map<UUID, Integer> value() {
            return Map.of(ID, 7);
        }
    }

    public static class OuterString {
        @JsonSerialize(keyUsing = StringKey.class)
        public Map<String, Wrapper> values = Map.of("item", new Wrapper());
    }

    public static class OuterObject {
        @JsonSerialize(keyUsing = ObjectKey.class)
        public Map<String, Wrapper> values = Map.of("item", new Wrapper());
    }

    public static class OuterAccessor {
        @JsonSerialize(keyUsing = StringKey.class)
        public Map<String, AccessorWrapper> values = Map.of("item", new AccessorWrapper());
    }

    public static class ObjectDeclared {
        @JsonSerialize(keyUsing = ObjectKey.class)
        public Object value = new Wrapper();
    }

    public static class SubWrapper extends Wrapper { }

    public static class SuperDeclared {
        @JsonSerialize(keyUsing = ObjectKey.class)
        public Wrapper value = new SubWrapper();
    }

    public static class DirectWrapper {
        @JsonSerialize(keyUsing = ObjectKey.class)
        public Wrapper value = new Wrapper();
    }

    public static class ContentForwarder extends ValueSerializer<Wrapper> {
        private final BeanProperty property;

        public ContentForwarder() {
            this(null);
        }

        private ContentForwarder(BeanProperty p) {
            property = p;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty p) {
            return new ContentForwarder(p);
        }

        @Override
        public void serialize(Wrapper value, JsonGenerator gen, SerializationContext ctxt) {
            ctxt.findPrimaryPropertySerializer(Wrapper.class, property).serialize(value, gen, ctxt);
        }
    }

    public static class OuterContent {
        @JsonSerialize(contentUsing = ContentForwarder.class)
        public Map<String, Wrapper> values = Map.of("item", new Wrapper());
    }

    public static class IntContent extends ValueSerializer<Integer> {
        @Override
        public void serialize(Integer v, JsonGenerator g, SerializationContext c) {
            g.writeString("int-" + v);
        }
    }

    public static class DirectContent {
        @JsonSerialize(contentUsing = IntContent.class)
        public Wrapper value = new Wrapper();
    }

    private ObjectMapper mapper(boolean staticTyping) {
        return staticTyping ? JsonMapper.builder().enable(MapperFeature.USE_STATIC_TYPING).build()
                : JsonMapper.builder().build();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void outerStringKeyDoesNotReachNestedJsonValueMap(boolean staticTyping) {
        assertEquals("{\"values\":{\"outer-item\":{\"" + ID + "\":7}}}",
                mapper(staticTyping).writeValueAsString(new OuterString()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void outerObjectKeyDoesNotRewriteNestedMapKeys(boolean staticTyping) {
        assertEquals("{\"values\":{\"outer-item\":{\"" + ID + "\":7}}}",
                mapper(staticTyping).writeValueAsString(new OuterObject()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nestedAccessorKeySerializerIsPreserved(boolean staticTyping) {
        assertEquals("{\"values\":{\"outer-item\":{\"uuid-" + ID + "\":7}}}",
                mapper(staticTyping).writeValueAsString(new OuterAccessor()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void outerContentSerializerDoesNotReachNestedMapContents(boolean staticTyping) {
        assertEquals("{\"values\":{\"item\":{\"" + ID + "\":7}}}",
                mapper(staticTyping).writeValueAsString(new OuterContent()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void matchingDeclaredWrapperTypeKeepsKeyOverride(boolean staticTyping) {
        assertEquals("{\"value\":{\"outer-" + ID + "\":7}}",
                mapper(staticTyping).writeValueAsString(new DirectWrapper()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void matchingDeclaredWrapperTypeKeepsContentOverride(boolean staticTyping) {
        assertEquals("{\"value\":{\"" + ID + "\":\"int-7\"}}",
                mapper(staticTyping).writeValueAsString(new DirectContent()));
    }

    @Test
    void mismatchedDeclaredTypeRetainsUpstreamBehavior() {
        ObjectMapper mapper = mapper(false);
        String expected = "{\"value\":{\"" + ID + "\":7}}";
        assertEquals(expected, mapper.writeValueAsString(new ObjectDeclared()));
        assertEquals(expected, mapper.writeValueAsString(new SuperDeclared()));
    }
}
