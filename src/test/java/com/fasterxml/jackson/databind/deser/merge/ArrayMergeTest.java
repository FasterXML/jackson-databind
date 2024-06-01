package com.fasterxml.jackson.databind.deser.merge;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;


import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class ArrayMergeTest
{
    static class MergedX<T>
    {
        @JsonMerge(OptBoolean.TRUE)
        public T value;

        public MergedX(T v) { value = v; }
        protected MergedX() { }
    }

    // [databind#4121]
    static class Merged4121
    {
        @JsonMerge(OptBoolean.TRUE)
        public Date[] value;
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            // 26-Oct-2016, tatu: Make sure we'll report merge problems by default
            .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
            .build();

    @Test
    public void testObjectArrayMerging() throws Exception
    {
        MergedX<Object[]> input = new MergedX<Object[]>(new Object[] {
                "foo"
        });
        final JavaType type = MAPPER.getTypeFactory().constructType(new TypeReference<MergedX<Object[]>>() {});
        MergedX<Object[]> result = MAPPER.readerFor(type)
                .withValueToUpdate(input)
                .readValue(a2q("{'value':['bar']}"));
        assertSame(input, result);
        assertEquals(2, result.value.length);
        assertEquals("foo", result.value[0]);
        assertEquals("bar", result.value[1]);

        // and with one trick
        result = MAPPER.readerFor(type)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .withValueToUpdate(input)
                .readValue(a2q("{'value':'zap'}"));
        assertSame(input, result);
        assertEquals(3, result.value.length);
        assertEquals("foo", result.value[0]);
        assertEquals("bar", result.value[1]);
        assertEquals("zap", result.value[2]);
    }

    // [databind#4121]
    @Test
    public void testComponentTypeArrayMerging() throws Exception
    {
        Merged4121 input = new Merged4121();
        input.value = new Date[] {new Date(1000L)};
        Merged4121 result = MAPPER.readerFor(Merged4121.class)
                .withValueToUpdate(input)
                .readValue(a2q("{'value':[2000]}"));
        assertSame(input, result);
        assertEquals(2, result.value.length);
        assertEquals(1000L, result.value[0].getTime());
        assertEquals(2000L, result.value[1].getTime());

        // and with one trick
        result = MAPPER.readerFor(Merged4121.class)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .withValueToUpdate(input)
                .readValue(a2q("{'value':3000}"));
        assertSame(input, result);
        assertEquals(3, result.value.length);
        assertEquals(1000L, result.value[0].getTime());
        assertEquals(2000L, result.value[1].getTime());
        assertEquals(3000L, result.value[2].getTime());
    }

    @Test
    public void testStringArrayMerging() throws Exception
    {
        MergedX<String[]> input = new MergedX<String[]>(new String[] { "foo" });
        MergedX<String[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<String[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':['bar']}"));
        assertSame(input, result);
        assertEquals(2, result.value.length);
        assertEquals("foo", result.value[0]);
        assertEquals("bar", result.value[1]);
    }

    @Test
    public void testBooleanArrayMerging() throws Exception
    {
        MergedX<boolean[]> input = new MergedX<boolean[]>(new boolean[] { true, false });
        MergedX<boolean[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<boolean[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':[true]}"));
        assertSame(input, result);
        assertEquals(3, result.value.length);
        assertArrayEquals(new boolean[] { true, false, true }, result.value);
    }

    @Test
    public void testByteArrayMerging() throws Exception
    {
        MergedX<byte[]> input = new MergedX<byte[]>(new byte[] { 1, 2 });
        MergedX<byte[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<byte[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':[4, 6.0, null]}"));
        assertSame(input, result);
        assertEquals(5, result.value.length);
        assertArrayEquals(new byte[] { 1, 2, 4, 6, 0 }, result.value);
    }

    @Test
    public void testShortArrayMerging() throws Exception
    {
        MergedX<short[]> input = new MergedX<short[]>(new short[] { 1, 2 });
        MergedX<short[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<short[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':[4, 6]}"));
        assertSame(input, result);
        assertEquals(4, result.value.length);
        assertArrayEquals(new short[] { 1, 2, 4, 6 }, result.value);
    }

    @Test
    public void testCharArrayMerging() throws Exception
    {
        MergedX<char[]> input = new MergedX<char[]>(new char[] { 'a', 'b' });
        MergedX<char[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<char[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':['c']}"));
        assertSame(input, result);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result.value);

        // also some variation
        input = new MergedX<char[]>(new char[] { });
        result = MAPPER
                .readerFor(new TypeReference<MergedX<char[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':['c']}"));
        assertSame(input, result);
        assertArrayEquals(new char[] { 'c' }, result.value);
    }

    @Test
    public void testIntArrayMerging() throws Exception
    {
        MergedX<int[]> input = new MergedX<int[]>(new int[] { 1, 2 });
        MergedX<int[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<int[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':[4, 6]}"));
        assertSame(input, result);
        assertEquals(4, result.value.length);
        assertArrayEquals(new int[] { 1, 2, 4, 6 }, result.value);

        // also some variation
        input = new MergedX<int[]>(new int[] { 3, 4, 6 });
        result = MAPPER
                .readerFor(new TypeReference<MergedX<int[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':[ ]}"));
        assertSame(input, result);
        assertArrayEquals(new int[] { 3, 4, 6 }, result.value);
    }

    @Test
    public void testLongArrayMerging() throws Exception
    {
        MergedX<long[]> input = new MergedX<long[]>(new long[] { 1, 2 });
        MergedX<long[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<long[]>>() {})
                .withValueToUpdate(input)
                .readValue(a2q("{'value':[4, 6]}"));
        assertSame(input, result);
        assertEquals(4, result.value.length);
        assertArrayEquals(new long[] { 1, 2, 4, 6 }, result.value);
    }
}
