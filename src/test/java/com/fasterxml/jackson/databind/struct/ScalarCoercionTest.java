package com.fasterxml.jackson.databind.struct;

import java.io.IOException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// for [databind#1106]
public class ScalarCoercionTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();
    private final ObjectReader COERCING_READER = MAPPER
            .reader().with(DeserializationFeature.ALLOW_COERCION_FOR_SCALARS);
    private final ObjectReader NOT_COERCING_READER = MAPPER
            .reader().without(DeserializationFeature.ALLOW_COERCION_FOR_SCALARS);

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testBoolean() throws Exception
    {
        // first successful coercions
        _verifyCoerceSuccess("1", Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess("1", Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(quote("true"), Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(quote("true"), Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess(quote("True"), Boolean.TYPE, Boolean.TRUE);
        _verifyCoerceSuccess(quote("True"), Boolean.class, Boolean.TRUE);
        _verifyCoerceSuccess("0", Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess("0", Boolean.class, Boolean.FALSE);
        _verifyCoerceSuccess(quote("false"), Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(quote("false"), Boolean.class, Boolean.FALSE);
        _verifyCoerceSuccess(quote("False"), Boolean.TYPE, Boolean.FALSE);
        _verifyCoerceSuccess(quote("False"), Boolean.class, Boolean.FALSE);

        // and then expected fails
        /*
        _verifyCoerceFail("1", Boolean.TYPE);
        _verifyCoerceFail("1", Boolean.class);
        _verifyCoerceFail(quote("true"), Boolean.TYPE);
        _verifyCoerceFail(quote("true"), Boolean.class);
        */
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyCoerceSuccess(String input, Class<?> type, Object exp) throws IOException
    {
        Object result = COERCING_READER.forType(type)
                .readValue(input);
        assertEquals(exp, result);
    }

    private void _verifyCoerceFail(String input, Class<?> type) throws IOException
    {
        try {
            NOT_COERCING_READER.forType(type)
                .readValue(input);
            fail("Should not have allowed coercion");
        } catch (MismatchedInputException e) {
            verifyException(e, "Coercion not enabled");
        }
    }
}
