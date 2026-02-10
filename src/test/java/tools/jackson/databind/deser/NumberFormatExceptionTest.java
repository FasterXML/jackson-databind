package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

import static org.junit.jupiter.api.Assertions.*;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/**
 * Demonstration test showing that NumberFormatException is properly caught and handled
 * when parsing invalid number strings to double/float primitives and wrappers.
 * 
 * This test documents the behavior: NumberFormatException thrown by NumberInput.parseDouble()
 * or parseFloat() is caught by the IllegalArgumentException handler and converted to
 * InvalidFormatException with proper context. All test methods verify that NFE does not
 * leak to user code.
 * 
 * See INVESTIGATION_NumberFormatException.md for detailed analysis.
 */
public class NumberFormatExceptionTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testInvalidDoubleString() throws Exception
    {
        // These should throw InvalidFormatException (or similar) not let NumberFormatException escape
        try {
            MAPPER.readValue("\"not_a_number\"", double.class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            // Expected - should be wrapped properly
            assertTrue(e.getMessage().contains("not a valid"),
                    "Expected error message about invalid value, got: " + e.getMessage());
        } catch (NumberFormatException nfe) {
            fail("NumberFormatException should be caught and wrapped, not propagated: " + nfe);
        }
    }

    @Test
    public void testInvalidFloatString() throws Exception
    {
        try {
            MAPPER.readValue("\"not_a_number\"", float.class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            // Expected - should be wrapped properly
            assertTrue(e.getMessage().contains("not a valid"),
                    "Expected error message about invalid value, got: " + e.getMessage());
        } catch (NumberFormatException nfe) {
            fail("NumberFormatException should be caught and wrapped, not propagated: " + nfe);
        }
    }

    @Test
    public void testInvalidDoubleStringInArray() throws Exception
    {
        try {
            MAPPER.readValue("[\"1.0\", \"not_a_number\", \"3.0\"]", double[].class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            // Expected - should be wrapped properly
            assertTrue(e.getMessage().contains("not a valid"),
                    "Expected error message about invalid value, got: " + e.getMessage());
        } catch (NumberFormatException nfe) {
            fail("NumberFormatException should be caught and wrapped, not propagated: " + nfe);
        }
    }

    @Test
    public void testInvalidFloatStringInArray() throws Exception
    {
        try {
            MAPPER.readValue("[\"1.0\", \"not_a_number\", \"3.0\"]", float[].class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            // Expected - should be wrapped properly
            assertTrue(e.getMessage().contains("not a valid"),
                    "Expected error message about invalid value, got: " + e.getMessage());
        } catch (NumberFormatException nfe) {
            fail("NumberFormatException should be caught and wrapped, not propagated: " + nfe);
        }
    }
}
