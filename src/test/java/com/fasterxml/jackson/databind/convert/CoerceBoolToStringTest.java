package com.fasterxml.jackson.databind.convert;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class CoerceBoolToStringTest
{
    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private final ObjectMapper MAPPER_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Boolean, CoercionAction.TryConvert))
            .build();

    private final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Boolean, CoercionAction.AsNull))
            .build();

    private final ObjectMapper MAPPER_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Textual, cfg ->
                    cfg.setCoercion(CoercionInputShape.Boolean, CoercionAction.AsEmpty))
            .build();

    @Test
    public void testDefaultBooleanToStringCoercion() throws JsonProcessingException
    {
        assertSuccessfulBooleanToStringCoercionWith(DEFAULT_MAPPER);
    }

    @Test
    public void testCoerceConfigToConvert() throws JsonProcessingException
    {
        assertSuccessfulBooleanToStringCoercionWith(MAPPER_TRY_CONVERT);
    }

    @Test
    public void testCoerceConfigToNull() throws JsonProcessingException
    {
        assertNull(MAPPER_TO_NULL.readValue("true", String.class));
        StringWrapper w = MAPPER_TO_NULL.readValue("{\"str\": false}", StringWrapper.class);
        assertNull(w.str);
        String[] arr = MAPPER_TO_NULL.readValue("[ true ]", String[].class);
        assertEquals(1, arr.length);
        assertNull(arr[0]);
    }

    @Test
    public void testCoerceConfigToEmpty() throws JsonProcessingException
    {
        assertEquals("", MAPPER_TO_EMPTY.readValue("true", String.class));
        StringWrapper w = MAPPER_TO_EMPTY.readValue("{\"str\": false}", StringWrapper.class);
        assertEquals("", w.str);
        String[] arr = MAPPER_TO_EMPTY.readValue("[ true ]", String[].class);
        assertEquals(1, arr.length);
        assertEquals("", arr[0]);
    }

    @Test
    public void testCoerceConfigToFail() throws JsonProcessingException
    {
        _verifyCoerceFail(MAPPER_TO_FAIL, String.class, "true");
        _verifyCoerceFail(MAPPER_TO_FAIL, StringWrapper.class, "{\"str\": false}", "string");
        _verifyCoerceFail(MAPPER_TO_FAIL, String[].class, "[ true ]", "to `java.lang.String` value");
    }

    /*
    /********************************************************
    /* Helper methods
    /********************************************************
     */

    private void assertSuccessfulBooleanToStringCoercionWith(ObjectMapper objectMapper)
        throws JsonProcessingException
    {
        assertEquals("false", objectMapper.readValue("false", String.class));
        assertEquals("true", objectMapper.readValue("true", String.class));
        {
            StringWrapper w = objectMapper.readValue("{\"str\": false}", StringWrapper.class);
            assertEquals("false", w.str);
            String[] arr = objectMapper.readValue("[ true ]", String[].class);
            assertEquals("true", arr[0]);
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
            fail("Should not accept Boolean for "+targetType.getName()+" when configured to fail.");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce Boolean");
            verifyException(e, targetTypeDesc);
        }
    }
}
