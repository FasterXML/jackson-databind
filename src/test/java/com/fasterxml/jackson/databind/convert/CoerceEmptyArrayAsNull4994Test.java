package com.fasterxml.jackson.databind.convert;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#4994] CoercionConfig for empty arrays not working as expected
public class CoerceEmptyArrayAsNull4994Test
    extends DatabindTestUtil
{
    public static class Pojo4994 {
        public String[] value;
    }

    @Test
    public void testAsNull()
        throws Exception
    {
        final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
                .withCoercionConfigDefaults(cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.AsNull))
                .build();

        String json = "{\"value\": []}";

        Pojo4994 pojo = MAPPER_TO_NULL.readValue(json, Pojo4994.class);

        assertNull(pojo.value); // expected: <null> but was: <[]>
    }

    @Test
    public void testAsEmpty()
        throws Exception
    {
        final ObjectMapper MAPPER_TO_EMPTY = jsonMapperBuilder()
                .withCoercionConfigDefaults(cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.AsEmpty))
                .build();

        String json = "{\"value\": []}";

        Pojo4994 pojo = MAPPER_TO_EMPTY.readValue(json, Pojo4994.class);

        assertEquals(0, pojo.value.length); // expected: <0> but was: <1>
    }

    @Test
    public void testFail()
        throws Exception
    {
        final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
                .withCoercionConfigDefaults(cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.Fail))
                .build();

        String json = "{\"value\": []}";

        try {
            MAPPER_TO_FAIL.readValue(json, Pojo4994.class);
        } catch (Exception e) {
            verifyException(e, "Cannot coerce empty Array");
        }
    }

    @Test
    public void testTryConvert()
        throws Exception
    {
        final ObjectMapper MAPPER_TO_TRY = jsonMapperBuilder()
                .withCoercionConfigDefaults(cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.TryConvert))
                .build();

        String json = "{\"value\": []}";

        Pojo4994 pojo = MAPPER_TO_TRY.readValue(json, Pojo4994.class);

        assertEquals(0, pojo.value.length); // expected: <0> but was: <1>
    }

}
