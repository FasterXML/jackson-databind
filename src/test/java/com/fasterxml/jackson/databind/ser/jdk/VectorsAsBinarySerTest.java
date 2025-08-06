package com.fasterxml.jackson.databind.ser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for serialization (and deserialization) of {@code float[]}
 * as "packed binary" data, as per [databind#5242].
 */
public class VectorsAsBinarySerTest extends DatabindTestUtil
{
    private final static float[] FLOAT_VECTOR = new float[] { 1.0f, 0.5f, -1.25f };
    private final static String FLOAT_VECTOR_STR = "[1.0,0.5,-1.25]";
    
    private final static double[] DOUBLE_VECTOR = new double[] { -1.0, 1.5, 0.0125 };
    private final static String DOUBLE_VECTOR_STR = "[-1.0,1.5,0.0125]";

    private final ObjectMapper MAPPER = sharedMapper();

    // // // Float Vector tests
    
    @Test
    public void defaultFloatVectorSerialization() throws Exception {
        String json = MAPPER.writeValueAsString(FLOAT_VECTOR);
        assertEquals(FLOAT_VECTOR_STR, json);

        float[] result = MAPPER.readValue(json, float[].class);
        assertArrayEquals(FLOAT_VECTOR, result);
    }

    // // // Double Vector tests
    
    @Test
    public void defaultDoubleVectorSerialization() throws Exception {
        String json = MAPPER.writeValueAsString(DOUBLE_VECTOR);
        assertEquals(DOUBLE_VECTOR_STR, json);

        double[] result = MAPPER.readValue(json, double[].class);
        assertArrayEquals(DOUBLE_VECTOR, result);
    }
}
