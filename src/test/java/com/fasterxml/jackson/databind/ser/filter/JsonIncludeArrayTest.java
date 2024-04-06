package com.fasterxml.jackson.databind.ser.filter;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class JsonIncludeArrayTest extends DatabindTestUtil
{
    static class NonEmptyByteArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public byte[] value;

        public NonEmptyByteArray(byte... v) { value = v; }
    }

    static class NonEmptyShortArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public short[] value;

        public NonEmptyShortArray(short... v) { value = v; }
    }

    static class NonEmptyCharArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public char[] value;

        public NonEmptyCharArray(char... v) { value = v; }
    }

    static class NonEmptyIntArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public int[] value;

        public NonEmptyIntArray(int... v) { value = v; }
    }

    static class NonEmptyLongArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public long[] value;

        public NonEmptyLongArray(long... v) { value = v; }
    }

    static class NonEmptyBooleanArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public boolean[] value;

        public NonEmptyBooleanArray(boolean... v) { value = v; }
    }

    static class NonEmptyDoubleArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public double[] value;

        public NonEmptyDoubleArray(double... v) { value = v; }
    }

    static class NonEmptyFloatArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public float[] value;

        public NonEmptyFloatArray(float... v) { value = v; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testByteArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyByteArray()));
    }

    @Test
    public void testShortArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyShortArray()));
        assertEquals("{\"value\":[1]}", MAPPER.writeValueAsString(new NonEmptyShortArray((short) 1)));
    }

    @Test
    public void testCharArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyCharArray()));
        // by default considered to be serialized as String
        assertEquals("{\"value\":\"ab\"}", MAPPER.writeValueAsString(new NonEmptyCharArray('a', 'b')));
        // but can force as sparse (real) array too
        assertEquals("{\"value\":[\"a\",\"b\"]}", MAPPER
                .writer().with(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)
                .writeValueAsString(new NonEmptyCharArray('a', 'b')));
    }

    @Test
    public void testIntArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyIntArray()));
        assertEquals("{\"value\":[2]}", MAPPER.writeValueAsString(new NonEmptyIntArray(2)));
    }

    @Test
    public void testLongArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyLongArray()));
        assertEquals("{\"value\":[3,4]}", MAPPER.writeValueAsString(new NonEmptyLongArray(3, 4)));
    }

    @Test
    public void testBooleanArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyBooleanArray()));
        assertEquals("{\"value\":[true,false]}", MAPPER.writeValueAsString(new NonEmptyBooleanArray(true,false)));
    }

    @Test
    public void testDoubleArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyDoubleArray()));
        assertEquals("{\"value\":[0.25,-1.0]}", MAPPER.writeValueAsString(new NonEmptyDoubleArray(0.25,-1.0)));
    }

    @Test
    public void testFloatArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyFloatArray()));
        assertEquals("{\"value\":[0.5]}", MAPPER.writeValueAsString(new NonEmptyFloatArray(0.5f)));
    }
}
