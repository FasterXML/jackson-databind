package tools.jackson.databind.ser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.Base64Variants;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

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

    static class BeanWithArrayFloatVector {
        @JsonFormat(shape = JsonFormat.Shape.NATURAL) // or ARRAY
        public float[] vector;

        protected BeanWithArrayFloatVector() { }
        public BeanWithArrayFloatVector(float[] v) {
            vector = v;
        }
    }

    static class BeanWithBinaryFloatVector {
        @JsonFormat(shape = JsonFormat.Shape.BINARY)
        public float[] vector;

        protected BeanWithBinaryFloatVector() { }
        public BeanWithBinaryFloatVector(float[] v) {
            vector = v;
        }
    }

    static class BeanWithArrayDoubleVector {
        @JsonFormat(shape = JsonFormat.Shape.NATURAL) // or ARRAY
        public double[] vector;

        protected BeanWithArrayDoubleVector() { }
        public BeanWithArrayDoubleVector(double[] v) {
            vector = v;
        }
    }

    static class BeanWithBinaryDoubleVector {
        @JsonFormat(shape = JsonFormat.Shape.BINARY)
        public double[] vector;

        protected BeanWithBinaryDoubleVector() { }
        public BeanWithBinaryDoubleVector(double[] v) {
            vector = v;
        }
    }

    private final ObjectMapper VANILLA_MAPPER = sharedMapper();

    private final ObjectMapper BINARY_VECTOR_MAPPER = jsonMapperBuilder()
            .withConfigOverride(float[].class,
                    c -> c.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.BINARY)))
            .withConfigOverride(double[].class,
                    c -> c.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.BINARY)))
            .build();

    // // // Float Vector tests, as-Array

    @Test
    public void defaultFloatVectorSerialization() throws Exception {
        String json = VANILLA_MAPPER.writeValueAsString(FLOAT_VECTOR);
        assertEquals(FLOAT_VECTOR_STR, json);

        float[] result = VANILLA_MAPPER.readValue(json, float[].class);
        assertArrayEquals(FLOAT_VECTOR, result);
    }

    @Test
    public void asArrayFloatVectorSerialization() throws Exception {
        final String exp = a2q("{'vector':"+FLOAT_VECTOR_STR+"}");
        String json = VANILLA_MAPPER.writeValueAsString(new BeanWithArrayFloatVector(FLOAT_VECTOR));
        assertEquals(exp, json);
        // And annotation overrides default shape override
        assertEquals(exp,
                BINARY_VECTOR_MAPPER.writeValueAsString(new BeanWithArrayFloatVector(FLOAT_VECTOR)));

        BeanWithArrayFloatVector result = VANILLA_MAPPER.readValue(json, BeanWithArrayFloatVector.class);
        assertArrayEquals(FLOAT_VECTOR, result.vector);
    }

    // // // Float Vector tests, as-Binary

    @Test
    public void asBinaryFloatVectorSerializationRoot() throws Exception {
        String json = BINARY_VECTOR_MAPPER.writeValueAsString(FLOAT_VECTOR);
        assertEquals(q(base64Encode(asBinary(FLOAT_VECTOR))), json);

        float[] result = BINARY_VECTOR_MAPPER.readValue(json, float[].class);
        assertArrayEquals(FLOAT_VECTOR, result);
    }

    @Test
    public void asBinaryFloatVectorSerializationPOJO() throws Exception {
        String json = VANILLA_MAPPER.writeValueAsString(new BeanWithBinaryFloatVector(FLOAT_VECTOR));
        assertEquals(a2q("{'vector':'"+base64Encode(asBinary(FLOAT_VECTOR))+"'}"), json);

        BeanWithArrayFloatVector result = VANILLA_MAPPER.readValue(json, BeanWithArrayFloatVector.class);
        assertArrayEquals(FLOAT_VECTOR, result.vector);
    }
    
    // // // Double Vector tests, as-Array

    @Test
    public void defaultDoubleVectorSerialization() throws Exception {
        String json = VANILLA_MAPPER.writeValueAsString(DOUBLE_VECTOR);
        assertEquals(DOUBLE_VECTOR_STR, json);

        double[] result = VANILLA_MAPPER.readValue(json, double[].class);
        assertArrayEquals(DOUBLE_VECTOR, result);
    }

    @Test
    public void asArrayDoubleVectorSerialization() throws Exception {
        String exp = a2q("{'vector':"+DOUBLE_VECTOR_STR+"}");
        String json = VANILLA_MAPPER.writeValueAsString(new BeanWithArrayDoubleVector(DOUBLE_VECTOR));
        assertEquals(exp, json);
        // And annotation overrides default shape override
        assertEquals(exp,
                BINARY_VECTOR_MAPPER.writeValueAsString(new BeanWithArrayDoubleVector(DOUBLE_VECTOR)));

        BeanWithArrayDoubleVector result = VANILLA_MAPPER.readValue(json, BeanWithArrayDoubleVector.class);
        assertArrayEquals(DOUBLE_VECTOR, result.vector);
    }

    // // // Double Vector tests, as-Binary

    @Test
    public void asBinaryDoubleVectorSerializationRoot() throws Exception {
        String json = BINARY_VECTOR_MAPPER.writeValueAsString(DOUBLE_VECTOR);
        assertEquals(q(base64Encode(asBinary(DOUBLE_VECTOR))), json);

        double[] result = BINARY_VECTOR_MAPPER.readValue(json, double[].class);
        assertArrayEquals(DOUBLE_VECTOR, result);
    }
    
    @Test
    public void asBinaryDoubleVectorSerializationPOJO() throws Exception {
        String json = VANILLA_MAPPER.writeValueAsString(new BeanWithBinaryDoubleVector(DOUBLE_VECTOR));
        assertEquals(a2q("{'vector':'"+base64Encode(asBinary(DOUBLE_VECTOR))+"'}"), json);

        BeanWithBinaryDoubleVector result = VANILLA_MAPPER.readValue(json, BeanWithBinaryDoubleVector.class);
        assertArrayEquals(DOUBLE_VECTOR, result.vector);
    }

    // // // Helper methods

    private static byte[] asBinary(float[] vector) {
        byte[] result = new byte[vector.length * 4];
        for (int i = 0; i < vector.length; i++) {
            int bits = Float.floatToIntBits(vector[i]);
            result[i * 4] = (byte) (bits >> 24);
            result[i * 4 + 1] = (byte) (bits >> 16);
            result[i * 4 + 2] = (byte) (bits >> 8);
            result[i * 4 + 3] = (byte) bits;
        }
        return result;
    }

    private static byte[] asBinary(double[] vector) {
        byte[] result = new byte[vector.length * 8];
        for (int i = 0; i < vector.length; i++) {
            long bits = Double.doubleToLongBits(vector[i]);
            result[i * 8] = (byte) (bits >> 56);
            result[i * 8 + 1] = (byte) (bits >> 48);
            result[i * 8 + 2] = (byte) (bits >> 40);
            result[i * 8 + 3] = (byte) (bits >> 32);
            result[i * 8 + 4] = (byte) (bits >> 24);
            result[i * 8 + 5] = (byte) (bits >> 16);
            result[i * 8 + 6] = (byte) (bits >> 8);
            result[i * 8 + 7] = (byte) bits;
        }
        return result;
    }

    private String base64Encode(byte[] data) {
        return Base64Variants.getDefaultVariant().encode(data, false);
    }
}
