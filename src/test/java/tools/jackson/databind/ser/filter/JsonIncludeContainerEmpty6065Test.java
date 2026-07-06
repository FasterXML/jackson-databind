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

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .build();

    private final ObjectMapper NO_FEATURE = JsonMapper.builder()
            .disable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .build();

    @Test
    public void testAllNull() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(new Bean()));
    }

    @Test
    public void testEmptyContainers() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>();
        bean.myMap = new HashMap<>();
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testContainersEmptyAfterContentFilter() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Collections.singletonList(null));
        bean.myMap = new HashMap<>();
        bean.myMap.put("1", null);
        // Both list and map become empty once null content is suppressed
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testStringListWithEmptyStringOnly() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Arrays.asList("", ""));
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testStringListKeepsNonEmpty() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Arrays.asList(null, "keep", ""));
        assertEquals("{\"myList\":[\"keep\"]}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testIntListWithNullsOnly() throws Exception {
        IntListBean bean = new IntListBean();
        bean.values = new ArrayList<>(Arrays.asList(null, null));
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testIntListKeepsValues() throws Exception {
        IntListBean bean = new IntListBean();
        bean.values = new ArrayList<>(Arrays.asList(null, 42));
        assertEquals("{\"values\":[42]}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testStringSetWithNullsOnly() throws Exception {
        StringSetBean bean = new StringSetBean();
        bean.values = new LinkedHashSet<>(Arrays.asList((String) null));
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: same handling for String arrays
    @Test
    public void testStringArrayEmptyAfterContentFilter() throws Exception {
        StringArrayBean bean = new StringArrayBean();
        bean.values = new String[] { null, "" };
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testStringArrayKeepsNonEmpty() throws Exception {
        StringArrayBean bean = new StringArrayBean();
        bean.values = new String[] { null, "keep", "" };
        assertEquals("{\"values\":[\"keep\"]}", MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: same handling for Object arrays
    @Test
    public void testObjectArrayNullsOnly() throws Exception {
        IntArrayBean bean = new IntArrayBean();
        bean.values = new Integer[] { null, null };
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testObjectArrayKeepsValues() throws Exception {
        IntArrayBean bean = new IntArrayBean();
        bean.values = new Integer[] { null, 42 };
        assertEquals("{\"values\":[42]}", MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: same handling for Iterable
    @Test
    public void testIterableEmptyAfterContentFilter() throws Exception {
        IterableBean bean = new IterableBean();
        bean.values = new StringIterable(null, "");
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testIterableKeepsNonEmpty() throws Exception {
        IterableBean bean = new IterableBean();
        bean.values = new StringIterable(null, "keep", "");
        assertEquals("{\"values\":[\"keep\"]}", MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: primitive arrays, all-default content suppressed -> property dropped
    @Test
    public void testPrimitiveArraysAllDefault() throws Exception {
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
    public void testPrimitiveArraysKeepNonDefault() throws Exception {
        PrimArraysBean bean = new PrimArraysBean();
        bean.ints = new int[] { 0, 42 };
        bean.longs = new long[] { 0L, 7L };
        bean.doubles = new double[] { 0.0, 1.5 };
        bean.bools = new boolean[] { false, true };
        assertEquals("{\"bools\":[true],\"doubles\":[1.5],\"ints\":[42],\"longs\":[7]}",
                MAPPER.writeValueAsString(bean));
    }

    // [databind#6065]: EnumSet, all elements suppressed by CUSTOM filter -> property dropped
    @Test
    public void testEnumSetAllSuppressed() throws Exception {
        EnumSetBean bean = new EnumSetBean();
        bean.values = EnumSet.of(Size.SMALL);
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testEnumSetKeepsNonSuppressed() throws Exception {
        EnumSetBean bean = new EnumSetBean();
        bean.values = EnumSet.of(Size.SMALL, Size.LARGE);
        assertEquals("{\"values\":[\"LARGE\"]}", MAPPER.writeValueAsString(bean));
    }

    // Without the feature, containers are left intact (only null containers dropped)
    @Test
    public void testFeatureDisabledLeavesContainers() throws Exception {
        Bean bean = new Bean();
        bean.myList = new ArrayList<>(Collections.singletonList(null));
        assertEquals("{\"myList\":[null]}", NO_FEATURE.writeValueAsString(bean));
    }
}
