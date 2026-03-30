package tools.jackson.databind.ser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SerializationFeature#ORDER_SET_ELEMENTS}
 * and {@link SerializationFeature#FAIL_ON_ORDER_SET_BY_INCOMPARABLE_ELEMENT}.
 */
public class CollectionSerializationOrderTest extends DatabindTestUtil
{
    // Helper bean wrapping Set<String> — routes through StringCollectionSerializer
    static class StringSetBean {
        public Set<String> values;

        StringSetBean(Set<String> v) { values = v; }
    }

    // Non-Comparable type for testing incomparable scenarios
    static class NonComparable {
        public final String name;

        NonComparable(String n) { name = n; }
    }

    // Custom String serializer that wraps output in angle brackets — used to verify
    // StaticListSerializerBase.createContextual() fallback to CollectionSerializer
    static class WrappingStringSerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator g, SerializationContext ctxt) {
            g.writeString("<" + value + ">");
        }
    }

    // Bean with custom content serializer to trigger fallback path
    static class CustomSerStringSetBean {
        @JsonSerialize(contentUsing = WrappingStringSerializer.class)
        public Set<String> values;

        CustomSerStringSetBean(Set<String> v) { values = v; }
    }

    private ObjectMapper orderedMapper() {
        return jsonMapperBuilder()
                .enable(SerializationFeature.ORDER_SET_ELEMENTS)
                .build();
    }

    /*
    /**********************************************************************
    /* Test methods: basic ordering
    /**********************************************************************
     */

    // #1: Feature disabled (default) → no sorting applied
    @Test
    public void testFeatureDisabledNoSorting() throws Exception {
        ObjectMapper mapper = newJsonMapper(); // default: feature disabled
        // LinkedHashSet preserves insertion order: c, a, b
        Set<String> set = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        assertEquals("[\"c\",\"a\",\"b\"]", mapper.writeValueAsString(set));
    }

    // #2: HashSet<Integer> sorted numerically (CollectionSerializer path)
    @Test
    public void testOrderedHashSetWithIntegers() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(3, 1, 2));
        assertEquals("[1,2,3]", mapper.writeValueAsString(set));
    }

    // #3: Set<String> bean property — verifies StringCollectionSerializer path
    @Test
    public void testSetStringBeanProperty() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<String> set = new LinkedHashSet<>(Arrays.asList("z", "a", "m"));
        assertEquals(a2q("{'values':['a','m','z']}"),
                mapper.writeValueAsString(new StringSetBean(set)));
    }

    // #4: SortedSet is already sorted — no additional sorting needed
    @Test
    public void testSortedSetUnchanged() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<String> set = new TreeSet<>(Arrays.asList("c", "a", "b"));
        assertEquals("[\"a\",\"b\",\"c\"]", mapper.writeValueAsString(set));
    }

    // #4b: SortedSet with custom comparator — preserve comparator order
    @Test
    public void testSortedSetWithCustomComparatorPreserved() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<String> set = new TreeSet<>(Comparator.reverseOrder());
        set.addAll(Arrays.asList("a", "b", "c"));
        // Even with ordering enabled, SortedSet keeps its own comparator order
        assertEquals("[\"c\",\"b\",\"a\"]", mapper.writeValueAsString(set));
    }

    // #5: EnumSet preserves ordinal order — not re-sorted
    @Test
    public void testEnumSetOrderUnchanged() throws Exception {
        ObjectMapper mapper = orderedMapper();
        // EnumSet iteration is always by ordinal, verify it stays that way
        Set<Thread.State> set = EnumSet.of(
                Thread.State.TERMINATED, Thread.State.NEW, Thread.State.RUNNABLE);
        String json = mapper.writeValueAsString(set);
        // NEW(0), RUNNABLE(1), TERMINATED(5) — ordinal order
        assertEquals("[\"NEW\",\"RUNNABLE\",\"TERMINATED\"]", json);
    }

    // #6: empty set — no issues
    @Test
    public void testEmptySet() throws Exception {
        ObjectMapper mapper = orderedMapper();
        assertEquals("[]", mapper.writeValueAsString(new HashSet<>()));
    }

    /*
    /**********************************************************************
    /* Test methods: incomparable elements
    /**********************************************************************
     */

    // #7: non-Comparable elements + FAIL enabled → exception
    @Test
    public void testNonComparableElementsFail() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.ORDER_SET_ELEMENTS)
                .enable(SerializationFeature.FAIL_ON_ORDER_SET_BY_INCOMPARABLE_ELEMENT)
                .build();
        Set<Object> set = new LinkedHashSet<>();
        set.add(new NonComparable("x"));
        set.add(new NonComparable("y"));
        assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(set));
    }

    // #8: non-Comparable elements + FAIL disabled → skip sorting, original order
    @Test
    public void testNonComparableElementsSkip() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.ORDER_SET_ELEMENTS)
                .disable(SerializationFeature.FAIL_ON_ORDER_SET_BY_INCOMPARABLE_ELEMENT)
                .build();
        Set<Object> set = new LinkedHashSet<>();
        set.add(new NonComparable("x"));
        set.add(new NonComparable("y"));
        // Sorting skipped; LinkedHashSet insertion order preserved
        assertEquals(a2q("[{'name':'x'},{'name':'y'}]"),
                mapper.writeValueAsString(set));
    }

    // #9: null elements in Set → nullsLast sorting (CollectionSerializer path — root level)
    @Test
    public void testSetWithNullElements() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<String> set = new LinkedHashSet<>(Arrays.asList(null, "b", "a"));
        // nulls sorted to end
        assertEquals("[\"a\",\"b\",null]", mapper.writeValueAsString(set));
    }

    // #10: DayOfWeek (original issue use case)
    @Test
    public void testDayOfWeekSet() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<java.time.DayOfWeek> set = new LinkedHashSet<>(Arrays.asList(
                java.time.DayOfWeek.FRIDAY,
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.WEDNESDAY));
        String json = mapper.writeValueAsString(set);
        // DayOfWeek implements Comparable; natural order is MONDAY < WEDNESDAY < FRIDAY
        assertEquals("[\"MONDAY\",\"WEDNESDAY\",\"FRIDAY\"]", json);
    }

    /*
    /**********************************************************************
    /* Test methods: non-Set collections not affected
    /**********************************************************************
     */

    // #11: List is NOT affected (Set-specific feature)
    @Test
    public void testListNotAffected() throws Exception {
        ObjectMapper mapper = orderedMapper();
        List<String> list = Arrays.asList("c", "a", "b");
        // List order preserved, not sorted
        assertEquals("[\"c\",\"a\",\"b\"]", mapper.writeValueAsString(list));
    }

    // #12: LinkedHashSet (insertion order) → sorting overrides insertion order
    @Test
    public void testLinkedHashSetSorted() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<String> set = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        assertEquals("[\"a\",\"b\",\"c\"]", mapper.writeValueAsString(set));
    }

    /*
    /**********************************************************************
    /* Test methods: polymorphic and edge cases
    /**********************************************************************
     */

    // #13: @JsonTypeInfo + Set → serializeWithType() path also sorts
    @Test
    public void testPolymorphicSetSorted() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.ORDER_SET_ELEMENTS)
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL)
                .build();
        Set<String> set = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        String json = mapper.writeValueAsString(set);
        // With default typing, output includes type info but elements should be sorted
        assertTrue(json.contains("\"a\""));
        // Verify order: a before b before c
        int posA = json.indexOf("\"a\"");
        int posB = json.indexOf("\"b\"");
        int posC = json.indexOf("\"c\"");
        assertTrue(posA < posB && posB < posC,
                "Elements should be sorted: " + json);
    }

    // #14: mixed incomparable types (Set<Object> with String + Integer)
    @Test
    public void testMixedIncomparableTypes() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.ORDER_SET_ELEMENTS)
                .disable(SerializationFeature.FAIL_ON_ORDER_SET_BY_INCOMPARABLE_ELEMENT)
                .build();
        Set<Object> set = new LinkedHashSet<>();
        set.add("hello");
        set.add(42);
        // ClassCastException caught, sorting skipped, insertion order preserved
        assertEquals("[\"hello\",42]", mapper.writeValueAsString(set));
    }

    // #15: null + multiple non-Comparable → ClassCastException caught, skip
    @Test
    public void testNullAndNonComparableMixed() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.ORDER_SET_ELEMENTS)
                .disable(SerializationFeature.FAIL_ON_ORDER_SET_BY_INCOMPARABLE_ELEMENT)
                .build();
        Set<Object> set = new LinkedHashSet<>();
        set.add(null);
        set.add(new NonComparable("x"));
        set.add(new NonComparable("y"));
        // ClassCastException caught, sorting skipped, insertion order preserved
        assertEquals(a2q("[null,{'name':'x'},{'name':'y'}]"),
                mapper.writeValueAsString(set));
    }

    // #16: Set<String> bean property with null → StringCollectionSerializer path nullsLast
    @Test
    public void testSetStringBeanPropertyWithNull() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<String> set = new LinkedHashSet<>(Arrays.asList(null, "b", "a"));
        // Bean property routes through StringCollectionSerializer; nulls sorted to end
        assertEquals(a2q("{'values':['a','b',null]}"),
                mapper.writeValueAsString(new StringSetBean(set)));
    }

    // #17: Set<String> + custom serializer → fallback to CollectionSerializer, still sorted
    @Test
    public void testSetStringWithCustomSerializerFallback() throws Exception {
        ObjectMapper mapper = orderedMapper();
        Set<String> set = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        CustomSerStringSetBean bean = new CustomSerStringSetBean(set);
        String json = mapper.writeValueAsString(bean);
        // Custom serializer wraps in <>, AND elements are sorted
        assertEquals(a2q("{'values':['<a>','<b>','<c>']}"), json);
    }
}
