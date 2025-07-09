package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.BrokenStringReader;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for verifying that exceptions are properly handled (caught,
 * re-thrown or wrapped, depending) with Object deserialization,
 * including using concrete subtypes of {@link DatabindException}
 * (and streaming-level equivalents).
 */
public class DeserExceptionTypeTest
    extends DatabindTestUtil
{
    static class Bean {
        public String propX;
    }

    // Class that has no applicable creators and thus cannot be instantiated;
    // definition problem
    static class NoCreatorsBean {
        public int x;

        // Constructor that is not detectable as Creator
        protected NoCreatorsBean(boolean foo, int foo2) { }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testHandlingOfUnrecognized() throws Exception
    {
        UnrecognizedPropertyException exc = null;
        try {
            MAPPER.readerFor(Bean.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue("{\"bar\":3}");
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
        // and as per [databind#5179] current token (pointing to start of value?)
        assertEquals(JsonToken.VALUE_NUMBER_INT, exc.getCurrentToken());
    }

    /**
     * Simple test to check behavior when end-of-stream is encountered
     * without content.
     */
    @Test
    public void testExceptionWithEmpty() throws Exception
    {
        try {
            Object result = MAPPER.readValue("    ", Object.class);
            fail("Expected an exception, but got result value: "+result);
        } catch (MismatchedInputException e) {
            verifyException(e, "No content");
            assertNull(e.getCurrentToken());
        }
    }

    @Test
    public void testExceptionWithIncomplete()
        throws Exception
    {
        BrokenStringReader r = new BrokenStringReader("[ 1, ", "TEST");
        try (JsonParser p = MAPPER.createParser(r)) {
            @SuppressWarnings("unused")
            Object ob = MAPPER.readValue(p, Object.class);
            fail("Should have gotten an exception");
        } catch (JacksonException e) {
            // For "bona fide" IO problems (due to low-level problem,
            // thrown by reader/stream), IOException must be thrown
            verifyException(e, JacksonIOException.class, "TEST");
        }
    }

    @Test
    public void testExceptionWithEOF() throws Exception
    {
        JsonParser p = MAPPER.createParser("  3");

        Integer I = MAPPER.readValue(p, Integer.class);
        assertEquals(3, I.intValue());

        // and then end-of-input...
        try {
            I = MAPPER.readValue(p, Integer.class);
            fail("Should have gotten an exception");
        } catch (JacksonException e) {
            verifyException(e, MismatchedInputException.class, "No content");
            MismatchedInputException mie = (MismatchedInputException) e;
            assertNull(mie.getCurrentToken());
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
    public void testExceptionForNoCreators() throws Exception
    {
        try {
            NoCreatorsBean b = MAPPER.readValue("{}", NoCreatorsBean.class);
            fail("Should not succeed, got: "+b);
        } catch (InvalidDefinitionException e) {
            verifyException(e, InvalidDefinitionException.class, "no Creators");
        }
    }
}
