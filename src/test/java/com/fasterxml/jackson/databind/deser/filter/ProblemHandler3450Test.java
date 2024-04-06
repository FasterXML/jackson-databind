package com.fasterxml.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNull;

public class ProblemHandler3450Test
{
    // [databind#3450]
    static class LenientDeserializationProblemHandler extends DeserializationProblemHandler {

        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType,
                String valueToConvert, String failureMsg)
        {
            // I just want to ignore badly formatted value
            return null;
        }
    }

    static class TestPojo3450Int {
        public Integer myInteger;
    }

    static class TestPojo3450Long {
        public Long myLong;
    }

    private final ObjectMapper LENIENT_MAPPER =
            JsonMapper.builder().addHandler(new LenientDeserializationProblemHandler()).build();

    // [databind#3450]
    @Test
    public void testIntegerCoercion3450() throws Exception
    {
        TestPojo3450Int pojo;

        // First expected coercion into `null` from empty String
        pojo = LENIENT_MAPPER.readValue("{\"myInteger\" : \"\"}", TestPojo3450Int.class);
        assertNull(pojo.myInteger);

        // and then coercion into `null` by our problem handler
        pojo = LENIENT_MAPPER.readValue("{\"myInteger\" : \"notInt\"}", TestPojo3450Int.class);
        assertNull(pojo.myInteger);
    }

    @Test
    public void testLongCoercion3450() throws Exception
    {
        TestPojo3450Long pojo;

        // First expected coercion into `null` from empty String
        pojo = LENIENT_MAPPER.readValue("{\"myLong\" : \"\"}", TestPojo3450Long.class);
        assertNull(pojo.myLong);

        // and then coercion into `null` by our problem handler
        pojo = LENIENT_MAPPER.readValue("{\"myLong\" : \"notSoLong\"}", TestPojo3450Long.class);
        assertNull(pojo.myLong);
    }
}
