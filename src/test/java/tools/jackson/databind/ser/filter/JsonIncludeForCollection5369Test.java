package tools.jackson.databind.ser.filter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5369] Support `@JsonInclude` for collection
public class JsonIncludeForCollection5369Test
    extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* POJOs + Filters (kept together)
    /**********************************************************
     */

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

    // NON_NULL
    static class NonNullListBean {
        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        public List<String> items = new ArrayList<>();

        NonNullListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // NON_EMPTY
    static class NonEmptyListBean {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<String> items = new ArrayList<>();

        NonEmptyListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // NON_DEFAULT
    static class NonDefaultListBean {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public List<String> items = new ArrayList<>();

        NonDefaultListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // Integer = 42 filter
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

    // Short = 7 filter
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

    // Byte = 9 filter
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

    // Long = 100 filter
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

    // Double = 1.25 filter
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

    // Counting filter
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

    enum Test5369Enum {
        A, FOO, B
    }

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

        EnumSet5369Bean(EnumSet<Test5369Enum> v) {
            values = v;
        }
    }

    static class Iterable5369 implements Iterable<Integer>
    {
        private final List<Integer> values;

        public Iterable5369(Integer... ints) {
            values = Arrays.asList(ints);
        }

        @Override
        public Iterator<Integer> iterator() {
            return values.iterator();
        }
    }

    static class BeanWithIterableIncludeNonNull
    {
        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        public Iterable<Integer> values =
                new Iterable5369(1);
    }

    static class IntegerOnly5369Filter {
        @Override
        public boolean equals(Object other) {
            return !Integer.valueOf(5369)
                    .equals(other);
        }
    }

    static class BeanWithIterableCustomInclude
    {
        @JsonInclude(
            content = JsonInclude.Include.CUSTOM,
            contentFilter = IntegerOnly5369Filter.class
        )
        public Iterable<Integer> values =
                new Iterable5369(1);
    }

    /*
    /**********************************************************
    /* Mapper
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
            .build();

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    @Test
    public void testCustomFilterWithList() throws Exception {
        FooListBean input = new FooListBean()
                .add("1").add("foo").add("2");

        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testNonNullContentInclusion() throws Exception {
        NonNullListBean input = new NonNullListBean()
                .add("1").add(null).add("2");

        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testNonEmptyContentInclusion() throws Exception {
        NonEmptyListBean input = new NonEmptyListBean()
                .add("1").add("").add("2");

        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testNonDefaultContentInclusion() throws Exception {
        NonDefaultListBean input = new NonDefaultListBean()
                .add("1").add(null).add("2");

        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithSet() throws Exception {
        FooSetBean input = new FooSetBean()
                .add("1").add("foo").add("2");

        assertEquals(a2q("{'items':['1','2']}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithIntegerList() throws Exception {
        IntegerListPojo input = new IntegerListPojo()
                .add(1).add(42).add(2);

        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithShortList() throws Exception {
        ShortListPojo input = new ShortListPojo()
                .add((short) 1).add((short) 7).add((short) 2);

        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithByteList() throws Exception {
        ByteListPojo input = new ByteListPojo()
                .add((byte) 1).add((byte) 9).add((byte) 2);

        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithDoubleList() throws Exception {
        DoubleListPojo input = new DoubleListPojo()
                .add(0.5).add(1.25).add(2.5);

        assertEquals(a2q("{'values':[0.5,2.5]}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithLongList() throws Exception {
        LongListPojo input = new LongListPojo()
                .add(10L).add(100L).add(20L);

        assertEquals(a2q("{'values':[10,20]}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithNumbers() throws Exception {
        NumberListBean input = new NumberListBean()
                .add(1)
                .add(42)
                .add(3);

        assertEquals(
                a2q("{'numbers':[1,3]}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @Test
    public void testEmptyListWithCustomFilter() throws Exception {
        FooListBean input = new FooListBean();

        assertEquals(
                a2q("{'items':[]}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @Test
    public void testAllFilteredOut() throws Exception {
        FooListBean input = new FooListBean()
                .add("foo")
                .add("foo")
                .add("foo");

        assertEquals(
                a2q("{'items':[]}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @Test
    public void testMixedNullsAndFiltered() throws Exception {
        FooListBean input = new FooListBean()
                .add("1")
                .add(null)
                .add("foo")
                .add("2")
                .add(null);

        // Custom filter should not filter nulls (based on FooFilter.equals implementation)
        assertEquals(
                a2q("{'items':['1',null,'2',null]}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @Test
    public void testContentIncludeOverrideForCollection() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
                .withConfigOverride(List.class,
                        o -> o.setInclude(JsonInclude.Value.empty().withContentFilter(FooFilter.class)))
                .build();

        SimpleList5369Bean input = new SimpleList5369Bean()
                .add("1")
                .add("foo")
                .add("2");

        assertEquals(
                a2q("{'values':['1','2']}"),
                mapper.writeValueAsString(input)
        );
    }

    @Test
    public void testContentIncludeOverrideForList() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
                .withConfigOverride(List.class,
                        o -> o.setInclude(JsonInclude.Value.empty().withContentFilter(FooFilter.class)))
                .build();

        SimpleList5369Bean input = new SimpleList5369Bean()
                .add("1")
                .add("foo")
                .add("2");

        assertEquals(
                a2q("{'values':['1','2']}"),
                mapper.writeValueAsString(input)
        );
    }

    @Test
    public void testEnumSetWithContentFilter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
                .build();

        EnumSet5369Bean input = new EnumSet5369Bean(
                EnumSet.of(Test5369Enum.A, Test5369Enum.FOO, Test5369Enum.B)
        );

        // EXPECTED if content filtering worked:
        //   FOO should be filtered out
        assertEquals(
                a2q("{'values':['A','B']}"),
                mapper.writeValueAsString(input)
        );
    }

    @Test
    public void testIterableWithContentFilteringForNulls() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
                .build();
        BeanWithIterableIncludeNonNull pojo = new BeanWithIterableIncludeNonNull();
        pojo.values = new Iterable5369(1, null, 2, null, 3);

        String json = mapper.writeValueAsString(pojo);

        assertEquals("{\"values\":[1,2,3]}", json);
    }

    @Test
    public void testIterableWithContentFilteringMagicNumber() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
                .build();
        BeanWithIterableCustomInclude pojo = new BeanWithIterableCustomInclude();
        pojo.values = new Iterable5369(1, null, 2, 3, 5369);

        String json = mapper.writeValueAsString(pojo);

        assertEquals("{\"values\":[5369]}", json);
    }

}
