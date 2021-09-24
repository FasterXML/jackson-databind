package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.databind.*;

// [databind#3234]
public class CoerceEmptyToInt3234Test extends BaseMapTest
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

    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectReader READER_INT_BASIC = MAPPER.readerFor(BasicIntWrapper.class);
    private final ObjectReader READER_LONG_BASIC = MAPPER.readerFor(BasicLongWrapper.class);
    private final ObjectReader READER_DOUBLE_BASIC = MAPPER.readerFor(BasicDoubleWrapper.class);

    // // // Ints

    public void testSimpleIntFromEmpty() throws Exception
    {
        BasicIntWrapper w = READER_INT_BASIC.readValue(a2q("{'value':''}"));
        assertEquals(0, w.value);
    }

    public void testSimpleIntFromBlank() throws Exception
    {
        BasicIntWrapper w = READER_INT_BASIC.readValue(a2q("{'value':' '}"));
        assertEquals(0, w.value);
    }

    // // // Long

    public void testSimpleLongFromEmpty() throws Exception
    {
        BasicLongWrapper w = READER_LONG_BASIC.readValue(a2q("{'value':''}"));
        assertEquals(0L, w.value);
    }

    public void testSimpleLongFromBlank() throws Exception
    {
        BasicLongWrapper w = READER_LONG_BASIC.readValue(a2q("{'value':' '}"));
        assertEquals(0L, w.value);
    }

    // // // Double

    public void testSimpleDoublegFromEmpty() throws Exception
    {
        BasicDoubleWrapper w = READER_DOUBLE_BASIC.readValue(a2q("{'value':''}"));
        assertEquals((double) 0, w.value);
    }

    public void testSimpleDoubleFromBlank() throws Exception
    {
        BasicDoubleWrapper w = READER_DOUBLE_BASIC.readValue(a2q("{'value':' '}"));
        assertEquals((double) 0, w.value);
    }
}
