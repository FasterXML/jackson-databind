package tools.jackson.databind.ser.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.Base64Variants;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class NumberSerTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = sharedMapper();

    private final ObjectMapper BINARY_VECTOR_MAPPER = jsonMapperBuilder()
            .withConfigOverride(float[].class,
                    c -> c.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.BINARY)))
            .withConfigOverride(double[].class,
                    c -> c.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.BINARY)))
            .build();

    private final ObjectMapper NON_EMPTY_MAPPER = jsonMapperBuilder()
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY))
            .build()
            ;

    static class IntWrapper {
        public int i;
        public IntWrapper(int value) { i = value; }
    }

    static class DoubleWrapper {
        public double value;
        public DoubleWrapper(double v) { value = v; }
    }

    static class BigDecimalWrapper {
        public BigDecimal value;
        public BigDecimalWrapper(BigDecimal v) { value = v; }
    }

    static class IntAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        @JsonProperty("value")
        public int foo = 3;
    }

    static class LongAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public long value = 4;
    }

    static class DoubleAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public double value = -0.5;
    }

    static class BigIntegerAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public BigInteger value = BigInteger.valueOf(123456L);
    }

    static class BigDecimalAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public BigDecimal value;

        public BigDecimalAsString() { this(BigDecimal.valueOf(0.25)); }
        public BigDecimalAsString(BigDecimal v) { value = v; }
    }

    static class NumberWrapper {
        // ensure it will use `Number` as statically force type, when looking for serializer
        @JsonSerialize(as=Number.class)
        public Number value;

        public NumberWrapper(Number v) { value = v; }
    }

    static class BigDecimalHolder {
        private final BigDecimal value;

        public BigDecimalHolder(String num) {
            value = new BigDecimal(num);
        }

        public BigDecimal getValue() {
            return value;
        }
    }

    static class BigDecimalAsStringSerializer extends ValueSerializer<BigDecimal> {
        private final DecimalFormat df = createDecimalFormatForDefaultLocale("0.0");

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext serializers) {
            gen.writeString(df.format(value));
        }
    }

    static class BigDecimalAsNumberSerializer extends ValueSerializer<BigDecimal> {
        private final DecimalFormat df = createDecimalFormatForDefaultLocale("0.0");

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext serializers) {
            gen.writeNumber(df.format(value));
        }
    }

    @SuppressWarnings("serial")
    static class MyBigDecimal extends BigDecimal {
        public MyBigDecimal(String value) {
            super(value);
        }
    }

    static class Bean2519Typed {
        public List<BigDecimal> values = new ArrayList<>();
    }

    static class Bean2519Untyped {
        public Collection<BigDecimal> values = new HashSet<>();
    }

    // // // Inner types from VectorsAsBinarySerTest

    private final static float[] FLOAT_VECTOR = new float[] { 1.0f, 0.5f, -1.25f };
    private final static String FLOAT_VECTOR_STR = "[1.0,0.5,-1.25]";

    private final static double[] DOUBLE_VECTOR = new double[] { -1.0, 1.5, 0.0125 };
    private final static String DOUBLE_VECTOR_STR = "[-1.0,1.5,0.0125]";

    static class BeanWithArrayFloatVector {
        @JsonFormat(shape = JsonFormat.Shape.NATURAL)
        public float[] vector;

        protected BeanWithArrayFloatVector() { }
        public BeanWithArrayFloatVector(float[] v) { vector = v; }
    }

    static class BeanWithBinaryFloatVector {
        @JsonFormat(shape = JsonFormat.Shape.BINARY)
        public float[] vector;

        protected BeanWithBinaryFloatVector() { }
        public BeanWithBinaryFloatVector(float[] v) { vector = v; }
    }

    static class BeanWithArrayDoubleVector {
        @JsonFormat(shape = JsonFormat.Shape.NATURAL)
        public double[] vector;

        protected BeanWithArrayDoubleVector() { }
        public BeanWithArrayDoubleVector(double[] v) { vector = v; }
    }

    static class BeanWithBinaryDoubleVector {
        @JsonFormat(shape = JsonFormat.Shape.BINARY)
        public double[] vector;

        protected BeanWithBinaryDoubleVector() { }
        public BeanWithBinaryDoubleVector(double[] v) { vector = v; }
    }

    /*
    /**********************************************************************
    /* Test methods: short/int/long/BigInteger
    /**********************************************************************
     */

    @Test
    public void testShortArray() throws Exception
    {
        assertEquals("[0,1]", MAPPER.writeValueAsString(new short[] { 0, 1 }));
        assertEquals("[2,3]", MAPPER.writeValueAsString(new Short[] { 2, 3 }));
    }

    @Test
    public void testIntArray() throws Exception
    {
        assertEquals("[0,-3]", MAPPER.writeValueAsString(new int[] { 0, -3 }));
        assertEquals("[13,9]", MAPPER.writeValueAsString(new Integer[] { 13, 9 }));
    }

    @Test
    public void testLongArray() throws Exception
    {
        assertEquals("[-123,42]", MAPPER.writeValueAsString(new long[] { -123, 42 }));
        assertEquals("[123,-999]", MAPPER.writeValueAsString(new Long[] { 123L, -999L }));
    }

    @Test
    public void testBigInteger() throws Exception
    {
        BigInteger[] values = new BigInteger[] {
                BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO,
                BigInteger.valueOf(1234567890L),
                new BigInteger("123456789012345678901234568"),
                new BigInteger("-1250000124326904597090347547457")
                };

        for (BigInteger value : values) {
            String expected = value.toString();
            assertEquals(expected, MAPPER.writeValueAsString(value));
        }
    }
    
    /*
    /**********************************************************************
    /* Test methods, float/double/BigDecimal
    /**********************************************************************
     */

    /* Note: dealing with floating-point values is tricky; not sure if
     * we can really use equality tests here... JDK does have decent
     * conversions though, to retain accuracy and round-trippability.
     * But still...
     */
    @Test
    public void testFloat() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double d : values) {
           float f = (float) d;
        String expected = String.valueOf(f);
           if (Float.isNaN(f) || Float.isInfinite(f)) {
               expected = "\""+expected+"\"";
             }
           assertEquals(expected, MAPPER.writeValueAsString(Float.valueOf(f)));
        }
    }

    @Test
    public void testDouble() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double d : values) {
            String expected = String.valueOf(d);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                expected = "\""+d+"\"";
            }
            assertEquals(expected, MAPPER.writeValueAsString(Double.valueOf(d)));
        }
    }

    @Test
    public void testBigDecimal() throws Exception
    {
        Map<String, Object> map = new HashMap<String, Object>();
        String PI_STR = "3.14159265";
        map.put("pi", new BigDecimal(PI_STR));
        String str = MAPPER.writeValueAsString(map);
        assertEquals("{\"pi\":3.14159265}", str);
    }

    @Test
    public void testBigDecimalAsPlainString() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper(JsonFactory.builder()
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build());
        Map<String, Object> map = new HashMap<String, Object>();
        String PI_STR = "3.00000000";
        map.put("pi", new BigDecimal(PI_STR));
        String str = mapper.writeValueAsString(map);
        assertEquals("{\"pi\":3.00000000}", str);
    }

    @Test
    public void testBigIntegerAsPlainTest() throws Exception
    {
        final String NORM_VALUE = "0.0000000005";
        final BigDecimal BD_VALUE = new BigDecimal(NORM_VALUE);
        final BigDecimalAsString INPUT = new BigDecimalAsString(BD_VALUE);
        // by default, use the default `toString()`
        assertEquals("{\"value\":\""+BD_VALUE.toString()+"\"}", MAPPER.writeValueAsString(INPUT));

        // but can force to "plain" notation
        final ObjectMapper m = jsonMapperBuilder()
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();
        assertEquals("{\"value\":\""+NORM_VALUE+"\"}", m.writeValueAsString(INPUT));
    }

    @Test
    public void testBigDecimalAsString2519Typed() throws Exception
    {
        Bean2519Typed foo = new Bean2519Typed();
        foo.values.add(new BigDecimal("2.34"));
        final ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(BigDecimal.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
                .build();
        String json = mapper.writeValueAsString(foo);
        assertEquals(a2q("{'values':['2.34']}"), json);
    }

    @Test
    public void testBigDecimalAsString2519Untyped() throws Exception
    {
        Bean2519Untyped foo = new Bean2519Untyped();
        foo.values.add(new BigDecimal("2.34"));
        final ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(BigDecimal.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
                .build();
        String json = mapper.writeValueAsString(foo);
        assertEquals(a2q("{'values':['2.34']}"), json);
    }

    @Test
    public void testCustomSerializationBigDecimalAsString() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new BigDecimalAsStringSerializer());
        ObjectMapper mapper = jsonMapperBuilder().addModule(module).build();
        assertEquals(a2q("{'value':'2.0'}"), mapper.writeValueAsString(new BigDecimalHolder("2")));
    }

    @Test
    public void testCustomSerializationBigDecimalAsNumber() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new BigDecimalAsNumberSerializer());
        ObjectMapper mapper = jsonMapperBuilder().addModule(module).build();
        assertEquals(a2q("{'value':2.0}"), mapper.writeValueAsString(new BigDecimalHolder("2")));
    }

    /*
    /**********************************************************************
    /* Test methods, as-String
    /**********************************************************************
     */
    
    @Test
    public void testNumbersAsString() throws Exception
    {
        assertEquals(a2q("{'value':'3'}"), MAPPER.writeValueAsString(new IntAsString()));
        assertEquals(a2q("{'value':'4'}"), MAPPER.writeValueAsString(new LongAsString()));
        assertEquals(a2q("{'value':'-0.5'}"), MAPPER.writeValueAsString(new DoubleAsString()));
        assertEquals(a2q("{'value':'0.25'}"), MAPPER.writeValueAsString(new BigDecimalAsString()));
        assertEquals(a2q("{'value':'123456'}"), MAPPER.writeValueAsString(new BigIntegerAsString()));
    }

    @Test
    public void testNumbersAsStringNonEmpty() throws Exception
    {
        assertEquals(a2q("{'value':'3'}"), NON_EMPTY_MAPPER.writeValueAsString(new IntAsString()));
        assertEquals(a2q("{'value':'4'}"), NON_EMPTY_MAPPER.writeValueAsString(new LongAsString()));
        assertEquals(a2q("{'value':'-0.5'}"), NON_EMPTY_MAPPER.writeValueAsString(new DoubleAsString()));
        assertEquals(a2q("{'value':'0.25'}"), NON_EMPTY_MAPPER.writeValueAsString(new BigDecimalAsString()));
        assertEquals(a2q("{'value':'123456'}"), NON_EMPTY_MAPPER.writeValueAsString(new BigIntegerAsString()));
    }

    @Test
    public void testConfigOverridesForNumbers() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withAllConfigOverrides(all -> { // could have used separate but test for funsies
                    all.findOrCreateOverride(Integer.TYPE) // for `int`
                        .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
                    all.findOrCreateOverride(Double.TYPE) // for `double`
                        .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
                    all.findOrCreateOverride(BigDecimal.class)
                        .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
                })
                .build();

        assertEquals(a2q("{'i':'3'}"),
                mapper.writeValueAsString(new IntWrapper(3)));
        assertEquals(a2q("{'value':'0.75'}"),
                mapper.writeValueAsString(new DoubleWrapper(0.75)));
        assertEquals(a2q("{'value':'-0.5'}"),
                mapper.writeValueAsString(new BigDecimalWrapper(BigDecimal.valueOf(-0.5))));
    }

    @Test
    public void testNumberType() throws Exception
    {
        assertEquals(a2q("{'value':1}"), MAPPER.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 1))));
        assertEquals(a2q("{'value':2}"), MAPPER.writeValueAsString(new NumberWrapper(Short.valueOf((short) 2))));
        assertEquals(a2q("{'value':3}"), MAPPER.writeValueAsString(new NumberWrapper(Integer.valueOf(3))));
        assertEquals(a2q("{'value':4}"), MAPPER.writeValueAsString(new NumberWrapper(Long.valueOf(4L))));
        assertEquals(a2q("{'value':0.5}"), MAPPER.writeValueAsString(new NumberWrapper(Float.valueOf(0.5f))));
        assertEquals(a2q("{'value':0.05}"), MAPPER.writeValueAsString(new NumberWrapper(Double.valueOf(0.05))));
        assertEquals(a2q("{'value':123}"), MAPPER.writeValueAsString(new NumberWrapper(BigInteger.valueOf(123))));
        assertEquals(a2q("{'value':0.025}"), MAPPER.writeValueAsString(new NumberWrapper(BigDecimal.valueOf(0.025))));
    }

    @Test
    public void testConfigOverrideJdkNumber() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().withConfigOverride(BigDecimal.class,
                        c -> c.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
                .build();
        String value = mapper.writeValueAsString(new BigDecimal("123.456"));
        assertEquals(a2q("'123.456'"), value);
    }

    @Test
    public void testConfigOverrideNonJdkNumber() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().withConfigOverride(MyBigDecimal.class,
                c -> c.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
                .build();
        String value = mapper.writeValueAsString(new MyBigDecimal("123.456"));
        assertEquals(a2q("'123.456'"), value);
    }

    // default locale is en_US
    static DecimalFormat createDecimalFormatForDefaultLocale(final String pattern) {
        return new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.ENGLISH));
    }

    /*
    /**********************************************************************
    /* Test methods, float[]/double[] as binary vectors [databind#5242]
    /**********************************************************************
     */

    @Test
    public void defaultFloatVectorSerialization() throws Exception {
        String json = MAPPER.writeValueAsString(FLOAT_VECTOR);
        assertEquals(FLOAT_VECTOR_STR, json);

        float[] result = MAPPER.readValue(json, float[].class);
        assertArrayEquals(FLOAT_VECTOR, result);
    }

    @Test
    public void asArrayFloatVectorSerialization() throws Exception {
        final String exp = a2q("{'vector':"+FLOAT_VECTOR_STR+"}");
        String json = MAPPER.writeValueAsString(new BeanWithArrayFloatVector(FLOAT_VECTOR));
        assertEquals(exp, json);
        // And annotation overrides default shape override
        assertEquals(exp,
                BINARY_VECTOR_MAPPER.writeValueAsString(new BeanWithArrayFloatVector(FLOAT_VECTOR)));

        BeanWithArrayFloatVector result = MAPPER.readValue(json, BeanWithArrayFloatVector.class);
        assertArrayEquals(FLOAT_VECTOR, result.vector);
    }

    @Test
    public void asBinaryFloatVectorSerializationRoot() throws Exception {
        String json = BINARY_VECTOR_MAPPER.writeValueAsString(FLOAT_VECTOR);
        assertEquals(q(base64Encode(asBinary(FLOAT_VECTOR))), json);

        float[] result = BINARY_VECTOR_MAPPER.readValue(json, float[].class);
        assertArrayEquals(FLOAT_VECTOR, result);
    }

    @Test
    public void asBinaryFloatVectorSerializationPOJO() throws Exception {
        String json = MAPPER.writeValueAsString(new BeanWithBinaryFloatVector(FLOAT_VECTOR));
        assertEquals(a2q("{'vector':'"+base64Encode(asBinary(FLOAT_VECTOR))+"'}"), json);

        BeanWithArrayFloatVector result = MAPPER.readValue(json, BeanWithArrayFloatVector.class);
        assertArrayEquals(FLOAT_VECTOR, result.vector);
    }

    @Test
    public void defaultDoubleVectorSerialization() throws Exception {
        String json = MAPPER.writeValueAsString(DOUBLE_VECTOR);
        assertEquals(DOUBLE_VECTOR_STR, json);

        double[] result = MAPPER.readValue(json, double[].class);
        assertArrayEquals(DOUBLE_VECTOR, result);
    }

    @Test
    public void asArrayDoubleVectorSerialization() throws Exception {
        String exp = a2q("{'vector':"+DOUBLE_VECTOR_STR+"}");
        String json = MAPPER.writeValueAsString(new BeanWithArrayDoubleVector(DOUBLE_VECTOR));
        assertEquals(exp, json);
        // And annotation overrides default shape override
        assertEquals(exp,
                BINARY_VECTOR_MAPPER.writeValueAsString(new BeanWithArrayDoubleVector(DOUBLE_VECTOR)));

        BeanWithArrayDoubleVector result = MAPPER.readValue(json, BeanWithArrayDoubleVector.class);
        assertArrayEquals(DOUBLE_VECTOR, result.vector);
    }

    @Test
    public void asBinaryDoubleVectorSerializationRoot() throws Exception {
        String json = BINARY_VECTOR_MAPPER.writeValueAsString(DOUBLE_VECTOR);
        assertEquals(q(base64Encode(asBinary(DOUBLE_VECTOR))), json);

        double[] result = BINARY_VECTOR_MAPPER.readValue(json, double[].class);
        assertArrayEquals(DOUBLE_VECTOR, result);
    }

    @Test
    public void asBinaryDoubleVectorSerializationPOJO() throws Exception {
        String json = MAPPER.writeValueAsString(new BeanWithBinaryDoubleVector(DOUBLE_VECTOR));
        assertEquals(a2q("{'vector':'"+base64Encode(asBinary(DOUBLE_VECTOR))+"'}"), json);

        BeanWithBinaryDoubleVector result = MAPPER.readValue(json, BeanWithBinaryDoubleVector.class);
        assertArrayEquals(DOUBLE_VECTOR, result.vector);
    }

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
