package com.fasterxml.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class JDKNumberLeniencyTest
{
    /**
     * Simple wrapper around boolean types, usually to test value
     * conversions or wrapping
     */
    protected static class BooleanWrapper {
        public Boolean b;

        public BooleanWrapper() { }
        public BooleanWrapper(Boolean value) { b = value; }
    }

    final ObjectMapper VANILLA_MAPPER = sharedMapper();

    final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
//            .defaultLeniency(false)
            .build();

    @Test
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
