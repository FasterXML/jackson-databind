package tools.jackson.databind.convert;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.type.LogicalType;

public class CoerceBoolToStringTest extends BaseMapTest
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

    public void testDefaultBooleanToStringCoercion() throws Exception
    {
        assertSuccessfulBooleanToStringCoercionWith(DEFAULT_MAPPER);
    }

    public void testCoerceConfigToConvert() throws Exception
    {
        assertSuccessfulBooleanToStringCoercionWith(MAPPER_TRY_CONVERT);
    }

    public void testCoerceConfigToNull() throws Exception
    {
        assertNull(MAPPER_TO_NULL.readValue("true", String.class));
        StringWrapper w = MAPPER_TO_NULL.readValue("{\"str\": false}", StringWrapper.class);
        assertNull(w.str);
        String[] arr = MAPPER_TO_NULL.readValue("[ true ]", String[].class);
        assertEquals(1, arr.length);
        assertNull(arr[0]);
    }

    public void testCoerceConfigToEmpty() throws Exception
    {
        assertEquals("", MAPPER_TO_EMPTY.readValue("true", String.class));
        StringWrapper w = MAPPER_TO_EMPTY.readValue("{\"str\": false}", StringWrapper.class);
        assertEquals("", w.str);
        String[] arr = MAPPER_TO_EMPTY.readValue("[ true ]", String[].class);
        assertEquals(1, arr.length);
        assertEquals("", arr[0]);
    }

    public void testCoerceConfigToFail() throws Exception
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
        throws Exception
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
            String doc, String targetTypeDesc)
        throws Exception
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
