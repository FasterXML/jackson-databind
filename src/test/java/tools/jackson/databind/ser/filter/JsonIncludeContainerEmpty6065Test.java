package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6065]: SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS should
// drop containers that become empty once content @JsonInclude filtering is applied,
// matching the behavior already implemented for Maps (#1649).
public class JsonIncludeContainerEmpty6065Test extends DatabindTestUtil
{
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class Bean {
        public String myString;
        public List<String> myList;
        public Map<String, String> myMap;
    }

    // List of non-String elements (routed through IndexedListSerializer)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class IntListBean {
        public List<Integer> values;
    }

    // Set (routed through CollectionSerializer)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class StringSetBean {
        public Set<String> values;
    }

    // String array (routed through StringArraySerializer)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class StringArrayBean {
        public String[] values;
    }

    // Object array (routed through ObjectArraySerializer)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class IntArrayBean {
        public Integer[] values;
    }

    // Iterable (routed through IterableSerializer): must be a genuine non-Collection
    // Iterable, otherwise runtime-type resolution routes it through CollectionSerializer
    static class StringIterable implements Iterable<String> {
        private final List<String> _values;
        StringIterable(String... values) { _values = Arrays.asList(values); }
        @Override public Iterator<String> iterator() { return _values.iterator(); }
    }

    // Primitive arrays (routed through JDKArraySerializers.*ArraySerializer): content
    // NON_DEFAULT suppresses default-valued elements (0 / false), which is the only
    // content inclusion that can suppress primitives (they are never "empty"/null).
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_DEFAULT)
    static class PrimArraysBean {
        public int[] ints;
        public long[] longs;
        public short[] shorts;
        public double[] doubles;
        public float[] floats;
        public boolean[] bools;
    }

    // EnumSet (routed through EnumSetSerializer): enums are never null/empty, so only a
    // CUSTOM content filter can suppress elements.
    enum Size { SMALL, LARGE }

    // Custom @JsonInclude content filter: its equals() returns true for values to suppress
    static class SuppressSmallFilter {
        @Override public boolean equals(Object other) { return other == Size.SMALL; }
        @Override public int hashCode() { return 0; }
    }

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY,
            content = JsonInclude.Include.CUSTOM, contentFilter = SuppressSmallFilter.class)
    static class EnumSetBean {
        public EnumSet<Size> values;
    }

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class IterableBean {
        public Iterable<String> values;
    }

    // Nested containers: content filtering recurses, so an inner container that becomes
    // empty is itself suppressed as an element of the outer container
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class NestedListBean {
        public List<List<String>> values;
    }

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class MapOfListsBean {
        public Map<String, List<String>> values;
    }

    // content = NON_NULL: only nulls are suppressed, empty Strings are retained
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
    static class NonNullContentBean {
        public List<String> strings;
        public List<Integer> numbers;
        public String[] stringArray;
        public Integer[] numberArray;
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .build();

    private final ObjectMapper NO_FEATURE = JsonMapper.builder()
            .disable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .build();

    @Test
    public void allNull() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(new Bean()));
    }

    @Test
    public void emptyContainers() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>();
        bean.myMap = new HashMap<>();
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void containersEmptyAfterContentFilter() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Collections.singletonList(null));
        bean.myMap = new HashMap<>();
        bean.myMap.put("1", null);
        // Both list and map become empty once null content is suppressed
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void stringListWithEmptyStringOnly() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Arrays.asList("", ""));
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void stringListKeepsNonEmpty() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Arrays.asList(null, "keep", ""));
        assertEquals("""
                {"myList":["keep"]}""",
                MAPPER.writeValueAsString(bean));
    }

    @Test
    public void intListWithNullsOnly() throws Exception {
        IntListBean bean = new IntListBean();
        bean.values = new ArrayList<>(Arrays.asList(null, null));
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void intListKeepsValues() throws Exception {
        IntListBean bean = new IntListBean();
        bean.values = new ArrayList<>(Arrays.asList(null, 42));
        assertEquals("""
                {"values":[42]}""",
                MAPPER.writeValueAsString(bean));
    }

    @Test
    public void stringSetWithNullsOnly() throws Exception {
        StringSetBean bean = new StringSetBean();
        bean.values = new LinkedHashSet<>(Arrays.asList((String) null));
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: same handling for String arrays
    @Test
    public void stringArrayEmptyAfterContentFilter() throws Exception {
        StringArrayBean bean = new StringArrayBean();
        bean.values = new String[] { null, "" };
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void stringArrayKeepsNonEmpty() throws Exception {
        StringArrayBean bean = new StringArrayBean();
        bean.values = new String[] { null, "keep", "" };
        assertEquals("""
                {"values":["keep"]}""",
                MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: same handling for Object arrays
    @Test
    public void objectArrayNullsOnly() throws Exception {
        IntArrayBean bean = new IntArrayBean();
        bean.values = new Integer[] { null, null };
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void objectArrayKeepsValues() throws Exception {
        IntArrayBean bean = new IntArrayBean();
        bean.values = new Integer[] { null, 42 };
        assertEquals("""
                {"values":[42]}""",
                MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: same handling for Iterable
    @Test
    public void iterableEmptyAfterContentFilter() throws Exception {
        IterableBean bean = new IterableBean();
        bean.values = new StringIterable(null, "");
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void iterableKeepsNonEmpty() throws Exception {
        IterableBean bean = new IterableBean();
        bean.values = new StringIterable(null, "keep", "");
        assertEquals("""
                {"values":["keep"]}""",
                MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: primitive arrays, all-default content suppressed -> property dropped
    @Test
    public void primitiveArraysAllDefault() throws Exception {
        PrimArraysBean bean = new PrimArraysBean();
        bean.ints = new int[] { 0, 0 };
        bean.longs = new long[] { 0L };
        bean.shorts = new short[] { 0, 0 };
        bean.doubles = new double[] { 0.0, 0.0 };
        bean.floats = new float[] { 0.0f };
        bean.bools = new boolean[] { false, false };
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void primitiveArraysKeepNonDefault() throws Exception {
        PrimArraysBean bean = new PrimArraysBean();
        bean.ints = new int[] { 0, 42 };
        bean.longs = new long[] { 0L, 7L };
        bean.doubles = new double[] { 0.0, 1.5 };
        bean.bools = new boolean[] { false, true };
        assertEquals("""
                {"bools":[true],"doubles":[1.5],"ints":[42],"longs":[7]}""",
                MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: EnumSet, all elements suppressed by CUSTOM filter -> property dropped
    @Test
    public void enumSetAllSuppressed() throws Exception {
        EnumSetBean bean = new EnumSetBean();
        bean.values = EnumSet.of(Size.SMALL);
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void enumSetKeepsNonSuppressed() throws Exception {
        EnumSetBean bean = new EnumSetBean();
        bean.values = EnumSet.of(Size.SMALL, Size.LARGE);
        assertEquals("""
                {"values":["LARGE"]}""",
                MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: nested containers -- inner List that is empty after content
    // filtering is itself suppressed as an element of the outer List
    @Test
    public void nestedListsAllEmptyAfterContentFilter() throws Exception {
        NestedListBean bean = new NestedListBean();
        bean.values = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("", "")),
                new ArrayList<>(Collections.singletonList((String) null)),
                new ArrayList<>()));
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void nestedListsKeepNonEmpty() throws Exception {
        NestedListBean bean = new NestedListBean();
        bean.values = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("", "")),
                new ArrayList<>(Arrays.asList(null, "keep"))));
        assertEquals("""
                {"values":[["keep"]]}""",
                MAPPER.writeValueAsString(bean));
    }

    @Test
    public void mapOfListsAllEmptyAfterContentFilter() throws Exception {
        MapOfListsBean bean = new MapOfListsBean();
        bean.values = new LinkedHashMap<>();
        bean.values.put("a", new ArrayList<>(Arrays.asList("", "")));
        bean.values.put("b", new ArrayList<>());
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void mapOfListsKeepsNonEmpty() throws Exception {
        MapOfListsBean bean = new MapOfListsBean();
        bean.values = new LinkedHashMap<>();
        bean.values.put("a", new ArrayList<>(Collections.singletonList("")));
        bean.values.put("b", new ArrayList<>(Arrays.asList(null, "keep")));
        assertEquals("""
                {"values":{"b":["keep"]}}""",
                MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: content = NON_NULL suppresses nulls only; container with
    // nothing but nulls becomes empty, but empty Strings/zeroes are retained
    @Test
    public void nonNullContentAllNulls() throws Exception {
        NonNullContentBean bean = new NonNullContentBean();
        bean.strings = new ArrayList<>(Arrays.asList(null, null));
        bean.numbers = new ArrayList<>(Collections.singletonList((Integer) null));
        bean.stringArray = new String[] { null };
        bean.numberArray = new Integer[] { null, null };
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void nonNullContentRetainsEmptyValues() throws Exception {
        NonNullContentBean bean = new NonNullContentBean();
        bean.strings = new ArrayList<>(Arrays.asList(null, ""));
        bean.numbers = new ArrayList<>(Arrays.asList(null, 0));
        bean.stringArray = new String[] { null, "" };
        bean.numberArray = new Integer[] { null, 0 };
        assertEquals("""
                {"numberArray":[0],"numbers":[0],"stringArray":[""],"strings":[""]}""",
                MAPPER.writeValueAsString(bean));
    }

    // Without the feature, containers are left intact (only null containers dropped)
    @Test
    public void featureDisabledLeavesContainers() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Collections.singletonList(null));
        assertEquals("""
                {"myList":[null]}""",
                NO_FEATURE.writeValueAsString(bean));
    }
}
