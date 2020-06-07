package com.fasterxml.jackson.databind.deser.jdk;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class JDKNumberLeniencyTest extends BaseMapTest
{
    final ObjectMapper VANILLA_MAPPER = sharedMapper();

    final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
//            .defaultLeniency(false)
            .build();

    public void testBooleanLeniencyInts() throws Exception
    {
        // First: read from integers fine by default
        assertEquals(Boolean.TRUE, VANILLA_MAPPER.readValue("1", Boolean.class));
        assertEquals(Boolean.TRUE,
                VANILLA_MAPPER.readValue("{\"b\" : 3}", BooleanWrapper.class).b);

        // But not with strict handling, first by global settings
        _verifyBooleanCoercionFailure(STRICT_MAPPER, "0", Boolean.class);
        _verifyBooleanCoercionFailure(STRICT_MAPPER, "{\"b\" : 1}", BooleanWrapper.class);
    }

    protected void _verifyBooleanCoercionFailure(ObjectMapper mapper, String json, Class<?> type)
            throws Exception
    {
        try {
            mapper.readValue(json, type);
            fail("Should not allow read in strict mode");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce");
            verifyException(e, "to `java.lang.Boolean` value");
        }
    }
}
