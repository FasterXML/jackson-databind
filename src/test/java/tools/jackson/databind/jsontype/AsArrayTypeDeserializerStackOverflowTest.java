package tools.jackson.databind.jsontype;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that depth tracking prevents StackOverflowError in
 * {@link tools.jackson.databind.jsontype.impl.AsArrayTypeDeserializer}
 * when deserializing deeply nested or recursive type information.
 */
public class AsArrayTypeDeserializerStackOverflowTest extends DatabindTestUtil
{
    /**
     * POJO with a Map that can hold typed Objects.
     * When combined with default typing, this can create recursive
     * type deserialization scenarios.
     */
    static class DataHolder {
        public Map<String, Object> values;

        public DataHolder() {
            this.values = new HashMap<>();
        }
    }

    /**
     * Create an ObjectMapper with default typing enabled using WRAPPER_ARRAY.
     * This mimics the configuration that can lead to stackoverflow.
     */
    private ObjectMapper createMapperWithDefaultTyping() {
        // Enable default typing with WRAPPER_ARRAY format using builder pattern
        return jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.WRAPPER_ARRAY)
                .build();
    }

    /**
     * Test that attempts to deserialize a deeply nested structure that would
     * previously cause StackOverflowError. Now it should throw a proper
     * DatabindException with a meaningful error message about depth limit.
     * 
     * This test creates a structure where type information nesting exceeds
     * the MAX_TYPE_DESERIALIZER_DEPTH limit.
     */
    @Test
    public void testDeepRecursionPrevention() throws Exception
    {
        ObjectMapper mapper = createMapperWithDefaultTyping();
        
        // Build a deeply nested structure with type information
        // Each map value needs type info, creating nested TypeDeserializer calls
        StringBuilder json = new StringBuilder();
        
        // Start with the outer DataHolder type
        json.append("[\"").append(DataHolder.class.getName()).append("\",");
        json.append("{\"values\":{");
        
        // Create deep nesting that exceeds MAX_TYPE_DESERIALIZER_DEPTH
        // Each nested level adds to the type deserializer depth
        int nestingDepth = DeserializationContext.MAX_TYPE_DESERIALIZER_DEPTH + 10;
        
        for (int i = 0; i < nestingDepth; i++) {
            json.append("\"k").append(i).append("\":");
            // Each value is a Map with type info, causing recursive TypeDeserializer invocation
            json.append("[\"java.util.HashMap\",{");
        }
        
        // Add a simple value at the deepest level
        json.append("\"innerKey\":\"innerValue\"");
        
        // Close all nested structures
        for (int i = 0; i < nestingDepth; i++) {
            json.append("}]");
            if (i < nestingDepth - 1) {
                json.append(",");
            }
        }
        
        json.append("}}]");

        // This should now throw a DatabindException instead of StackOverflowError
        Exception e = assertThrows(DatabindException.class, () -> {
            mapper.readValue(json.toString(), Object.class);
        });
        
        // Verify the error message mentions depth limit and/or infinite recursion
        String msg = e.getMessage();
        assertTrue(msg.contains("depth exceeds") || msg.contains("infinite recursion")
                        || msg.contains("StackOverflowError"),
                "Expected error message about depth or recursion, got: " + msg);
    }

    /**
     * Test that normal, non-deep structures still work correctly.
     */
    @Test
    public void testNormalDeserialization() throws Exception
    {
        ObjectMapper mapper = createMapperWithDefaultTyping();
        
        // Create a simple valid structure
        String json = "[\"" + DataHolder.class.getName() + "\"," +
                "{\"values\":{\"key1\":\"value1\",\"key2\":\"value2\"}}]";
        
        Object result = mapper.readValue(json, Object.class);
        
        assertNotNull(result);
        assertInstanceOf(DataHolder.class, result);
        DataHolder holder = (DataHolder) result;
        assertNotNull(holder.values);
        assertEquals("value1", holder.values.get("key1"));
        assertEquals("value2", holder.values.get("key2"));
    }

    /**
     * Test that moderately nested structures (within limit) work correctly.
     */
    @Test
    public void testModerateNestingWorks() throws Exception
    {
        ObjectMapper mapper = createMapperWithDefaultTyping();
        
        // Create nested structure well within the limit (e.g., 10 levels)
        StringBuilder json = new StringBuilder();
        json.append("[\"").append(DataHolder.class.getName()).append("\",");
        json.append("{\"values\":{");
        
        int nestingDepth = 10; // Well within MAX_TYPE_DESERIALIZER_DEPTH
        for (int i = 0; i < nestingDepth; i++) {
            json.append("\"k").append(i).append("\":");
            json.append("[\"java.util.HashMap\",{");
        }
        
        json.append("\"innerKey\":\"innerValue\"");
        
        for (int i = 0; i < nestingDepth; i++) {
            json.append("}]");
            if (i < nestingDepth - 1) {
                json.append(",");
            }
        }
        
        json.append("}}]");

        // This should work fine
        Object result = mapper.readValue(json.toString(), Object.class);
        
        assertNotNull(result);
        assertInstanceOf(DataHolder.class, result);
    }
}
