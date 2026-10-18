package tools.jackson.databind.ser.filter;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.filter.FilteringGeneratorDelegate;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.filter.JsonIncludeUnwrappedContainer6299Test.Bean;
import tools.jackson.databind.ser.filter.JsonIncludeUnwrappedContainer6299Test.Size;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonIncludeUnwrappedContainer6299CompatibilityTest extends DatabindTestUtil
{
    @JsonSerialize(using = ItemSerializer.class)
    @JsonTypeName("item")
    static class Item {
        final String text;

        Item(String text) {
            this.text = text;
        }
    }

    static class ItemSerializer extends StdSerializer<Item> {
        ItemSerializer() {
            super(Item.class);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, Item value) {
            return value.text.isEmpty();
        }

        @Override
        public void serialize(Item value, JsonGenerator g, SerializationContext ctxt) {
            g.writeStartObject(value);
            _writeContents(value, g);
            g.writeEndObject();
        }

        @Override
        public void serializeWithType(Item value, JsonGenerator g,
                SerializationContext ctxt, TypeSerializer typeSer) {
            WritableTypeId typeId = typeSer.writeTypePrefix(g, ctxt,
                    typeSer.typeId(value, JsonToken.START_OBJECT));
            _writeContents(value, g);
            typeSer.writeTypeSuffix(g, ctxt, typeId);
        }

        private void _writeContents(Item value, JsonGenerator g) {
            g.writeName("nested");
            g.writeStartObject();
            g.writeName("text");
            g.writeString(value.text);
            g.writeEndObject();
        }
    }

    static class FixedItemSerializerMixin {
        @JsonSerialize(contentUsing = ItemSerializer.class)
        public Object values;
    }

    static class EnumSerializer extends StdSerializer<Size> {
        EnumSerializer() {
            super(Size.class);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt,
                Size value) {
            assertTrue(ctxt.getGenerator().currentValue() instanceof EnumSet<?>);
            return value == Size.SMALL;
        }

        @Override
        public void serialize(Size value,
                JsonGenerator g, SerializationContext ctxt) {
            g.writeString(value.name());
        }
    }

    static class EnumSerializerMixin {
        @JsonSerialize(contentUsing = EnumSerializer.class)
        public Object values;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "kind")
    static class TypedItemMixin { }

    @JsonPropertyOrder({"values", "after"})
    static class NonEmptyBean {
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY,
                content = JsonInclude.Include.NON_EMPTY)
        public List<String> values;
        public int after = 1;

        NonEmptyBean(List<String> values) {
            this.values = values;
        }
    }

    static Stream<Arguments> itemContainers() {
        return Stream.of(
            Arguments.of("indexed objects", new TypeReference<Bean<List<Item>>>() { },
                    (Function<Item, Object>) item -> new ArrayList<>(List.of(item))),
            Arguments.of("object set", new TypeReference<Bean<Set<Item>>>() { },
                    (Function<Item, Object>) item -> new LinkedHashSet<>(List.of(item))),
            Arguments.of("object array", new TypeReference<Bean<Item[]>>() { },
                    (Function<Item, Object>) item -> new Item[]{item}),
            Arguments.of("iterable", new TypeReference<Bean<Iterable<Item>>>() { },
                    (Function<Item, Object>) item -> (Iterable<Item>) () -> List.of(item).iterator())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("itemContainers")
    void retainedCustomObjectAndContentType(String description, TypeReference<?> type,
            Function<Item, Object> container) {
        for (boolean fixedSerializer : new boolean[]{false, true}) {
            for (boolean typed : new boolean[]{false, true}) {
                ObjectMapper mapper = _mapper(JsonInclude.Value.construct(
                        JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_EMPTY));
                if (fixedSerializer) {
                    mapper = mapper.rebuild()
                            .addMixIn(Bean.class, FixedItemSerializerMixin.class).build();
                }
                if (typed) {
                    mapper = mapper.rebuild().addMixIn(Item.class, TypedItemMixin.class).build();
                }
                ObjectWriter writer = mapper.writerFor(type);
                String expectedValue = typed ? """
                        {"kind":"item","nested":{"text":"keep"}}""" : """
                        {"nested":{"text":"keep"}}""";
                String expected = "{\"values\":" + expectedValue + ",\"after\":1}";
                Bean<Object> bean = new Bean<>(container.apply(new Item("keep")));
                assertEquals(expected, writer.writeValueAsString(bean),
                        "fixed=" + fixedSerializer + ", typed=" + typed);
                assertEquals(expected, _writeThroughDelegate(writer, bean, true));
                assertEquals("""
                        {"values":[],"after":1}""",
                        writer.writeValueAsString(new Bean<>(container.apply(new Item("")))));
            }
        }
    }

    @Test
    void enumEmptinessUsesTheCurrentContainer() {
        ObjectWriter writer = _mapper(JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_EMPTY)).rebuild()
                .addMixIn(Bean.class, EnumSerializerMixin.class).build()
                .writerFor(new TypeReference<Bean<EnumSet<Size>>>() { });
        assertEquals("""
                {"values":[],"after":1}""", writer.writeValueAsString(new Bean<>(
                        EnumSet.of(Size.SMALL))));
        assertEquals("""
                {"values":"LARGE","after":1}""", writer.writeValueAsString(new Bean<>(
                        EnumSet.of(Size.LARGE))));
    }

    @Test
    void contentSuppressionDoesNotChangePropertyOmission() {
        ObjectMapper mapper = _mapper(JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS));
        assertEquals("""
                {"after":1}""", mapper.writeValueAsString(new NonEmptyBean(List.of())));
        // A physically nonempty container is retained as a property on 3.1,
        // including when its sole element is excluded by content inclusion.
        assertEquals("""
                {"values":[],"after":1}""",
                mapper.writeValueAsString(new NonEmptyBean(List.of(""))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tools.jackson.databind.ser.filter.JsonIncludeUnwrappedContainer6299Test#containers")
    void generatorDelegates(String description, TypeReference<?> type, Object suppressed,
            Object kept, Object mixed, JsonInclude.Value inclusion, String keptJson) {
        ObjectMapper mapper = _mapper(inclusion);
        ObjectWriter writer = mapper.writerFor(type);
        ObjectWriter rootWriter = mapper.writerFor(mapper.constructType(type).containedType(0));
        for (boolean filtering : new boolean[]{false, true}) {
            assertEquals("""
                    {"values":[],"after":1}""",
                    _writeThroughDelegate(writer, new Bean<>(suppressed), filtering));
            assertEquals("{\"values\":" + keptJson + ",\"after\":1}",
                    _writeThroughDelegate(writer, new Bean<>(kept), filtering));
            assertEquals("[]", _writeThroughDelegate(rootWriter, suppressed, filtering));
            assertEquals(keptJson, _writeThroughDelegate(rootWriter, kept, filtering));
        }
    }

    private ObjectMapper _mapper(JsonInclude.Value inclusion) {
        return JsonMapper.builder()
                .changeDefaultPropertyInclusion(ignored -> inclusion)
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS,
                        SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .build();
    }

    private String _writeThroughDelegate(ObjectWriter writer, Object value, boolean filtering) {
        StringWriter output = new StringWriter();
        JsonGenerator generator = writer.createGenerator(output);
        try (JsonGenerator delegate = filtering
                ? new FilteringGeneratorDelegate(generator, TokenFilter.INCLUDE_ALL,
                        TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH, true)
                : new JsonGeneratorDelegate(generator)) {
            writer.writeValue(delegate, value);
        }
        return output.toString();
    }
}
