package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class CoerceFloatToStringTest extends BaseMapTest
{
    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Float, CoercionAction.Fail))
            .build();

    private final ObjectMapper MAPPER_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Float, CoercionAction.TryConvert))
            .build();

    private final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Float, CoercionAction.AsNull))
            .build();

    private final ObjectMapper MAPPER_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Float, CoercionAction.AsEmpty))
            .build();

    public void testDefaultFloatToStringCoercion() throws JsonProcessingException
    {
        assertSuccessfulFloatToStringCoercionWith(DEFAULT_MAPPER);
    }

    public void testCoerceConfigToConvert() throws JsonProcessingException
    {
        assertSuccessfulFloatToStringCoercionWith(MAPPER_TRY_CONVERT);
    }

    public void testCoerceConfigToNull() throws JsonProcessingException
    {
        assertNull(MAPPER_TO_NULL.readValue("1.2", String.class));
        StringWrapper w = MAPPER_TO_NULL.readValue("{\"str\": -5.3}", StringWrapper.class);
        assertNull(w.str);
        String[] arr = MAPPER_TO_NULL.readValue("[ 2.1 ]", String[].class);
        assertEquals(1, arr.length);
        assertNull(arr[0]);
    }

    public void testCoerceConfigToEmpty() throws JsonProcessingException
    {
        assertEquals("", MAPPER_TO_EMPTY.readValue("3.5", String.class));
        StringWrapper w = MAPPER_TO_EMPTY.readValue("{\"str\": -5.3}", StringWrapper.class);
        assertEquals("", w.str);
        String[] arr = MAPPER_TO_EMPTY.readValue("[ 2.1 ]", String[].class);
        assertEquals(1, arr.length);
        assertEquals("", arr[0]);
    }


    public void testCoerceConfigToFail() throws JsonProcessingException
    {
        _verifyCoerceFail(MAPPER_TO_FAIL, String.class, "3.5");
        _verifyCoerceFail(MAPPER_TO_FAIL, StringWrapper.class, "{\"str\": -5.3}", "string");
        _verifyCoerceFail(MAPPER_TO_FAIL, String[].class, "[ 2.1 ]",
                "to `java.lang.String` value");
    }

    /*
    /********************************************************
    /* Helper methods
    /********************************************************
     */

    private void assertSuccessfulFloatToStringCoercionWith(ObjectMapper objectMapper)
        throws JsonProcessingException
    {
        assertEquals("3.0", objectMapper.readValue("3.0", String.class));
        assertEquals("-2.0", objectMapper.readValue("-2.0", String.class));
        {
            StringWrapper w = objectMapper.readValue("{\"str\": -5.0}", StringWrapper.class);
            assertEquals("-5.0", w.str);
            String[] arr = objectMapper.readValue("[ 2.0 ]", String[].class);
            assertEquals("2.0", arr[0]);
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
            String doc, String targetTypeDesc)
        throws JsonProcessingException
    {
        try {
            r.forType(targetType).readValue(doc);
            fail("Should not accept Float for "+targetType.getName()+" when configured to fail.");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce Float");
            verifyException(e, targetTypeDesc);
        }
    }
}
