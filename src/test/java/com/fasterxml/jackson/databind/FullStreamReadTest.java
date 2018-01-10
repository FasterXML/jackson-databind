package com.fasterxml.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
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

    public void testMapperAcceptTrailing() throws Exception
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
    }

    public void testMapperFailOnTrailing() throws Exception
    {
        // but things change if we enforce checks
        ObjectMapper strict = newObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        assertTrue(strict.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS));

        // some still ok
        _verifyArray(strict.readTree(JSON_OK_ARRAY));
        _verifyCollection(strict.readValue(JSON_OK_ARRAY, List.class));

        // but if real content exists, will fail
        try {
            strict.readTree(JSON_FAIL_ARRAY);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Trailing token (of type START_ARRAY)");
            verifyException(e, "value (bound as `com.fasterxml.jackson.databind.JsonNode`)");
        }

        try {
            strict.readValue(JSON_FAIL_ARRAY, List.class);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Trailing token (of type START_ARRAY)");
            verifyException(e, "value (bound as `java.util.List`)");
        }

        // others fail conditionally: will fail on comments unless enabled

        try {
            strict.readValue(JSON_OK_ARRAY_WITH_COMMENT, List.class);
            fail("Should not have passed");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character");
            verifyException(e, "maybe a (non-standard) comment");
        }
        try {
            strict.readTree(JSON_OK_ARRAY_WITH_COMMENT);
            fail("Should not have passed");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character");
            verifyException(e, "maybe a (non-standard) comment");
        }

        ObjectMapper strictWithComments = new ObjectMapper(
                strict.tokenStreamFactory().rebuild()
                .with(JsonParser.Feature.ALLOW_COMMENTS)
                .build());
        _verifyArray(strictWithComments.readTree(JSON_OK_ARRAY_WITH_COMMENT));
        _verifyCollection(strictWithComments.readValue(JSON_OK_ARRAY_WITH_COMMENT, List.class));
    }

    public void testReaderAcceptTrailing() throws Exception
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
    }

    public void testReaderFailOnTrailing() throws Exception
    {
        ObjectReader strictR = MAPPER.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        ObjectReader strictRForList = strictR.forType(List.class);
        _verifyArray(strictR.readTree(JSON_OK_ARRAY));
        _verifyCollection((List<?>)strictRForList.readValue(JSON_OK_ARRAY));

        // Will fail hard if there is a trailing token
        try {
            strictRForList.readValue(JSON_FAIL_ARRAY);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Trailing token (of type START_ARRAY)");
            verifyException(e, "value (bound as `java.util.List`)");
        }
        try {
            strictR.readTree(JSON_FAIL_ARRAY);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Trailing token (of type START_ARRAY)");
            verifyException(e, "value (bound as `com.fasterxml.jackson.databind.JsonNode`)");
        }

        // ... also verify that same happens with "value to update"
        try {
            strictR.withValueToUpdate(new ArrayList<Object>())
                .readValue(JSON_FAIL_ARRAY);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Trailing token (of type START_ARRAY)");
            verifyException(e, "value (bound as `java.util.ArrayList`)");
        }

        // others conditionally: will fail on comments unless enabled

        try {
            strictRForList.readValue(JSON_OK_ARRAY_WITH_COMMENT);
            fail("Should not have passed");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character");
            verifyException(e, "maybe a (non-standard) comment");
        }
        try {
            strictR.readTree(JSON_OK_ARRAY_WITH_COMMENT);
            fail("Should not have passed");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character");
            verifyException(e, "maybe a (non-standard) comment");
        }

        // but works if comments enabled etc

        ObjectReader strictRWithComments = strictR.with(JsonParser.Feature.ALLOW_COMMENTS);
        
        _verifyCollection((List<?>)strictRWithComments.forType(List.class).readValue(JSON_OK_ARRAY_WITH_COMMENT));
        _verifyArray(strictRWithComments.readTree(JSON_OK_ARRAY_WITH_COMMENT));
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
