package tools.jackson.databind.ext;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Optional primitive serializers to improve code coverage.
 */
public class OptionalPrimitiveSerializationTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class OptionalDoubleWrapper {
        public OptionalDouble value;

        public OptionalDoubleWrapper(OptionalDouble value) {
            this.value = value;
        }

        protected OptionalDoubleWrapper() { }
    }

    static class OptionalIntWrapper {
        public OptionalInt value;

        public OptionalIntWrapper(OptionalInt value) {
            this.value = value;
        }

        protected OptionalIntWrapper() { }
    }

    static class OptionalLongWrapper {
        public OptionalLong value;

        public OptionalLongWrapper(OptionalLong value) {
            this.value = value;
        }

        protected OptionalLongWrapper() { }
    }

    // Test 6: OptionalDouble empty serialization
    @Test
    public void testOptionalDoubleEmpty() throws Exception {
        String json = MAPPER.writeValueAsString(new OptionalDoubleWrapper(OptionalDouble.empty()));
        assertEquals("{\"value\":null}", json);

        OptionalDoubleWrapper result = MAPPER.readValue(json, OptionalDoubleWrapper.class);
        assertFalse(result.value.isPresent());
    }

    // Test 7: OptionalInt with value serialization
    @Test
    public void testOptionalIntWithValue() throws Exception {
        OptionalIntWrapper wrapper = new OptionalIntWrapper(OptionalInt.of(42));
        String json = MAPPER.writeValueAsString(wrapper);
        assertEquals("{\"value\":42}", json);

        OptionalIntWrapper result = MAPPER.readValue(json, OptionalIntWrapper.class);
        assertTrue(result.value.isPresent());
        assertEquals(42, result.value.getAsInt());
    }

    // Test 8: OptionalLong boundary values
    @Test
    public void testOptionalLongBoundaryValues() throws Exception {
        // Test with Long.MAX_VALUE
        OptionalLongWrapper maxWrapper = new OptionalLongWrapper(OptionalLong.of(Long.MAX_VALUE));
        String maxJson = MAPPER.writeValueAsString(maxWrapper);
        OptionalLongWrapper maxResult = MAPPER.readValue(maxJson, OptionalLongWrapper.class);
        assertTrue(maxResult.value.isPresent());
        assertEquals(Long.MAX_VALUE, maxResult.value.getAsLong());

        // Test with Long.MIN_VALUE
        OptionalLongWrapper minWrapper = new OptionalLongWrapper(OptionalLong.of(Long.MIN_VALUE));
        String minJson = MAPPER.writeValueAsString(minWrapper);
        OptionalLongWrapper minResult = MAPPER.readValue(minJson, OptionalLongWrapper.class);
        assertTrue(minResult.value.isPresent());
        assertEquals(Long.MIN_VALUE, minResult.value.getAsLong());
    }
}
