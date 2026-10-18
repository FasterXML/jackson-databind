package tools.jackson.databind.ser.filter;

import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonIncludeUnwrappedContainer6299Test extends DatabindTestUtil
{
    @JsonPropertyOrder({"values", "after"})
    static class Bean<T> {
        public T values;
        public int after = 1;

        Bean(T values) {
            this.values = values;
        }
    }

    static class UnwrapMixin {
        @JsonFormat(with = JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
        public Object values;
    }

    static class WrapMixin {
        @JsonFormat(without = JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
        public Object values;
    }

    static class StringIterable implements Iterable<String> {
        private final List<String> _values;

        StringIterable(String... values) {
            _values = Arrays.asList(values);
        }

        @Override
        public Iterator<String> iterator() {
            return _values.iterator();
        }
    }

    enum Size { SMALL, LARGE { } }

    static class SuppressSmallFilter {
        @Override
        public boolean equals(Object other) {
            return other == Size.SMALL;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    static class Item {
        final String value;

        Item(String value) {
            this.value = value;
        }
    }

    static class ItemSerializer extends StdSerializer<Item> {
        ItemSerializer() {
            super(Item.class);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, Item value) {
            return "skip".equals(value.value);
        }

        @Override
        public void serialize(Item value, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(value.value);
        }
    }

    static class ItemSerializerMixin {
        @JsonSerialize(contentUsing = ItemSerializer.class)
        public Object values;
    }

    static class ThrowingFilter {
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            throw new IllegalStateException("broken content filter");
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    static Stream<Arguments> exceptionContainers() {
        return Stream.of(
            Arguments.of("indexed strings", new TypeReference<List<String>>() { },
                    List.of("keep")),
            Arguments.of("string set", new TypeReference<Set<String>>() { },
                    new LinkedHashSet<>(List.of("keep"))),
            Arguments.of("indexed objects", new TypeReference<List<Object>>() { },
                    new ArrayList<>(List.of("keep"))),
            Arguments.of("object set", new TypeReference<Set<Object>>() { },
                    new LinkedHashSet<>(List.of("keep"))),
            Arguments.of("object array", new TypeReference<Object[]>() { },
                    new Object[]{"keep"})
        );
    }

    static Stream<Arguments> containers() {
        JsonInclude.Value nonEmpty = JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_EMPTY);
        JsonInclude.Value nonDefault = nonEmpty.withContentInclusion(
                JsonInclude.Include.NON_DEFAULT);
        JsonInclude.Value custom = nonEmpty.withContentFilter(SuppressSmallFilter.class);
        return Stream.of(
            Arguments.of("indexed strings", new TypeReference<Bean<List<String>>>() { },
                    List.of(""), List.of("keep"), List.of("", "keep"), nonEmpty, "\"keep\""),
            Arguments.of("string set", new TypeReference<Bean<Set<String>>>() { },
                    new LinkedHashSet<>(List.of("")), new LinkedHashSet<>(List.of("keep")),
                    new LinkedHashSet<>(List.of("", "keep")), nonEmpty, "\"keep\""),
            Arguments.of("string deque", new TypeReference<Bean<Deque<String>>>() { },
                    new ArrayDeque<>(List.of("")), new ArrayDeque<>(List.of("keep")),
                    new ArrayDeque<>(List.of("", "keep")), nonEmpty, "\"keep\""),
            Arguments.of("indexed objects", new TypeReference<Bean<List<Object>>>() { },
                    new ArrayList<>(List.of("")), new ArrayList<>(List.of("keep")),
                    new ArrayList<>(List.of("", "keep")), nonEmpty, "\"keep\""),
            Arguments.of("object set", new TypeReference<Bean<Set<Object>>>() { },
                    new LinkedHashSet<>(List.of("")), new LinkedHashSet<>(List.of("keep")),
                    new LinkedHashSet<>(List.of("", "keep")), nonEmpty, "\"keep\""),
            Arguments.of("string array", new TypeReference<Bean<String[]>>() { },
                    new String[]{""}, new String[]{"keep"}, new String[]{"", "keep"},
                    nonEmpty, "\"keep\""),
            Arguments.of("object array", new TypeReference<Bean<Object[]>>() { },
                    new Object[]{""}, new Object[]{"keep"}, new Object[]{"", "keep"},
                    nonEmpty, "\"keep\""),
            Arguments.of("iterable", new TypeReference<Bean<Iterable<String>>>() { },
                    new StringIterable(""), new StringIterable("keep"),
                    new StringIterable("", "keep"), nonEmpty, "\"keep\""),
            Arguments.of("enum set", new TypeReference<Bean<EnumSet<Size>>>() { },
                    EnumSet.of(Size.SMALL), EnumSet.of(Size.LARGE),
                    EnumSet.allOf(Size.class), custom, "\"LARGE\""),
            Arguments.of("boolean array", new TypeReference<Bean<boolean[]>>() { },
                    new boolean[]{false}, new boolean[]{true}, new boolean[]{false, true},
                    nonDefault, "true"),
            Arguments.of("short array", new TypeReference<Bean<short[]>>() { },
                    new short[]{0}, new short[]{1}, new short[]{0, 1}, nonDefault, "1"),
            Arguments.of("int array", new TypeReference<Bean<int[]>>() { },
                    new int[]{0}, new int[]{1}, new int[]{0, 1}, nonDefault, "1"),
            Arguments.of("long array", new TypeReference<Bean<long[]>>() { },
                    new long[]{0}, new long[]{1}, new long[]{0, 1}, nonDefault, "1"),
            Arguments.of("float array", new TypeReference<Bean<float[]>>() { },
                    new float[]{0}, new float[]{1}, new float[]{0, 1}, nonDefault, "1.0"),
            Arguments.of("double array", new TypeReference<Bean<double[]>>() { },
                    new double[]{0}, new double[]{1}, new double[]{0, 1}, nonDefault, "1.0")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("containers")
    void suppressedSingleton(String description, TypeReference<?> type, Object suppressed,
            Object kept, Object mixed, JsonInclude.Value inclusion, String keptJson) {
        ObjectMapper mapper = _mapper(inclusion);
        _assertValues(mapper, type, suppressed, "[]");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("containers")
    void retainedContents(String description, TypeReference<?> type, Object suppressed,
            Object kept, Object mixed, JsonInclude.Value inclusion, String keptJson) {
        ObjectMapper mapper = _mapper(inclusion);
        _assertValues(mapper, type, kept, keptJson);
        // Unwrapping is based on physical size, not the number of surviving elements.
        _assertValues(mapper, type, mixed, "[" + keptJson + "]");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("containers")
    void propertyFormatOverrides(String description, TypeReference<?> type, Object suppressed,
            Object kept, Object mixed, JsonInclude.Value inclusion, String keptJson) {
        ObjectMapper unwrap = _mapper(inclusion).rebuild()
                .disable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .addMixIn(Bean.class, UnwrapMixin.class)
                .build();
        _assertValues(unwrap, type, suppressed, "[]");
        _assertValues(unwrap, type, kept, keptJson);

        ObjectMapper wrap = _mapper(inclusion).rebuild()
                .addMixIn(Bean.class, WrapMixin.class)
                .build();
        _assertValues(wrap, type, suppressed, "[]");
        _assertValues(wrap, type, kept, "[" + keptJson + "]");
    }

    @Test
    void nullContentAndDisabledFiltering() {
        JsonInclude.Value nonNull = JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL);
        ObjectMapper mapper = _mapper(nonNull);
        TypeReference<Bean<List<String>>> type = new TypeReference<>() { };
        _assertValues(mapper, type, Collections.singletonList(null), "[]");
        _assertValues(mapper, type, List.of(""), "\"\"");
        _assertValues(mapper.rebuild()
                .disable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS).build(),
                type, Collections.singletonList(null), "null");
    }

    @Test
    void rootContainer() {
        ObjectMapper mapper = _mapper(JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_EMPTY));
        ObjectWriter writer = mapper.writerFor(new TypeReference<List<String>>() { });
        assertEquals("[]", writer.writeValueAsString(List.of("")));
        assertEquals("\"keep\"", writer.writeValueAsString(List.of("keep")));
    }

    @Test
    void customContentSerializer() {
        ObjectMapper mapper = _mapper(JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_EMPTY)).rebuild()
                .addMixIn(Bean.class, ItemSerializerMixin.class)
                .build();
        TypeReference<Bean<List<Item>>> type = new TypeReference<>() { };
        _assertValues(mapper, type, List.of(new Item("skip")), "[]");
        _assertValues(mapper, type, List.of(new Item("keep")), "\"keep\"");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exceptionContainers")
    void contentFilterException(String description, TypeReference<?> type, Object value) {
        ObjectMapper mapper = _mapper(JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.CUSTOM)
                .withContentFilter(ThrowingFilter.class));
        ObjectWriter writer = mapper.writerFor(type).with(SerializationFeature.WRAP_EXCEPTIONS);
        DatabindException exception = assertThrows(DatabindException.class,
                () -> writer.writeValueAsString(value));
        Object expectedSource = value instanceof Object[] array ? array[0] : value;
        assertSame(expectedSource, exception.getPath().get(0).from());
        assertEquals(0, exception.getPath().get(0).getIndex());
        assertEquals(IllegalStateException.class, exception.getCause().getClass());
        ObjectWriter unwrapped = writer.without(SerializationFeature.WRAP_EXCEPTIONS);
        assertThrows(IllegalStateException.class, () -> unwrapped.writeValueAsString(value));
    }

    private ObjectMapper _mapper(JsonInclude.Value inclusion) {
        return JsonMapper.builder()
                .changeDefaultPropertyInclusion(ignored -> inclusion)
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS,
                        SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .build();
    }

    private void _assertValues(ObjectMapper mapper, TypeReference<?> type,
            Object values, String expectedValues) {
        String expected = "{\"values\":" + expectedValues + ",\"after\":1}";
        ObjectWriter writer = mapper.writerFor(type);
        Bean<Object> bean = new Bean<>(values);
        String actual = writer.writeValueAsString(bean);
        assertEquals(expected, actual);
        assertEquals(mapper.readTree(expected), mapper.readTree(actual));
        // Tree conversion preserves Java numeric types, unlike parsing JSON numbers.
        assertEquals(expected, writer.valueToTree(bean).toString());
    }
}
