package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.databind.*;

public class ScalarConversionTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#1433]
    public void testConvertValueNullPrimitive() throws Exception
    {
        assertEquals(Byte.valueOf((byte) 0), MAPPER.convertValue(null, Byte.TYPE));
        assertEquals(Short.valueOf((short) 0), MAPPER.convertValue(null, Short.TYPE));
        assertEquals(Integer.valueOf(0), MAPPER.convertValue(null, Integer.TYPE));
        assertEquals(Long.valueOf(0L), MAPPER.convertValue(null, Long.TYPE));
        assertEquals(Float.valueOf(0f), MAPPER.convertValue(null, Float.TYPE));
        assertEquals(Double.valueOf(0d), MAPPER.convertValue(null, Double.TYPE));
        assertEquals(Character.valueOf('\0'), MAPPER.convertValue(null, Character.TYPE));
        assertEquals(Boolean.FALSE, MAPPER.convertValue(null, Boolean.TYPE));
    }

    // [databind#1433]
    public void testConvertValueNullBoxed() throws Exception
    {
        assertNull(MAPPER.convertValue(null, Byte.class));
        assertNull(MAPPER.convertValue(null, Short.class));
        assertNull(MAPPER.convertValue(null, Integer.class));
        assertNull(MAPPER.convertValue(null, Long.class));
        assertNull(MAPPER.convertValue(null, Float.class));
        assertNull(MAPPER.convertValue(null, Double.class));
        assertNull(MAPPER.convertValue(null, Character.class));
        assertNull(MAPPER.convertValue(null, Boolean.class));
    }
}
