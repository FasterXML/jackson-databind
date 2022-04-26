package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TestFloats extends BaseMapTest
{

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testFloatPrimitive() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(7.038531e-26f, mapper.readValue("\"7.038531e-26\"", float.class));
        assertEquals(1.1999999f, mapper.readValue("\"1.199999988079071\"", float.class));
        assertEquals(3.4028235e38f, mapper.readValue("\"3.4028235677973366e38\"", float.class));
        //this assertion fails unless toString is used
        assertEquals("1.4E-45", mapper.readValue("\"7.006492321624086e-46\"", float.class).toString());
    }

    public void testFloatClass() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(Float.valueOf(7.038531e-26f), mapper.readValue("\"7.038531e-26\"", Float.class));
        assertEquals(Float.valueOf(1.1999999f), mapper.readValue("\"1.199999988079071\"", Float.class));
        assertEquals(Float.valueOf(3.4028235e38f), mapper.readValue("\"3.4028235677973366e38\"", Float.class));
        //this assertion fails unless toString is used
        assertEquals("1.4E-45", mapper.readValue("\"7.006492321624086e-46\"", Float.class).toString());
    }

    public void testArrayOfFloatPrimitives() throws Exception
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

    public void testBigArrayOfFloatPrimitives() throws Exception {
        StringBuilder sb = new StringBuilder(1024);
        ObjectMapper mapper = new ObjectMapper();
        try (
                InputStream stream = TestFloats.class.getResourceAsStream("/data/float-array.txt");
                InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8)
        ) {
            char[] chars = new char[1024];
            int n;
            while ((n = isr.read(chars)) != -1) {
                sb.append(chars, 0, n);
            }
        }
        float[] floats = mapper.readValue(sb.toString(), float[].class);
        assertEquals(1004, floats.length);
        assertEquals(7.038531e-26f, floats[0]);
        assertEquals(1.1999999f, floats[1]);
        assertEquals(3.4028235e38f, floats[2]);
        assertEquals("1.4E-45", Float.toString(floats[3])); //this assertion fails unless toString is used
    }


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
        Float[] floats = mapper.readValue(sb.toString(), Float[].class);
        assertEquals(4, floats.length);
        assertEquals(Float.valueOf(7.038531e-26f), floats[0]);
        assertEquals(Float.valueOf(1.1999999f), floats[1]);
        assertEquals(Float.valueOf(3.4028235e38f), floats[2]);
        assertEquals(Float.valueOf("1.4E-45"), floats[3]);
    }

}
