package com.fasterxml.jackson.databind.exc;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.BrokenStringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

/**
 * Unit test for verifying that exceptions are properly handled (caught,
 * re-thrown or wrapped, depending) with Object deserialization,
 * including using concrete subtypes of {@link DatabindException}
 * (and streaming-level equivalents).
 */
class DeserExceptionTypeTest
{
    static class Bean {
        public String propX;
    }

    // Class that has no applicable creators and thus cannot be instantiated;
    // definition problem
    static class NoCreatorsBean {
        public int x;

        // Constructor that is not detectable as Creator
        public NoCreatorsBean(boolean foo, int foo2) { }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void handlingOfUnrecognized() throws Exception
    {
        UnrecognizedPropertyException exc = null;
        try {
            MAPPER.readValue("{\"bar\":3}", Bean.class);
        } catch (UnrecognizedPropertyException e) {
            exc = e;
        }
        if (exc == null) {
            fail("Should have failed binding");
        }
        assertEquals("bar", exc.getPropertyName());
        assertEquals(Bean.class, exc.getReferringClass());
        // also: should get list of known properties
        verifyException(exc, "propX");
    }

    /**
     * Simple test to check behavior when end-of-stream is encountered
     * without content.
     */
    @Test
    void exceptionWithEmpty() throws Exception
    {
        try {
            Object result = MAPPER.readValue("    ", Object.class);
            fail("Expected an exception, but got result value: "+result);
        } catch (MismatchedInputException e) {
            verifyException(e, "No content");
        }
    }

    @Test
    void exceptionWithIncomplete()
            throws Exception
    {
        BrokenStringReader r = new BrokenStringReader("[ 1, ", "TEST");
        try (JsonParser p = MAPPER.createParser(r)) {
            @SuppressWarnings("unused")
            Object ob = MAPPER.readValue(p, Object.class);
            fail("Should have gotten an exception");
        } catch (IOException e) {
            // For "bona fide" IO problems (due to low-level problem,
            // thrown by reader/stream), IOException must be thrown
            verifyException(e, "TEST");
        }
    }

    @Test
    void exceptionWithEOF() throws Exception
    {
        JsonParser p = MAPPER.createParser("  3");

        Integer I = MAPPER.readValue(p, Integer.class);
        assertEquals(3, I.intValue());

        // and then end-of-input...
        try {
            I = MAPPER.readValue(p, Integer.class);
            fail("Should have gotten an exception");
        } catch (MismatchedInputException e) {
            verifyException(e, "No content");
        }
        // also: should have no current token after end-of-input
        JsonToken t = p.currentToken();
        if (t != null) {
            fail("Expected current token to be null after end-of-stream, was: "+t);
        }
        p.close();
    }

    // [databind#1414]
    @Test
    void exceptionForNoCreators() throws Exception
    {
        try {
            NoCreatorsBean b = MAPPER.readValue("{}", NoCreatorsBean.class);
            fail("Should not succeed, got: "+b);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no Creators");
        }
    }
}
