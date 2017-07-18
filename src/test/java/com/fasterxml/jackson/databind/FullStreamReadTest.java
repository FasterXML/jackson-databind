package com.fasterxml.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class FullStreamReadTest extends BaseMapTest
{
    private final static String JSON_OK_ARRAY = " [ 1, 2, 3]    ";
    private final static String JSON_OK_ARRAY_WITH_COMMENT = JSON_OK_ARRAY + " // stuff ";

    private final static String JSON_FAIL_ARRAY = JSON_OK_ARRAY + " [ ]";

    /*
    /**********************************************************
    /* Test methods, config
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();

    public void XXXtestViaMapper() throws Exception
    {
        assertFalse(MAPPER.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS));

        // by default, should be ok to read, all
        _verifyArray(MAPPER.readTree(JSON_OK_ARRAY));
        _verifyArray(MAPPER.readTree(JSON_OK_ARRAY_WITH_COMMENT));
        _verifyArray(MAPPER.readTree(JSON_FAIL_ARRAY));

        // and also via "untyped"
        _verifyCollection(MAPPER.readValue(JSON_OK_ARRAY, List.class));
        _verifyCollection(MAPPER.readValue(JSON_OK_ARRAY_WITH_COMMENT, List.class));
        _verifyCollection(MAPPER.readValue(JSON_FAIL_ARRAY, List.class));

        // but things change if we enforce checks
        ObjectMapper strict = newObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

        // some still ok
        _verifyArray(strict.readTree(JSON_OK_ARRAY));
        _verifyArray(strict.readTree(JSON_OK_ARRAY_WITH_COMMENT));

        _verifyCollection(strict.readValue(JSON_OK_ARRAY, List.class));
        _verifyCollection(strict.readValue(JSON_OK_ARRAY_WITH_COMMENT, List.class));

        // but not all
        try {
            strict.readTree(JSON_FAIL_ARRAY);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "foo");
        }

        try {
            strict.readValue(JSON_FAIL_ARRAY, List.class);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "foo");
        }
    }

    public void testViaReader() throws Exception
    {
        ObjectReader R = MAPPER.reader();
        assertFalse(R.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS));

        _verifyArray(R.readTree(JSON_OK_ARRAY));
        _verifyArray(R.readTree(JSON_OK_ARRAY_WITH_COMMENT));
        _verifyArray(R.readTree(JSON_FAIL_ARRAY));
        ObjectReader rColl = R.forType(List.class);
        _verifyCollection((List<?>)rColl.readValue(JSON_OK_ARRAY));
        _verifyCollection((List<?>)rColl.readValue(JSON_OK_ARRAY_WITH_COMMENT));
        _verifyCollection((List<?>)rColl.readValue(JSON_FAIL_ARRAY));

        /*
        ObjectReader strictR = R.with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        try {
            strictR.readTree(JSON_FAIL_ARRAY);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "foo");
        }

        try {
            strictR.forType(List.class).readValue(JSON_FAIL_ARRAY);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "foo");
        }
        */
    }

    private void _verifyArray(JsonNode n) throws Exception
    {
        assertTrue(n.isArray());
        assertEquals(3, n.size());
    }

    private void _verifyCollection(List<?> coll) throws Exception
    {
        assertEquals(3, coll.size());
        assertEquals(Integer.valueOf(1), coll.get(0));
        assertEquals(Integer.valueOf(2), coll.get(1));
        assertEquals(Integer.valueOf(3), coll.get(2));
    }
}
