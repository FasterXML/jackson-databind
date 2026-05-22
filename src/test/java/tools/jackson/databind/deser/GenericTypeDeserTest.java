package tools.jackson.databind.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/**
 * Tests for deserialization of generic types: generic collections,
 * generic value wrappers, recursive wildcards, and multi-param wildcards.
 */
public class GenericTypeDeserTest
{
    /*
    /**********************************************************************
    /* Helper classes for generic value tests
    /**********************************************************************
     */

    static abstract class BaseNumberBean<T extends Number>
    {
        public abstract void setNumber(T value);
    }

    static class NumberBean
        extends BaseNumberBean<Long>
    {
        long _number;

        @Override
        public void setNumber(Long value)
        {
            _number = value.intValue();
        }
    }

    static class SimpleBean
    {
        public int x;
    }

    static class Wrapper<T>
    {
        public T value;

        public Wrapper() { }

        public Wrapper(T v) { value = v; }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Wrapper<?> w) && (w.value.equals(value));
        }
    }

    /*
    /**********************************************************************
    /* Helper classes for recursive wildcard tests [databind#4118]
    /**********************************************************************
     */

    static class Tree<T extends Tree<?>> {

        final List<T> children;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Tree(List<T> children) {
            if (!children.stream().allMatch(c -> c instanceof Tree<?>)) {
                throw new IllegalArgumentException("Incorrect type");
            }
            this.children = children;
        }
    }

    static class TestAttribute4118<T extends TestAttribute4118<?>> {

        public List<T> attributes;

        public TestAttribute4118() {
        }

        public TestAttribute4118(List<T> attributes) {
            this.attributes = attributes;
        }
    }

    static class TestObject4118 {

        public List<TestAttribute4118<?>> attributes = new ArrayList<>();

        public TestObject4118() {
        }

        public TestObject4118(List<TestAttribute4118<?>> attributes) {
            this.attributes = attributes;
        }
    }

    /*
    /**********************************************************************
    /* Helper classes for multi-param wildcard tests [databind#4147]
    /**********************************************************************
     */

    // Simulates Either<L, R> pattern
    static class Pair<L, R> {
        public L left;
        public R right;

        public Pair() {}
        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }
    }

    static class PairContainer {
        public List<Pair<String, ?>> pairs;

        public PairContainer() {}
        public PairContainer(List<Pair<String, ?>> pairs) {
            this.pairs = pairs;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, generic values/wrappers
    /**********************************************************************
     */

    @Test
    public void testSimpleNumberBean() throws Exception
    {
        NumberBean result = MAPPER.readValue("{\"number\":17}", NumberBean.class);
        assertEquals(17, result._number);
    }

    /**
     * Unit test for verifying fix to [JACKSON-109].
     */
    @Test
    public void testGenericWrapper() throws Exception
    {
        Wrapper<SimpleBean> result = MAPPER.readValue
            ("{\"value\": { \"x\" : 13 } }",
             new TypeReference<Wrapper<SimpleBean>>() { });
        assertNotNull(result);
        assertEquals(Wrapper.class, result.getClass());
        Object contents = result.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(13, bean.x);
    }

    @Test
    public void testGenericWrapperWithSingleElementArray() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
        Wrapper<SimpleBean> result = mapper.readValue
            ("[{\"value\": [{ \"x\" : 13 }] }]",
             new TypeReference<Wrapper<SimpleBean>>() { });
        assertNotNull(result);
        assertEquals(Wrapper.class, result.getClass());
        Object contents = result.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(13, bean.x);
    }

    // Test for verifying that we can use different
    // type bindings for individual generic types.
    @Test
    public void testMultipleWrappers() throws Exception
    {
        // First, numeric wrapper
        Wrapper<Boolean> result = MAPPER.readValue
            ("{\"value\": true}", new TypeReference<Wrapper<Boolean>>() { });
        assertEquals(new Wrapper<Boolean>(Boolean.TRUE), result);

        // Then string one
        Wrapper<String> result2 = MAPPER.readValue
            ("{\"value\": \"abc\"}", new TypeReference<Wrapper<String>>() { });
        assertEquals(new Wrapper<String>("abc"), result2);

        // And then number
        Wrapper<Long> result3 = MAPPER.readValue
            ("{\"value\": 7}", new TypeReference<Wrapper<Long>>() { });
        assertEquals(new Wrapper<Long>(7L), result3);
    }

    //[databind#381]
    @Test
    public void testMultipleWrappersSingleValueArray() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

        // First, numeric wrapper
        Wrapper<Boolean> result = mapper.readValue
            ("[{\"value\": [true]}]", new TypeReference<Wrapper<Boolean>>() { });
        assertEquals(new Wrapper<Boolean>(Boolean.TRUE), result);

        // Then string one
        Wrapper<String> result2 = mapper.readValue
            ("[{\"value\": [\"abc\"]}]", new TypeReference<Wrapper<String>>() { });
        assertEquals(new Wrapper<String>("abc"), result2);

        // And then number
        Wrapper<Long> result3 = mapper.readValue
            ("[{\"value\": [7]}]", new TypeReference<Wrapper<Long>>() { });
        assertEquals(new Wrapper<Long>(7L), result3);
    }

    /**
     * Unit test for verifying fix to [JACKSON-109].
     */
    @Test
    public void testArrayOfGenericWrappers() throws Exception
    {
        Wrapper<SimpleBean>[] result = MAPPER.readValue
            ("[ {\"value\": { \"x\" : 9 } } ]",
             new TypeReference<Wrapper<SimpleBean>[]>() { });
        assertNotNull(result);
        assertEquals(Wrapper[].class, result.getClass());
        assertEquals(1, result.length);
        Wrapper<SimpleBean> elem = result[0];
        Object contents = elem.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(9, bean.x);
    }

    // [Issue#381]
    @Test
    public void testArrayOfGenericWrappersSingleValueArray() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

        Wrapper<SimpleBean>[] result = mapper.readValue
            ("[ {\"value\": [ { \"x\" : [ 9 ] } ] } ]",
             new TypeReference<Wrapper<SimpleBean>[]>() { });
        assertNotNull(result);
        assertEquals(Wrapper[].class, result.getClass());
        assertEquals(1, result.length);
        Wrapper<SimpleBean> elem = result[0];
        Object contents = elem.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(9, bean.x);
    }

    /*
    /**********************************************************************
    /* Test methods, recursive wildcards [databind#4118]
    /**********************************************************************
     */

    // for [databind#4118]
    @Test
    void recursiveWildcard4118() throws Exception {
        Tree<?> tree = MAPPER.readValue("[[[]]]", new TypeReference<Tree<?>>() {
        });

        assertEquals(1, tree.children.size());
        assertEquals(1, tree.children.get(0).children.size());
        assertEquals(0, tree.children.get(0).children.get(0).children.size());
    }

    // for [databind#4118]
    @Test
    void deserWildcard4118() throws Exception {
        // Given
        TestAttribute4118<?> a = new TestAttribute4118<>(null);
        TestAttribute4118<?> b = new TestAttribute4118<>(Arrays.asList(a));
        TestAttribute4118<?> c = new TestAttribute4118<>(Arrays.asList(b));
        TestObject4118 test = new TestObject4118(Arrays.asList(c));

        String serialized = MAPPER.writeValueAsString(test);

        // When
        TestObject4118 deserialized = MAPPER.readValue(serialized, TestObject4118.class);

        // Then
        assertInstanceOf(TestAttribute4118.class, deserialized.attributes.get(0).attributes.get(0));
    }

    /*
    /**********************************************************************
    /* Test methods, multi-param wildcards [databind#4147]
    /**********************************************************************
     */

    // for [databind#4147]
    @Test
    void multiParamWithWildcard() throws Exception {
        String json = a2q("{'pairs':[{'left':'test','right':123}]}");
        PairContainer result = MAPPER.readValue(json, PairContainer.class);

        assertNotNull(result.pairs);
        assertEquals(1, result.pairs.size());
        assertEquals("test", result.pairs.get(0).left);
        assertNotNull(result.pairs.get(0).right);
        // Right side should be deserialized as Integer (not LinkedHashMap)
        assertEquals(Integer.class, result.pairs.get(0).right.getClass());
        assertEquals(123, result.pairs.get(0).right);
    }

    // Additional test: both parameters are wildcards
    @Test
    void multiParamWithBothWildcards() throws Exception {
        String json = a2q("{'left':'hello','right':456}");

        Pair<?, ?> result = MAPPER.readValue(json, Pair.class);

        assertNotNull(result);
        assertEquals("hello", result.left);
        assertEquals(456, result.right);
    }
}
