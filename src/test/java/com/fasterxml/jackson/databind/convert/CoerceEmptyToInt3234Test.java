package com.fasterxml.jackson.databind.convert;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3234]
public class CoerceEmptyToInt3234Test
    extends DatabindTestUtil
{
    static class BasicIntWrapper {
        public int value = 13;
    }

    static class BasicLongWrapper {
        public long value = 7L;
    }

    static class BasicDoubleWrapper {
        public double value = -1.25;
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            // 08-Jan-2025, tatu: Need to allow null-to-int coercion for some tests
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
    private final ObjectReader READER_INT_BASIC = MAPPER.readerFor(BasicIntWrapper.class);
    private final ObjectReader READER_LONG_BASIC = MAPPER.readerFor(BasicLongWrapper.class);
    private final ObjectReader READER_DOUBLE_BASIC = MAPPER.readerFor(BasicDoubleWrapper.class);

    // // // Ints

    @Test
    public void testSimpleIntFromEmpty() throws Exception
    {
        BasicIntWrapper w = READER_INT_BASIC.readValue(a2q("{'value':''}"));
        assertEquals(0, w.value);
    }

    @Test
    public void testSimpleIntFromBlank() throws Exception
    {
        BasicIntWrapper w = READER_INT_BASIC.readValue(a2q("{'value':' '}"));
        assertEquals(0, w.value);
    }

    // // // Long

    @Test
    public void testSimpleLongFromEmpty() throws Exception
    {
        BasicLongWrapper w = READER_LONG_BASIC.readValue(a2q("{'value':''}"));
        assertEquals(0L, w.value);
    }

    @Test
    public void testSimpleLongFromBlank() throws Exception
    {
        BasicLongWrapper w = READER_LONG_BASIC.readValue(a2q("{'value':' '}"));
        assertEquals(0L, w.value);
    }

    // // // Double

    @Test
    public void testSimpleDoublegFromEmpty() throws Exception
    {
        BasicDoubleWrapper w = READER_DOUBLE_BASIC.readValue(a2q("{'value':''}"));
        assertEquals((double) 0, w.value);
    }

    @Test
    public void testSimpleDoubleFromBlank() throws Exception
    {
        BasicDoubleWrapper w = READER_DOUBLE_BASIC.readValue(a2q("{'value':' '}"));
        assertEquals((double) 0, w.value);
    }
}
