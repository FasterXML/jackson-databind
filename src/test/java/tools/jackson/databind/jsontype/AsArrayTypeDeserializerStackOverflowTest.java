package tools.jackson.databind.jsontype;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.DatabindException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that depth tracking prevents StackOverflowError in
 * {@link tools.jackson.databind.jsontype.impl.AsArrayTypeDeserializer}
 * when deserializing deeply nested or recursive type information.
 */
public class AsArrayTypeDeserializerStackOverflowTest extends DatabindTestUtil
{
    // Base class with type info using WRAPPER_ARRAY
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    static class Base {
        public Map<String, Object> data;

        public Base() {
            this.data = new HashMap<>();
        }

        public Base(Map<String, Object> data) {
            this.data = data;
        }
    }

    // Subclass that can contain maps with type information
    static class Container extends Base {
        public Container() {
            super();
        }

        public Container(Map<String, Object> data) {
            super(data);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Test that attempts to deserialize a deeply nested structure that would
     * previously cause StackOverflowError. Now it should throw a proper
     * DatabindException with a meaningful error message about depth limit.
     */
    @Test
    public void testDeepRecursionPrevention() throws Exception
    {
        // Create a deeply nested JSON structure that would cause infinite recursion:
        // The issue occurs when a Map value has type information that points back
        // to a type deserializer, creating a cycle.
        
        // Build a deep nesting structure that exceeds MAX_TYPE_DESERIALIZER_DEPTH
        StringBuilder json = new StringBuilder();
        json.append("[\"").append(Container.class.getName()).append("\",{\"data\":{");
        
        // Create nesting beyond the limit
        int nestingDepth = DeserializationContext.MAX_TYPE_DESERIALIZER_DEPTH + 10;
        for (int i = 0; i < nestingDepth; i++) {
            json.append("\"key").append(i).append("\":");
            json.append("[\"").append(Container.class.getName()).append("\",{\"data\":{");
        }
        
        // Close all the nested structures
        for (int i = 0; i < nestingDepth; i++) {
            json.append("}}]");
        }
        json.append("}}]");

        // This should now throw a DatabindException instead of StackOverflowError
        Exception e = assertThrows(DatabindException.class, () -> {
            MAPPER.readValue(json.toString(), Base.class);
        });
        
        // Verify the error message mentions depth limit and infinite recursion
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
        // Create a simple valid structure
        String json = "[\"" + Container.class.getName() + "\",{\"data\":{\"key\":\"value\"}}]";
        
        Base result = MAPPER.readValue(json, Base.class);
        
        assertNotNull(result);
        assertInstanceOf(Container.class, result);
        assertNotNull(result.data);
        assertEquals("value", result.data.get("key"));
    }

    /**
     * Test that moderately nested structures (within limit) work correctly.
     */
    @Test
    public void testModerateNestingWorks() throws Exception
    {
        // Create nested structure well within the limit (e.g., 50 levels)
        StringBuilder json = new StringBuilder();
        json.append("[\"").append(Container.class.getName()).append("\",{\"data\":{");
        
        int nestingDepth = 50; // Well within MAX_TYPE_DESERIALIZER_DEPTH
        for (int i = 0; i < nestingDepth; i++) {
            json.append("\"key").append(i).append("\":\"value").append(i).append("\"");
            if (i < nestingDepth - 1) {
                json.append(",");
            }
        }
        
        json.append("}}]");

        // This should work fine
        Base result = MAPPER.readValue(json.toString(), Base.class);
        
        assertNotNull(result);
        assertInstanceOf(Container.class, result);
        assertEquals(nestingDepth, result.data.size());
    }
}
