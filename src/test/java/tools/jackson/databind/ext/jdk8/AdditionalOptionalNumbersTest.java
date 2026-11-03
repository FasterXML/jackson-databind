package tools.jackson.databind.ext.jdk8;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for OptionalInt, OptionalLong, and OptionalDouble
 * covering UNWRAP_SINGLE_VALUE_ARRAYS, boundary values, serialization
 * filters, and coercion edge cases.
 */
public class AdditionalOptionalNumbersTest
    extends DatabindTestUtil
{
    static class OptionalIntBean {
        public OptionalInt value;

        public OptionalIntBean() { value = OptionalInt.empty(); }
        OptionalIntBean(int v) { this(OptionalInt.of(v)); }
        OptionalIntBean(OptionalInt v) { value = v; }
    }

    static class OptionalLongBean {
        public OptionalLong value;

        public OptionalLongBean() { value = OptionalLong.empty(); }
        OptionalLongBean(long v) { this(OptionalLong.of(v)); }
        OptionalLongBean(OptionalLong v) { value = v; }
    }

    static class OptionalDoubleBean {
        public OptionalDouble value;

        public OptionalDoubleBean() { value = OptionalDouble.empty(); }
        OptionalDoubleBean(double v) { this(OptionalDouble.of(v)); }
        OptionalDoubleBean(OptionalDouble v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* OptionalInt: boundary values
    /**********************************************************
     */

    @Test
    public void testOptionalIntBoundaryValues() throws Exception
    {
        OptionalIntBean maxBean = new OptionalIntBean(Integer.MAX_VALUE);
        String json = MAPPER.writeValueAsString(maxBean);
        OptionalIntBean result = MAPPER.readValue(json, OptionalIntBean.class);
        assertTrue(result.value.isPresent());
        assertEquals(Integer.MAX_VALUE, result.value.getAsInt());

        OptionalIntBean minBean = new OptionalIntBean(Integer.MIN_VALUE);
        json = MAPPER.writeValueAsString(minBean);
        result = MAPPER.readValue(json, OptionalIntBean.class);
        assertTrue(result.value.isPresent());
        assertEquals(Integer.MIN_VALUE, result.value.getAsInt());
    }

    @Test
    public void testOptionalIntZero() throws Exception
    {
        OptionalIntBean bean = new OptionalIntBean(0);
        String json = MAPPER.writeValueAsString(bean);
        OptionalIntBean result = MAPPER.readValue(json, OptionalIntBean.class);
        assertTrue(result.value.isPresent());
        assertEquals(0, result.value.getAsInt());
    }

    /*
    /**********************************************************
    /* OptionalInt: serialization filter
    /**********************************************************
     */

    @Test
    public void testOptionalIntSerializeFilter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        assertEquals(a2q("{'value':123}"),
                mapper.writeValueAsString(new OptionalIntBean(123)));
        // absent is not strictly null so still serialized
        assertEquals(a2q("{'value':null}"),
                mapper.writeValueAsString(new OptionalIntBean()));

        mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT))
                .build();
        assertEquals(a2q("{'value':456}"),
                mapper.writeValueAsString(new OptionalIntBean(456)));
        assertEquals(a2q("{}"),
                mapper.writeValueAsString(new OptionalIntBean()));
    }

    /*
    /**********************************************************
    /* OptionalInt: UNWRAP_SINGLE_VALUE_ARRAYS
    /**********************************************************
     */

    @Test
    public void testOptionalIntUnwrapSingleValueArrays() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
        OptionalInt result = mapper.readValue("[42]", OptionalInt.class);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testOptionalIntUnwrapSingleValueArraysDisabled() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("[42]", OptionalInt.class));
    }

    /*
    /**********************************************************
    /* OptionalInt: float-to-int coercion
    /**********************************************************
     */

    @Test
    public void testOptionalIntFromFloat() throws Exception
    {
        OptionalInt result = MAPPER.readValue("2.0", OptionalInt.class);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    /*
    /**********************************************************
    /* OptionalLong: UNWRAP_SINGLE_VALUE_ARRAYS
    /**********************************************************
     */

    @Test
    public void testOptionalLongUnwrapSingleValueArrays() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
        OptionalLong result = mapper.readValue("[99]", OptionalLong.class);
        assertTrue(result.isPresent());
        assertEquals(99L, result.getAsLong());
    }

    @Test
    public void testOptionalLongUnwrapSingleValueArraysDisabled() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("[99]", OptionalLong.class));
    }

    /*
    /**********************************************************
    /* OptionalLong: float-to-long coercion
    /**********************************************************
     */

    @Test
    public void testOptionalLongFromFloat() throws Exception
    {
        OptionalLong result = MAPPER.readValue("3.0", OptionalLong.class);
        assertTrue(result.isPresent());
        assertEquals(3L, result.getAsLong());
    }

    /*
    /**********************************************************
    /* OptionalDouble: boundary values
    /**********************************************************
     */

    @Test
    public void testOptionalDoubleBoundaryValues() throws Exception
    {
        OptionalDoubleBean maxBean = new OptionalDoubleBean(Double.MAX_VALUE);
        String json = MAPPER.writeValueAsString(maxBean);
        OptionalDoubleBean result = MAPPER.readValue(json, OptionalDoubleBean.class);
        assertTrue(result.value.isPresent());
        assertEquals(Double.MAX_VALUE, result.value.getAsDouble());

        OptionalDoubleBean minBean = new OptionalDoubleBean(Double.MIN_VALUE);
        json = MAPPER.writeValueAsString(minBean);
        result = MAPPER.readValue(json, OptionalDoubleBean.class);
        assertTrue(result.value.isPresent());
        assertEquals(Double.MIN_VALUE, result.value.getAsDouble());
    }

    @Test
    public void testOptionalDoubleZero() throws Exception
    {
        OptionalDoubleBean bean = new OptionalDoubleBean(0.0);
        String json = MAPPER.writeValueAsString(bean);
        OptionalDoubleBean result = MAPPER.readValue(json, OptionalDoubleBean.class);
        assertTrue(result.value.isPresent());
        assertEquals(0.0, result.value.getAsDouble());
    }

    @Test
    public void testOptionalDoubleNegativeZero() throws Exception
    {
        OptionalDouble result = MAPPER.readValue("-0.0", OptionalDouble.class);
        assertTrue(result.isPresent());
        assertEquals(-0.0, result.getAsDouble());
    }

    /*
    /**********************************************************
    /* OptionalDouble: serialization filter
    /**********************************************************
     */

    @Test
    public void testOptionalDoubleSerializeFilter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        assertEquals(a2q("{'value':1.5}"),
                mapper.writeValueAsString(new OptionalDoubleBean(1.5)));
        // absent is not strictly null so still serialized
        assertEquals(a2q("{'value':null}"),
                mapper.writeValueAsString(new OptionalDoubleBean()));

        mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT))
                .build();
        assertEquals(a2q("{'value':2.5}"),
                mapper.writeValueAsString(new OptionalDoubleBean(2.5)));
        assertEquals(a2q("{}"),
                mapper.writeValueAsString(new OptionalDoubleBean()));
    }

    /*
    /**********************************************************
    /* OptionalDouble: UNWRAP_SINGLE_VALUE_ARRAYS
    /**********************************************************
     */

    @Test
    public void testOptionalDoubleUnwrapSingleValueArrays() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
        OptionalDouble result = mapper.readValue("[3.14]", OptionalDouble.class);
        assertTrue(result.isPresent());
        assertEquals(3.14, result.getAsDouble(), 0.001);
    }

    @Test
    public void testOptionalDoubleUnwrapSingleValueArraysDisabled() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("[3.14]", OptionalDouble.class));
    }

    /*
    /**********************************************************
    /* OptionalDouble: integer-to-double coercion
    /**********************************************************
     */

    @Test
    public void testOptionalDoubleFromInteger() throws Exception
    {
        OptionalDouble result = MAPPER.readValue("42", OptionalDouble.class);
        assertTrue(result.isPresent());
        assertEquals(42.0, result.getAsDouble());
    }

    /*
    /**********************************************************
    /* OptionalDouble: special values in bean context
    /**********************************************************
     */

    @Test
    public void testOptionalDoubleSpecialValuesRoundTrip() throws Exception
    {
        // NaN
        OptionalDouble nan = MAPPER.readValue(q("NaN"), OptionalDouble.class);
        assertTrue(nan.isPresent());
        assertTrue(Double.isNaN(nan.getAsDouble()));

        // Positive Infinity
        OptionalDouble posInf = MAPPER.readValue(q("Infinity"), OptionalDouble.class);
        assertTrue(posInf.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, posInf.getAsDouble());

        // Negative Infinity
        OptionalDouble negInf = MAPPER.readValue(q("-Infinity"), OptionalDouble.class);
        assertTrue(negInf.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, negInf.getAsDouble());
    }

    /*
    /**********************************************************
    /* Cross-type: unexpected token handling
    /**********************************************************
     */

    @Test
    public void testOptionalIntFromBooleanFails() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("true", OptionalInt.class));
    }

    @Test
    public void testOptionalLongFromBooleanFails() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("true", OptionalLong.class));
    }

    @Test
    public void testOptionalDoubleFromBooleanFails() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("true", OptionalDouble.class));
    }

    @Test
    public void testOptionalIntFromObjectFails() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("{}", OptionalInt.class));
    }

    @Test
    public void testOptionalLongFromObjectFails() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("{}", OptionalLong.class));
    }

    @Test
    public void testOptionalDoubleFromObjectFails() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("{}", OptionalDouble.class));
    }
}
