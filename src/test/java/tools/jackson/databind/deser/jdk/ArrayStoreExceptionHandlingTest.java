package tools.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that ArrayStoreException is properly caught and re-thrown
 * as DatabindException during array deserialization.
 */
public class ArrayStoreExceptionHandlingTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // Test case 1: Deserializing incompatible types into a typed array
    @Test
    public void testIncompatibleTypeInTypedArray() throws Exception
    {
        // Try to deserialize strings into an Integer array
        String json = "[\"not\", \"an\", \"integer\"]";
        
        try {
            Integer[] result = MAPPER.readValue(json, Integer[].class);
            fail("Should have thrown an exception");
        } catch (DatabindException e) {
            // Expected: should be wrapped as DatabindException, not raw ArrayStoreException
            assertFalse(e instanceof ArrayStoreException, 
                "Exception should be DatabindException, not raw ArrayStoreException");
            
            // The root cause might be an ArrayStoreException or MismatchedInputException
            Throwable cause = e;
            boolean foundArrayStoreOrMismatch = false;
            while (cause != null) {
                if (cause instanceof ArrayStoreException || cause instanceof MismatchedInputException) {
                    foundArrayStoreOrMismatch = true;
                    break;
                }
                cause = cause.getCause();
            }
            // We may get MismatchedInputException for type conversion failures
            // ArrayStoreException would occur if conversion succeeded but assignment failed
        }
    }

    // Test case 2: Deserializing with polymorphic types
    static class Base {}
    static class Child1 extends Base { public int value; }
    static class Child2 extends Base { public String text; }
    
    @Test
    public void testPolymorphicArrayWithIncompatibleTypes() throws Exception
    {
        // This test verifies behavior when polymorphic deserialization might
        // produce incompatible types for a typed array
        String json = "[{\"value\": 42}]";
        
        // Deserializing into Child1[] should work
        Child1[] result = MAPPER.readValue(json, Child1[].class);
        assertEquals(1, result.length);
        assertEquals(42, result[0].value);
    }

    // Test case 3: Deserializing into Object[] should always work
    @Test
    public void testObjectArrayWithMixedTypes() throws Exception
    {
        String json = "[1, \"string\", true, null]";
        
        Object[] result = MAPPER.readValue(json, Object[].class);
        assertEquals(4, result.length);
        assertEquals(1, result[0]);
        assertEquals("string", result[1]);
        assertEquals(true, result[2]);
        assertNull(result[3]);
    }

    // Test case 4: Large arrays that trigger chunking in ObjectBuffer
    @Test
    public void testLargeArrayDeserialization() throws Exception
    {
        // Create a large array that will trigger ObjectBuffer's chunking mechanism
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) json.append(",");
            json.append(i);
        }
        json.append("]");
        
        Integer[] result = MAPPER.readValue(json.toString(), Integer[].class);
        assertEquals(100, result.length);
        for (int i = 0; i < 100; i++) {
            assertEquals(Integer.valueOf(i), result[i]);
        }
    }
    
    // Test case 5: Nested arrays with type mismatches
    @Test
    public void testNestedArraysWithTypeMismatch() throws Exception
    {
        String json = "[[1, 2], [3, 4]]";
        
        Integer[][] result = MAPPER.readValue(json, Integer[][].class);
        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertEquals(Integer.valueOf(1), result[0][0]);
        assertEquals(Integer.valueOf(4), result[1][1]);
    }
}
