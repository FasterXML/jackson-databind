package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CoerceNaNStringToNumberTest extends BaseMapTest
{
    static class DoubleBean {
        double _v;
        public void setV(double v) { _v = v; }
    }

    static class FloatBean {
        float _v;
        public void setV(float v) { _v = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_NO_COERCION = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    /*
    /**********************************************************************
    /* Test methods for coercing from "NaN Strings": tricky edge case as
    /* NaNs are not legal JSON tokens by default (although Jackson has options
    /* to allow)... so with 2.12 we consider String as natural representation
    /* and not coercible. This may need to be resolved in future but for now
    /* this is needed for backwards-compatibility.
    /**********************************************************************
     */

    public void testDoublePrimitiveNonNumeric() throws Exception
    {
        // first, simple case:
        // bit tricky with binary fps but...
        double value = Double.POSITIVE_INFINITY;
        DoubleBean result = MAPPER.readValue("{\"v\":\""+value+"\"}", DoubleBean.class);
        assertEquals(value, result._v);

        // should work with arrays too..
        double[] array = MAPPER.readValue("[ \"Infinity\" ]", double[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(Double.POSITIVE_INFINITY, array[0]);
    }

    public void testDoublePrimFromNaNCoercionDisabled() throws Exception
    {
        // first, simple case:
        double value = Double.POSITIVE_INFINITY;
        DoubleBean result = MAPPER_NO_COERCION.readValue("{\"v\":\""+value+"\"}", DoubleBean.class);
        assertEquals(value, result._v);

        // should work with arrays too..
        double[] array = MAPPER_NO_COERCION.readValue("[ \"Infinity\" ]", double[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(Double.POSITIVE_INFINITY, array[0]);
    }

    public void testDoubleWrapperFromNaNCoercionDisabled() throws Exception
    {
        double value = Double.POSITIVE_INFINITY;
        Double dv = MAPPER_NO_COERCION.readValue(q(String.valueOf(value)), Double.class);
        assertTrue(dv.isInfinite());
    }

    public void testFloatPrimitiveNonNumeric() throws Exception
    {
        // bit tricky with binary fps but...
        float value = Float.POSITIVE_INFINITY;
        FloatBean result = MAPPER.readValue("{\"v\":\""+value+"\"}", FloatBean.class);
        assertEquals(value, result._v);

        // should work with arrays too..
        float[] array = MAPPER.readValue("[ \"Infinity\" ]", float[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(Float.POSITIVE_INFINITY, array[0]);
    }

    public void testFloatPriFromNaNCoercionDisabled() throws Exception
    {
        // first, simple case:
        float value = Float.POSITIVE_INFINITY;
        FloatBean result = MAPPER_NO_COERCION.readValue("{\"v\":\""+value+"\"}", FloatBean.class);
        assertEquals(value, result._v);

        // should work with arrays too..
        float[] array = MAPPER_NO_COERCION.readValue("[ \"Infinity\" ]", float[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(Float.POSITIVE_INFINITY, array[0]);
    }

    public void testFloatWrapperFromNaNCoercionDisabled() throws Exception
    {
        float value = Float.POSITIVE_INFINITY;
        Float dv = MAPPER_NO_COERCION.readValue(q(String.valueOf(value)), Float.class);
        assertTrue(dv.isInfinite());
    }
}
