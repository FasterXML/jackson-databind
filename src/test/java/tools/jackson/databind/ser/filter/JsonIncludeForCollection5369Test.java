package tools.jackson.databind.ser.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5369] Support `@JsonInclude` for collection
public class JsonIncludeForCollection5369Test
    extends DatabindTestUtil
{
    static class FooFilter {
        @Override
        public boolean equals(Object other) {
            if (other == null) { // do NOT filter out nulls
                return false;
            }
            // in fact, only filter out exact String "foo"
            return "foo".equals(other);
        }
    }

    static class FooListBean {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public List<String> items = new ArrayList<String>();

        public FooListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // Test NON_NULL content inclusion
    static class NonNullListBean {
        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        public List<String> items = new ArrayList<String>();

        public NonNullListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // Test NON_EMPTY content inclusion
    static class NonEmptyListBean {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<String> items = new ArrayList<String>();

        public NonEmptyListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // Test NON_DEFAULT content inclusion
    static class NonDefaultListBean {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public List<String> items = new ArrayList<String>();

        public NonDefaultListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // Test with different collection types
    static class FooSetBean {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public Set<String> items = new LinkedHashSet<String>();

        public FooSetBean add(String value) {
            items.add(value);
            return this;
        }
    }

    // Test with Integer values
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
        public List<Integer> numbers = new ArrayList<Integer>();

        public NumberListBean add(Integer value) {
            numbers.add(value);
            return this;
        }
    }

    // Test counting filter behavior
    static class CountingFooFilter {
        public final static AtomicInteger counter = new AtomicInteger(0);

        @Override
        public boolean equals(Object other) {
            counter.incrementAndGet();
            return "foo".equals(other);
        }
    }

    static class CountingFooListBean {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = CountingFooFilter.class)
        public List<String> items = new ArrayList<String>();

        public CountingFooListBean add(String value) {
            items.add(value);
            return this;
        }
    }

    /*
    /**********************************************************
    /* Test methods, success
    /**********************************************************
     */

    final private ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
            .build();

    @Test
    public void testCustomFilterWithList() throws Exception {
        FooListBean input = new FooListBean()
                .add("1")
                .add("foo")
                .add("2");

        assertEquals(a2q("{'items':['1','2']}"), MAPPER.writeValueAsString(input));
    }

    @Test
    public void testNonNullContentInclusion() throws Exception {
        NonNullListBean input = new NonNullListBean()
                .add("1")
                .add(null)
                .add("2");

        assertEquals(a2q("{'items':['1','2']}"), MAPPER.writeValueAsString(input));
    }

    @Test
    public void testNonEmptyContentInclusion() throws Exception {
        NonEmptyListBean input = new NonEmptyListBean()
                .add("1")
                .add("")
                .add("2");

        assertEquals(a2q("{'items':['1','2']}"), MAPPER.writeValueAsString(input));
    }

    @Test
    public void testNonDefaultContentInclusion() throws Exception {
        NonDefaultListBean input = new NonDefaultListBean()
                .add("1")
                .add(null) // null is default for String
                .add("2");

        assertEquals(a2q("{'items':['1','2']}"), MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithSet() throws Exception {
        FooSetBean input = new FooSetBean()
                .add("1")
                .add("foo")
                .add("2");

        assertEquals(a2q("{'items':['1','2']}"), MAPPER.writeValueAsString(input));
    }

    @Test
    public void testCustomFilterWithNumbers() throws Exception {
        NumberListBean input = new NumberListBean()
                .add(1)
                .add(42)
                .add(3);

        assertEquals(a2q("{'numbers':[1,3]}"), MAPPER.writeValueAsString(input));
    }

    @Test
    public void testEmptyListWithCustomFilter() throws Exception {
        FooListBean input = new FooListBean();
        assertEquals(a2q("{'items':[]}"), MAPPER.writeValueAsString(input));
    }

    @Test
    public void testAllFilteredOut() throws Exception {
        FooListBean input = new FooListBean()
                .add("foo")
                .add("foo")
                .add("foo");

        assertEquals(a2q("{'items':[]}"), MAPPER.writeValueAsString(input));
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
        assertEquals(a2q("{'items':['1',null,'2',null]}"), MAPPER.writeValueAsString(input));
    }


}