package tools.jackson.databind.ext;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Stream serializers to improve code coverage.
 */
public class StreamSerializationTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class IntStreamWrapper {
        public IntStream value;

        public IntStreamWrapper(IntStream value) {
            this.value = value;
        }

        protected IntStreamWrapper() { }
    }

    static class LongStreamWrapper {
        public LongStream value;

        public LongStreamWrapper(LongStream value) {
            this.value = value;
        }

        protected LongStreamWrapper() { }
    }

    static class DoubleStreamWrapper {
        public DoubleStream value;

        public DoubleStreamWrapper(DoubleStream value) {
            this.value = value;
        }

        protected DoubleStreamWrapper() { }
    }

    // Test 9: Empty IntStream serialization
    @Test
    public void testEmptyIntStream() throws Exception {
        IntStreamWrapper wrapper = new IntStreamWrapper(IntStream.empty());
        String json = MAPPER.writeValueAsString(wrapper);
        // Empty stream should serialize to empty array
        assertEquals("{\"value\":[]}", json);
    }

    // Test 10: LongStream with multiple elements
    @Test
    public void testLongStreamWithElements() throws Exception {
        LongStreamWrapper wrapper = new LongStreamWrapper(LongStream.of(1L, 2L, 3L, 100L));
        String json = MAPPER.writeValueAsString(wrapper);
        assertTrue(json.contains("\"value\":[1,2,3,100]"));
    }
}
