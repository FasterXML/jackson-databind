package com.fasterxml.jackson.databind.deser.jdk;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class JDKNumberLeniencyTest extends BaseMapTest
{
    final ObjectMapper VANILLA_MAPPER = sharedMapper();

    final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
            .defaultLeniency(false)
            .build();

    public void testBooleanLeniencyInts() throws Exception
    {
        // First: read from integers fine by default
        assertEquals(Boolean.TRUE, VANILLA_MAPPER.readValue("1", Boolean.class));
        assertEquals(Boolean.TRUE,
                VANILLA_MAPPER.readValue("{\"b\" : 3}", BooleanWrapper.class).b);

        // But not with strict handling, first by global settings
        /*
        _verifyCoercionFailure(STRICT_MAPPER, "0", Boolean.class);

        _verifyCoercionFailure(STRICT_MAPPER, "{\"b\" : 1}", BooleanWrapper.class);
        */
    }

    protected void _verifyCoercionFailure(ObjectMapper mapper, String json, Class<?> type)
            throws Exception
    {
        try {
            VANILLA_MAPPER.readValue("1", Boolean.class);
            fail("Should not allow read in strict mode");
        } catch (MismatchedInputException e) {
            verifyException(e, "foo");
        }
    }

    //    protected ObjectMapper _
}
