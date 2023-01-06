package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

// [databind#3013] / PR #3608
public class CoerceIntToStringTest extends BaseMapTest
{
    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .build();

    private final ObjectMapper MAPPER_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert))
            .build();

    private final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.AsNull))
            .build();

    private final ObjectMapper MAPPER_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.AsEmpty))
            .build();

    public void testDefaultIntToStringCoercion() throws JsonProcessingException
    {
        assertSuccessfulIntToStringCoercionWith(DEFAULT_MAPPER);
    }

    public void testCoerceConfigToConvert() throws JsonProcessingException
    {
        assertSuccessfulIntToStringCoercionWith(MAPPER_TRY_CONVERT);
    }

    public void testCoerceConfigToNull() throws JsonProcessingException
    {
        assertNull(MAPPER_TO_NULL.readValue("1", String.class));
        StringWrapper w = MAPPER_TO_NULL.readValue("{\"str\": -5}", StringWrapper.class);
        assertNull(w.str);
        String[] arr = MAPPER_TO_NULL.readValue("[ 2 ]", String[].class);
        assertEquals(1, arr.length);
        assertNull(arr[0]);
    }

    public void testCoerceConfigToEmpty() throws JsonProcessingException
    {
        assertEquals("", MAPPER_TO_EMPTY.readValue("3", String.class));
        StringWrapper w = MAPPER_TO_EMPTY.readValue("{\"str\": -5}", StringWrapper.class);
        assertEquals("", w.str);
        String[] arr = MAPPER_TO_EMPTY.readValue("[ 2 ]", String[].class);
        assertEquals(1, arr.length);
        assertEquals("", arr[0]);
    }

    public void testCoerceConfigToFail() throws JsonProcessingException
    {
        _verifyCoerceFail(MAPPER_TO_FAIL, String.class, "3");
        _verifyCoerceFail(MAPPER_TO_FAIL, StringWrapper.class, "{\"str\": -5}", "string");
        _verifyCoerceFail(MAPPER_TO_FAIL, String[].class, "[ 2 ]", "to `java.lang.String` value");
    }

    /*
    /********************************************************
    /* Helper methods
    /********************************************************
     */

    private void assertSuccessfulIntToStringCoercionWith(ObjectMapper objectMapper)
            throws JsonProcessingException
    {
        assertEquals("3", objectMapper.readValue("3", String.class));
        assertEquals("-2", objectMapper.readValue("-2", String.class));
        {
            StringWrapper w = objectMapper.readValue("{\"str\": -5}", StringWrapper.class);
            assertEquals("-5", w.str);
            String[] arr = objectMapper.readValue("[ 2 ]", String[].class);
            assertEquals("2", arr[0]);
        }
    }

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
                                   String doc) throws JsonProcessingException
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetType.getName());
    }

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
                                   String doc, String targetTypeDesc) throws JsonProcessingException
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetTypeDesc);
    }

    private void _verifyCoerceFail(ObjectReader r, Class<?> targetType,
                                   String doc, String targetTypeDesc) throws JsonProcessingException
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
