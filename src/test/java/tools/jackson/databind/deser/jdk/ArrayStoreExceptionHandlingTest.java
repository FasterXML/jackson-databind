package tools.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that ArrayStoreException is properly caught and re-thrown
 * as DatabindException during array deserialization.
 * 
 * Note: Creating reliable tests for ArrayStoreException is challenging because:
 * - Jackson uses Object[] chunks internally, which accept any object type
 * - Integer/String/etc are final classes, so type casts fail before array assignment
 * - The exception handling code is defensive for edge cases that are hard to reproduce
 * 
 * These tests focus on verifying the exception handling doesn't break normal behavior.
 */
public class ArrayStoreExceptionHandlingTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // Test case 1: Deserializing with polymorphic types
    static class Base {}
    static class Child1 extends Base { public int value; }
    static class Child2 extends Base { public String text; }
    
    @Test
    public void testPolymorphicArrayDeserialization() throws Exception
    {
        // Verify normal polymorphic deserialization works
        String json = "[{\"value\": 42}]";
        
        Child1[] result = MAPPER.readValue(json, Child1[].class);
        assertEquals(1, result.length);
        assertEquals(42, result[0].value);
    }

    // Test case 2: Deserializing into Object[] should always work
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

    // Test case 3: Large arrays that trigger chunking in ObjectBuffer
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
    
    // Test case 4: Nested arrays
    @Test
    public void testNestedArrayDeserialization() throws Exception
    {
        String json = "[[1, 2], [3, 4]]";
        
        Integer[][] result = MAPPER.readValue(json, Integer[][].class);
        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertEquals(Integer.valueOf(1), result[0][0]);
        assertEquals(Integer.valueOf(4), result[1][1]);
    }
    
    // Test case 5: Single value unwrapping
    @Test
    public void testSingleValueUnwrapping() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();
        
        String json = "42";  // Single value, not an array
        
        Integer[] result = mapper.readValue(json, Integer[].class);
        assertEquals(1, result.length);
        assertEquals(Integer.valueOf(42), result[0]);
    }
}
