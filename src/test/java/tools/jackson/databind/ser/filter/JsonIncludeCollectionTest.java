package tools.jackson.databind.ser.filter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class JsonIncludeCollectionTest extends DatabindTestUtil
{
    // Basic NON_EMPTY on EnumSet field
    static class NonEmptyEnumSet {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public EnumSet<ABC> v;

        public NonEmptyEnumSet(ABC...values) {
            if (values.length == 0) {
                v = EnumSet.noneOf(ABC.class);
            } else {
                v = EnumSet.copyOf(Arrays.asList(values));
            }
        }
    }

    // [databind#5369] Content filtering on collections

    // String "foo" filter
    static class FooFilter {
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            return "foo".equals(other);
        }
    }

    static class FooListBean {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public List<String> items = new ArrayList<>();

        FooListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    static class FooSetBean {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public Set<String> items = new LinkedHashSet<>();

        FooSetBean add(String value) {
            items.add(value);
            return this;
        }
    }

    static class NonNullListBean {
        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        public List<String> items = new ArrayList<>();

        NonNullListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    static class NonEmptyListBean {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<String> items = new ArrayList<>();

        NonEmptyListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    static class NonDefaultListBean {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public List<String> items = new ArrayList<>();

        NonDefaultListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    static class IntegerFilter {
        @Override
        public boolean equals(Object other) {
            return Integer.valueOf(42).equals(other);
        }
    }

    static class IntegerListPojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = IntegerFilter.class)
        public List<Integer> values = new ArrayList<>();

        IntegerListPojo add(int v) {
            values.add(v);
            return this;
        }
    }

    static class ShortFilter {
        @Override
        public boolean equals(Object other) {
            return Short.valueOf((short) 7).equals(other);
        }
    }

    static class ShortListPojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = ShortFilter.class)
        public List<Short> values = new ArrayList<>();

        ShortListPojo add(short v) {
            values.add(v);
            return this;
        }
    }

    static class ByteFilter {
        @Override
        public boolean equals(Object other) {
            return Byte.valueOf((byte) 9).equals(other);
        }
    }

    static class ByteListPojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = ByteFilter.class)
        public List<Byte> values = new ArrayList<>();

        ByteListPojo add(byte v) {
            values.add(v);
            return this;
        }
    }

    static class LongFilter {
        @Override
        public boolean equals(Object other) {
            return Long.valueOf(100L).equals(other);
        }
    }

    static class LongListPojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = LongFilter.class)
        public List<Long> values = new ArrayList<>();

        LongListPojo add(long v) {
            values.add(v);
            return this;
        }
    }

    static class DoubleFilter {
        @Override
        public boolean equals(Object other) {
            return Double.valueOf(1.25).equals(other);
        }
    }

    static class DoubleListPojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = DoubleFilter.class)
        public List<Double> values = new ArrayList<>();

        DoubleListPojo add(double v) {
            values.add(v);
            return this;
        }
    }

    static class CountingFooFilter {
        static final AtomicInteger counter = new AtomicInteger();

        @Override
        public boolean equals(Object other) {
            counter.incrementAndGet();
            return "foo".equals(other);
        }
    }

    static class CountingFooListBean {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = CountingFooFilter.class)
        public List<String> items = new ArrayList<>();

        CountingFooListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    static class NumberFilter {
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            return Integer.valueOf(42).equals(other);
        }
    }

    static class NumberListBean {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = NumberFilter.class)
        public List<Integer> numbers = new ArrayList<>();

        public NumberListBean add(Integer value) {
            numbers.add(value);
            return this;
        }
    }

    static class SimpleList5369Bean {
        public List<String> values = new ArrayList<>();

        SimpleList5369Bean add(String v) {
            values.add(v);
            return this;
        }
    }

    enum Test5369Enum { A, FOO, B }

    static class FooEnum5369Filter {
        @Override
        public boolean equals(Object other) {
            return Test5369Enum.FOO.equals(other);
        }
    }

    static class EnumSet5369Bean {
        @JsonInclude(
                content = JsonInclude.Include.CUSTOM,
                contentFilter = FooEnum5369Filter.class
        )
        public EnumSet<Test5369Enum> values;

        EnumSet5369Bean(EnumSet<Test5369Enum> v) { values = v; }
    }

    static class Iterable5369 implements Iterable<Integer>
    {
        private final List<Integer> values;

        public Iterable5369(Integer... ints) { values = Arrays.asList(ints); }

        @Override
        public Iterator<Integer> iterator() { return values.iterator(); }
    }

    static class BeanWithIterableIncludeNonNull
    {
        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        public Iterable<Integer> values = new Iterable5369(1);
    }

    static class IntegerOnly5369Filter {
        @Override
        public boolean equals(Object other) {
            return !Integer.valueOf(5369).equals(other);
        }
    }

    static class BeanWithIterableCustomInclude
    {
        @JsonInclude(
            content = JsonInclude.Include.CUSTOM,
            contentFilter = IntegerOnly5369Filter.class
        )
        public Iterable<Integer> values = new Iterable5369(1);
    }

    // Wildcard-typed Iterable so that `_elementSerializer` stays null and
    // per-element serializers are resolved dynamically at serialize time.
    static class IterableOfObjects implements Iterable<Object>
    {
        private final List<Object> values;

        IterableOfObjects(Object... items) { values = Arrays.asList(items); }

        @Override
        public Iterator<Object> iterator() { return values.iterator(); }
    }

    static class BeanWithIterableNonEmpty
    {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Iterable<Object> values;

        BeanWithIterableNonEmpty(Object... items) {
            values = new IterableOfObjects(items);
        }
    }

    // [databind#5370]: content filtering must also apply on the typed (polymorphic)
    // serialization path of `StringCollectionSerializer`, like the regular path and
    // sibling `IndexedStringListSerializer` already do.
    static class TypedStringCollectionNonEmpty {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Collection<String> values;

        TypedStringCollectionNonEmpty(Collection<String> v) { values = v; }
    }

    static class TypedStringCollectionNonNull {
        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        public Collection<String> values;

        TypedStringCollectionNonNull(Collection<String> v) { values = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5369]
    private final ObjectMapper MAPPER_CONTAINERS = jsonMapperBuilder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .build();

    // [databind#5370]: default typing forces `serializeWithType` on the String collection
    private final ObjectMapper MAPPER_CONTAINERS_TYPED = jsonMapperBuilder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .activateDefaultTyping(NoCheckSubTypeValidator.instance, DefaultTyping.NON_FINAL)
            .build();

    /*
    /**********************************************************************
    /* Test methods, @JsonInclude(NON_EMPTY) on collection field
    /**********************************************************************
     */

    @Test
    public void testEnumSet() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyEnumSet()));
        assertEquals("{\"v\":[\"B\"]}", MAPPER.writeValueAsString(new NonEmptyEnumSet(ABC.B)));
    }

    /*
    /**********************************************************************
    /* Test methods, content filtering on collections [databind#5369]
    /**********************************************************************
     */

    // [databind#5369]
    @Test
    public void testCustomFilterWithList() throws Exception {
        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER_CONTAINERS.writeValueAsString(new FooListBean().add("1").add("foo").add("2")));
    }

    @Test
    public void testNonNullContentInclusion() throws Exception {
        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER_CONTAINERS.writeValueAsString(new NonNullListBean().add("1").add(null).add("2")));
    }

    @Test
    public void testNonEmptyContentInclusion() throws Exception {
        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER_CONTAINERS.writeValueAsString(new NonEmptyListBean().add("1").add("").add("2")));
    }

    @Test
    public void testNonDefaultContentInclusion() throws Exception {
        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER_CONTAINERS.writeValueAsString(new NonDefaultListBean().add("1").add(null).add("2")));
    }

    @Test
    public void testCustomFilterWithSet() throws Exception {
        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER_CONTAINERS.writeValueAsString(new FooSetBean().add("1").add("foo").add("2")));
    }

    @Test
    public void testCustomFilterWithIntegerList() throws Exception {
        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER_CONTAINERS.writeValueAsString(new IntegerListPojo().add(1).add(42).add(2)));
    }

    @Test
    public void testCustomFilterWithShortList() throws Exception {
        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER_CONTAINERS.writeValueAsString(
                        new ShortListPojo().add((short) 1).add((short) 7).add((short) 2)));
    }

    @Test
    public void testCustomFilterWithByteList() throws Exception {
        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER_CONTAINERS.writeValueAsString(
                        new ByteListPojo().add((byte) 1).add((byte) 9).add((byte) 2)));
    }

    @Test
    public void testCustomFilterWithDoubleList() throws Exception {
        assertEquals(a2q("{'values':[0.5,2.5]}"),
                MAPPER_CONTAINERS.writeValueAsString(
                        new DoubleListPojo().add(0.5).add(1.25).add(2.5)));
    }

    @Test
    public void testCustomFilterWithLongList() throws Exception {
        assertEquals(a2q("{'values':[10,20]}"),
                MAPPER_CONTAINERS.writeValueAsString(
                        new LongListPojo().add(10L).add(100L).add(20L)));
    }

    @Test
    public void testCustomFilterWithNumbers() throws Exception {
        assertEquals(a2q("{'numbers':[1,3]}"),
                MAPPER_CONTAINERS.writeValueAsString(new NumberListBean().add(1).add(42).add(3)));
    }

    @Test
    public void testEmptyListWithCustomFilter() throws Exception {
        assertEquals(a2q("{'items':[]}"),
                MAPPER_CONTAINERS.writeValueAsString(new FooListBean()));
    }

    @Test
    public void testAllFilteredOut() throws Exception {
        assertEquals(a2q("{'items':[]}"),
                MAPPER_CONTAINERS.writeValueAsString(
                        new FooListBean().add("foo").add("foo").add("foo")));
    }

    @Test
    public void testMixedNullsAndFiltered() throws Exception {
        // Custom filter should not filter nulls (based on FooFilter.equals implementation)
        assertEquals(a2q("{'items':['1',null,'2',null]}"),
                MAPPER_CONTAINERS.writeValueAsString(
                        new FooListBean().add("1").add(null).add("foo").add("2").add(null)));
    }

    @Test
    public void testContentIncludeOverrideForCollection() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
                .withConfigOverride(List.class,
                        o -> o.setInclude(JsonInclude.Value.empty().withContentFilter(FooFilter.class)))
                .build();

        assertEquals(a2q("{'values':['1','2']}"),
                mapper.writeValueAsString(new SimpleList5369Bean().add("1").add("foo").add("2")));
    }

    @Test
    public void testContentIncludeOverrideForList() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
                .withConfigOverride(List.class,
                        o -> o.setInclude(JsonInclude.Value.empty().withContentFilter(FooFilter.class)))
                .build();

        assertEquals(a2q("{'values':['1','2']}"),
                mapper.writeValueAsString(new SimpleList5369Bean().add("1").add("foo").add("2")));
    }

    @Test
    public void testEnumSetWithContentFilter() throws Exception
    {
        assertEquals(a2q("{'values':['A','B']}"),
                MAPPER_CONTAINERS.writeValueAsString(
                        new EnumSet5369Bean(EnumSet.of(Test5369Enum.A, Test5369Enum.FOO, Test5369Enum.B))));
    }

    @Test
    public void testIterableWithContentFilteringForNulls() throws Exception
    {
        BeanWithIterableIncludeNonNull pojo = new BeanWithIterableIncludeNonNull();
        pojo.values = new Iterable5369(1, null, 2, null, 3);

        assertEquals("{\"values\":[1,2,3]}", MAPPER_CONTAINERS.writeValueAsString(pojo));
    }

    @Test
    public void testIterableWithContentFilteringMagicNumber() throws Exception
    {
        BeanWithIterableCustomInclude pojo = new BeanWithIterableCustomInclude();
        pojo.values = new Iterable5369(1, null, 2, 3, 5369);

        assertEquals("{\"values\":[5369]}", MAPPER_CONTAINERS.writeValueAsString(pojo));
    }

    // Regression: when `_elementSerializer` is not resolved statically (wildcard/Object element type),
    // per-element dynamic serializer must be passed to `_shouldSerializeElement` so that
    // `serializer.isEmpty(ctxt, elem)` is consulted for NON_EMPTY content inclusion.
    @Test
    public void testIterableNonEmptyWithWildcardElementType() throws Exception
    {
        BeanWithIterableNonEmpty pojo = new BeanWithIterableNonEmpty(
                Collections.emptyMap(),
                Collections.singletonMap("k", "v"),
                Collections.emptyList(),
                Collections.singletonList("x"));

        assertEquals(a2q("{'values':[{'k':'v'},['x']]}"),
                MAPPER_CONTAINERS.writeValueAsString(pojo));
    }

    // [databind#5370]: NON_EMPTY content filtering applied on typed (polymorphic) path,
    // not just the regular `serialize()` path
    @Test
    public void testTypedStringCollectionNonEmpty() throws Exception
    {
        Collection<String> set = new LinkedHashSet<>(Arrays.asList("1", "", "2"));
        assertEquals(a2q("['tools.jackson.databind.ser.filter.JsonIncludeCollectionTest$TypedStringCollectionNonEmpty',"
                        + "{'values':['java.util.LinkedHashSet',['1','2']]}]"),
                MAPPER_CONTAINERS_TYPED.writeValueAsString(new TypedStringCollectionNonEmpty(set)));
    }

    // [databind#5370]: NON_NULL content filtering applied on typed (polymorphic) path
    @Test
    public void testTypedStringCollectionNonNull() throws Exception
    {
        Collection<String> set = new LinkedHashSet<>(Arrays.asList("1", null, "2"));
        assertEquals(a2q("['tools.jackson.databind.ser.filter.JsonIncludeCollectionTest$TypedStringCollectionNonNull',"
                        + "{'values':['java.util.LinkedHashSet',['1','2']]}]"),
                MAPPER_CONTAINERS_TYPED.writeValueAsString(new TypedStringCollectionNonNull(set)));
    }
}
