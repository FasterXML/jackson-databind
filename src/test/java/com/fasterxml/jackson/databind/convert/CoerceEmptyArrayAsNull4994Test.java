package com.fasterxml.jackson.databind.convert;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#4994] CoercionConfig for empty arrays not working as expected
public class CoerceEmptyArrayAsNull4994Test
    extends DatabindTestUtil
{
    public static class StringArrayWrapper4994 {
        public String[] value;
    }

    public static class ObjectArrayWrapper4994 {
        public Object[] value;
    }

    public static class PojoArrayWrapper4994 {
        public Pojo4994[] value;
    }

    public static class Pojo4994 {
        public String name;
        public int age;
    }

    private final String json = "{\"value\": []}";

    private final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
                    cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.AsNull))
            .build();

    @Test
    public void testAsNull()
        throws Exception
    {
        StringArrayWrapper4994 pojo = MAPPER_TO_NULL.readValue(json, StringArrayWrapper4994.class);

        assertNull(pojo.value); // expected: <null> but was: <[]>
    }

    @Test
    public void testPojoArrayAsNull()
            throws Exception
    {
        PojoArrayWrapper4994 wrapper = MAPPER_TO_NULL.readValue(json, PojoArrayWrapper4994.class);

        assertNull(wrapper.value);
    }

    @Test
    public void testObjectArrayAsNull()
            throws Exception
    {
        ObjectArrayWrapper4994 wrapper = MAPPER_TO_NULL.readValue(json, ObjectArrayWrapper4994.class);

        assertNull(wrapper.value);
    }

}
