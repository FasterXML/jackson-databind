package tools.jackson.databind.ext.jdk8;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for Stream serializers covering special values,
 * wrapper-based serialization, contextual serialization with static
 * typing, and edge cases.
 */
public class AdditionalStreamSerializerTest
    extends DatabindTestUtil
{
    static class IntStreamWrapper {
        public IntStream value;

        public IntStreamWrapper() { }
        IntStreamWrapper(IntStream v) { value = v; }
    }

    static class LongStreamWrapper {
        public LongStream value;

        public LongStreamWrapper() { }
        LongStreamWrapper(LongStream v) { value = v; }
    }

    static class DoubleStreamWrapper {
        public DoubleStream value;

        public DoubleStreamWrapper() { }
        DoubleStreamWrapper(DoubleStream v) { value = v; }
    }

    static class StringStreamWrapper {
        public Stream<String> value;

        public StringStreamWrapper() { }
        StringStreamWrapper(Stream<String> v) { value = v; }
    }

    static final class FinalValueBean {
        public int x;

        public FinalValueBean() { }
        FinalValueBean(int x) { this.x = x; }

        @Override
        public boolean equals(Object o) {
            return (o instanceof FinalValueBean) && ((FinalValueBean) o).x == x;
        }

        @Override
        public int hashCode() { return x; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* IntStream additional tests
    /**********************************************************
     */

    @Test
    public void testIntStreamInWrapper() throws Exception
    {
        String json = MAPPER.writeValueAsString(
                new IntStreamWrapper(IntStream.of(10, 20, 30)));
        assertEquals(a2q("{'value':[10,20,30]}"), json);
    }

    @Test
    public void testIntStreamSingleNegative() throws Exception
    {
        String json = MAPPER.writeValueAsString(IntStream.of(-1));
        assertEquals("[-1]", json);
    }

    @Test
    public void testIntStreamBoundaryValues() throws Exception
    {
        int[] values = { Integer.MIN_VALUE, 0, Integer.MAX_VALUE };
        String json = MAPPER.writeValueAsString(IntStream.of(values));
        int[] result = MAPPER.readValue(json, int[].class);
        assertArrayEquals(values, result);
    }

    /*
    /**********************************************************
    /* LongStream additional tests
    /**********************************************************
     */

    @Test
    public void testLongStreamInWrapper() throws Exception
    {
        String json = MAPPER.writeValueAsString(
                new LongStreamWrapper(LongStream.of(100L, 200L, 300L)));
        assertEquals(a2q("{'value':[100,200,300]}"), json);
    }

    @Test
    public void testLongStreamSingleNegative() throws Exception
    {
        String json = MAPPER.writeValueAsString(LongStream.of(-1L));
        assertEquals("[-1]", json);
    }

    @Test
    public void testLongStreamBoundaryValues() throws Exception
    {
        long[] values = { Long.MIN_VALUE, 0L, Long.MAX_VALUE };
        String json = MAPPER.writeValueAsString(LongStream.of(values));
        long[] result = MAPPER.readValue(json, long[].class);
        assertArrayEquals(values, result);
    }

    /*
    /**********************************************************
    /* DoubleStream additional tests
    /**********************************************************
     */

    @Test
    public void testDoubleStreamInWrapper() throws Exception
    {
        String json = MAPPER.writeValueAsString(
                new DoubleStreamWrapper(DoubleStream.of(1.1, 2.2, 3.3)));
        assertEquals(a2q("{'value':[1.1,2.2,3.3]}"), json);
    }

    @Test
    public void testDoubleStreamSingleNegative() throws Exception
    {
        String json = MAPPER.writeValueAsString(DoubleStream.of(-1.5));
        assertEquals("[-1.5]", json);
    }

    @Test
    public void testDoubleStreamBoundaryValues() throws Exception
    {
        double[] values = { Double.MIN_VALUE, 0.0, Double.MAX_VALUE };
        String json = MAPPER.writeValueAsString(DoubleStream.of(values));
        double[] result = MAPPER.readValue(json, double[].class);
        assertArrayEquals(values, result, 0.0);
    }

    @Test
    public void testDoubleStreamEmpty() throws Exception
    {
        String json = MAPPER.writeValueAsString(
                new DoubleStreamWrapper(DoubleStream.empty()));
        assertEquals(a2q("{'value':[]}"), json);
    }

    /*
    /**********************************************************
    /* Generic Stream: contextual serialization
    /**********************************************************
     */

    @Test
    public void testStreamWithStaticTyping() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_STATIC_TYPING)
                .build();
        String json = mapper.writeValueAsString(
                new StringStreamWrapper(Stream.of("a", "b", "c")));
        assertEquals(a2q("{'value':['a','b','c']}"), json);
    }

    @Test
    public void testStreamWithFinalElementType() throws Exception
    {
        // FinalValueBean is a final class, so createContextual() should
        // resolve a specific content serializer even without USE_STATIC_TYPING
        Stream<FinalValueBean> stream = Stream.of(
                new FinalValueBean(1), new FinalValueBean(2));
        String json = MAPPER.writeValueAsString(stream);
        assertEquals(a2q("[{'x':1},{'x':2}]"), json);
    }

    @Test
    public void testStreamOfStrings() throws Exception
    {
        String json = MAPPER.writeValueAsString(Stream.of("hello", "world"));
        assertEquals("[\"hello\",\"world\"]", json);
    }

    @Test
    public void testStreamOfIntegers() throws Exception
    {
        String json = MAPPER.writeValueAsString(Stream.of(1, 2, 3));
        assertEquals("[1,2,3]", json);
    }

    /*
    /**********************************************************
    /* Stream: closure verification for wrapper contexts
    /**********************************************************
     */

    @Test
    public void testIntStreamInWrapperCloses() throws Exception
    {
        java.util.concurrent.atomic.AtomicBoolean closed =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        IntStream stream = IntStream.of(1, 2, 3).onClose(() -> closed.set(true));
        MAPPER.writeValueAsString(new IntStreamWrapper(stream));
        assertTrue(closed.get());
    }

    @Test
    public void testLongStreamInWrapperCloses() throws Exception
    {
        java.util.concurrent.atomic.AtomicBoolean closed =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        LongStream stream = LongStream.of(1L, 2L).onClose(() -> closed.set(true));
        MAPPER.writeValueAsString(new LongStreamWrapper(stream));
        assertTrue(closed.get());
    }

    @Test
    public void testDoubleStreamInWrapperCloses() throws Exception
    {
        java.util.concurrent.atomic.AtomicBoolean closed =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        DoubleStream stream = DoubleStream.of(1.0, 2.0).onClose(() -> closed.set(true));
        MAPPER.writeValueAsString(new DoubleStreamWrapper(stream));
        assertTrue(closed.get());
    }
}
