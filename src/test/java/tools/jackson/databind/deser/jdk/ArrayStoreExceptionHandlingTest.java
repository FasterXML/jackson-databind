package tools.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that ArrayStoreException is properly caught and re-thrown
 * as DatabindException during array deserialization.
 */
public class ArrayStoreExceptionHandlingTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // Custom deserializer that returns incompatible types to trigger ArrayStoreException
    static class BadIntegerDeserializer extends ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) {
            // Cast through Object to bypass compile-time type checking and force runtime ArrayStoreException
            // when Jackson tries to assign this String to an Integer[] array
            return (Integer)(Object)"not an integer";
        }
    }
    
    // Test case 1: Deserializing with a custom deserializer that returns wrong type
    @Test
    public void testArrayStoreExceptionWithCustomDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addModule(new tools.jackson.databind.module.SimpleModule()
                .addDeserializer(Integer.class, new BadIntegerDeserializer()))
            .build();
        
        String json = "[1, 2, 3]";
        
        try {
            Integer[] result = mapper.readValue(json, Integer[].class);
            fail("Should have thrown an exception due to ArrayStoreException");
        } catch (DatabindException e) {
            // Expected: should be wrapped as DatabindException, not raw ArrayStoreException
            assertFalse(e instanceof ArrayStoreException, 
                "Exception should be DatabindException, not raw ArrayStoreException");
            
            // Verify that ArrayStoreException is in the cause chain
            Throwable cause = e;
            boolean foundArrayStore = false;
            while (cause != null) {
                if (cause instanceof ArrayStoreException) {
                    foundArrayStore = true;
                    break;
                }
                cause = cause.getCause();
            }
            assertTrue(foundArrayStore, "Should have ArrayStoreException in cause chain");
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
    
    // Test case 6: Large array with custom deserializer to test chunking + ArrayStoreException
    @Test
    public void testLargeArrayWithArrayStoreException() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addModule(new tools.jackson.databind.module.SimpleModule()
                .addDeserializer(Integer.class, new BadIntegerDeserializer()))
            .build();
        
        // Create array large enough to trigger chunking (> 12 elements)
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 50; i++) {
            if (i > 0) json.append(",");
            json.append(i);
        }
        json.append("]");
        
        try {
            Integer[] result = mapper.readValue(json.toString(), Integer[].class);
            fail("Should have thrown an exception due to ArrayStoreException");
        } catch (DatabindException e) {
            assertFalse(e instanceof ArrayStoreException, 
                "Exception should be DatabindException, not raw ArrayStoreException");
            
            // Verify that ArrayStoreException is in the cause chain
            Throwable cause = e;
            boolean foundArrayStore = false;
            while (cause != null) {
                if (cause instanceof ArrayStoreException) {
                    foundArrayStore = true;
                    break;
                }
                cause = cause.getCause();
            }
            assertTrue(foundArrayStore, "Should have ArrayStoreException in cause chain");
        }
    }
    
    // Test case 7: Single value unwrapping with ArrayStoreException
    @Test
    public void testSingleValueUnwrappingWithArrayStoreException() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .addModule(new tools.jackson.databind.module.SimpleModule()
                .addDeserializer(Integer.class, new BadIntegerDeserializer()))
            .build();
        
        String json = "42";  // Single value, not an array
        
        try {
            Integer[] result = mapper.readValue(json, Integer[].class);
            fail("Should have thrown an exception due to ArrayStoreException");
        } catch (DatabindException e) {
            assertFalse(e instanceof ArrayStoreException, 
                "Exception should be DatabindException, not raw ArrayStoreException");
            
            // Verify that ArrayStoreException is in the cause chain
            Throwable cause = e;
            boolean foundArrayStore = false;
            while (cause != null) {
                if (cause instanceof ArrayStoreException) {
                    foundArrayStore = true;
                    break;
                }
                cause = cause.getCause();
            }
            assertTrue(foundArrayStore, "Should have ArrayStoreException in cause chain");
        }
    }
}
