package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestArrayOfFloats extends BaseMapTest
{

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testArrayOfFloats() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder sb = new StringBuilder();
        sb.append('[')
                .append("\"7.038531e-26\",")
                .append("\"1.199999988079071\",")
                .append("\"3.4028235677973366e38\",")
                .append("\"7.006492321624086e-46\"")
                .append(']');
        float[] floats = mapper.readValue(sb.toString(), float[].class);
        assertEquals(4, floats.length);
        assertEquals(7.038531e-26f, floats[0]);
        assertEquals(1.1999999f, floats[1]);
        assertEquals(3.4028235e38f, floats[2]);
        assertEquals("1.4E-45", Float.toString(floats[3])); //this assertion fails unless toString is used
    }
}
