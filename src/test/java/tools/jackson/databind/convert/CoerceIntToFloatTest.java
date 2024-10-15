package tools.jackson.databind.convert;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.type.LogicalType;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class CoerceIntToFloatTest
{
    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Float, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .build();

    private final ObjectMapper MAPPER_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Float, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert))
            .build();

    private final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Float, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.AsNull))
            .build();

    private final ObjectMapper MAPPER_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Float, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.AsEmpty))
            .build();

    private final ObjectMapper LEGACY_SCALAR_COERCION_FAIL = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    @Test
    public void testDefaultIntToFloatCoercion() throws Exception
    {
        assertSuccessfulIntToFloatConversionsWith(DEFAULT_MAPPER);
    }

    @Test
    public void testCoerceConfigToConvert() throws Exception
    {
        assertSuccessfulIntToFloatConversionsWith(MAPPER_TRY_CONVERT);
    }

    @Test
    public void testCoerceConfigToNull() throws Exception
    {
        assertNull(MAPPER_TO_NULL.readValue("1", Float.class));
        // `null` not possible for primitives, must use empty (aka default) value
        assertEquals(0.0f, MAPPER_TO_NULL.readValue("-2", Float.TYPE));
        {
            FloatWrapper w = MAPPER_TO_NULL.readValue("{\"f\": -5}", FloatWrapper.class);
            assertEquals(0.0f, w.f);
            float[] arr = MAPPER_TO_NULL.readValue("[ 2 ]", float[].class);
            assertEquals(1, arr.length);
            assertEquals(0.0f, arr[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue("-1", Double.class));
        assertEquals(0.0d, MAPPER_TO_NULL.readValue("4", Double.TYPE));
        {
            DoubleWrapper w = MAPPER_TO_NULL.readValue("{\"d\": 2}", DoubleWrapper.class);
            assertEquals(0.0d, w.d);
            double[] arr = MAPPER_TO_NULL.readValue("[ -7 ]", double[].class);
            assertEquals(1, arr.length);
            assertEquals(0.0d, arr[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue("420", BigDecimal.class));
        {
            BigDecimal[] arr = MAPPER_TO_NULL.readValue("[ 420 ]", BigDecimal[].class);
            assertEquals(1, arr.length);
            assertNull(arr[0]);
        }
    }

    @Test
    public void testCoerceConfigToEmpty() throws Exception
    {
        assertEquals(0.0f, MAPPER_TO_EMPTY.readValue("3", Float.class));
        assertEquals(0.0f, MAPPER_TO_EMPTY.readValue("-2", Float.TYPE));
        {
            FloatWrapper w = MAPPER_TO_EMPTY.readValue("{\"f\": -5}", FloatWrapper.class);
            assertEquals(0.0f, w.f);
            float[] arr = MAPPER_TO_EMPTY.readValue("[ 2 ]", float[].class);
            assertEquals(1, arr.length);
            assertEquals(0.0f, arr[0]);
        }

        assertEquals(0.0d, MAPPER_TO_EMPTY.readValue("-1", Double.class));
        assertEquals(0.0d, MAPPER_TO_EMPTY.readValue("-5", Double.TYPE));
        {
            DoubleWrapper w = MAPPER_TO_EMPTY.readValue("{\"d\": 2}", DoubleWrapper.class);
            assertEquals(0.0d, w.d);
            double[] arr = MAPPER_TO_EMPTY.readValue("[ -2 ]", double[].class);
            assertEquals(1, arr.length);
            assertEquals(0.0d, arr[0]);
        }

        assertEquals(BigDecimal.valueOf(0), MAPPER_TO_EMPTY.readValue("3643", BigDecimal.class));
    }

    @Test
    public void testCoerceConfigToFail() throws Exception
    {
        _verifyCoerceFail(MAPPER_TO_FAIL, Float.class, "3");
        _verifyCoerceFail(MAPPER_TO_FAIL, Float.TYPE, "-2");
        _verifyCoerceFail(MAPPER_TO_FAIL, FloatWrapper.class, "{\"f\": -5}", "float");
        _verifyCoerceFail(MAPPER_TO_FAIL, float[].class, "[ 2 ]", "to `float` value");

        _verifyCoerceFail(MAPPER_TO_FAIL, Double.class, "-1");
        _verifyCoerceFail(MAPPER_TO_FAIL, Double.TYPE, "4");
        _verifyCoerceFail(MAPPER_TO_FAIL, DoubleWrapper.class, "{\"d\": 2}", "double");
        _verifyCoerceFail(MAPPER_TO_FAIL, double[].class, "[ -2 ]", "to `double` value");

        _verifyCoerceFail(MAPPER_TO_FAIL, BigDecimal.class, "73455342");
    }

    @Test
    public void testLegacyConfiguration() throws Exception
    {
        assertSuccessfulIntToFloatConversionsWith(LEGACY_SCALAR_COERCION_FAIL);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void assertSuccessfulIntToFloatConversionsWith(ObjectMapper objectMapper)
            throws Exception
    {
        assertEquals(3.0f, objectMapper.readValue("3", Float.class));
        assertEquals(-2.0f, objectMapper.readValue("-2", Float.TYPE));
        {
            FloatWrapper w = objectMapper.readValue("{\"f\": -5}", FloatWrapper.class);
            assertEquals(-5.0f, w.f);
            float[] arr = objectMapper.readValue("[ 2 ]", float[].class);
            assertEquals(2.0f, arr[0]);
        }

        assertEquals(-1.0d, objectMapper.readValue("-1", Double.class));
        assertEquals(4.0d, objectMapper.readValue("4", Double.TYPE));
        {
            DoubleWrapper w = objectMapper.readValue("{\"d\": 2}", DoubleWrapper.class);
            assertEquals(2.0d, w.d);
            double[] arr = objectMapper.readValue("[ -2 ]", double[].class);
            assertEquals(-2.0d, arr[0]);
        }

        BigDecimal biggie = objectMapper.readValue("423451233", BigDecimal.class);
        assertEquals(BigDecimal.valueOf(423451233.0d), biggie);
    }

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
                                   String doc) throws Exception
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetType.getName());
    }

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
                                   String doc, String targetTypeDesc) throws Exception
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetTypeDesc);
    }

    private void _verifyCoerceFail(ObjectReader r, Class<?> targetType,
                                   String doc, String targetTypeDesc) throws Exception
    {
        try {
            r.forType(targetType).readValue(doc);
            fail("Should not accept Integer for "+targetType.getName()+" when configured to");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce Integer");
            verifyException(e, targetTypeDesc);
        }
    }
}
