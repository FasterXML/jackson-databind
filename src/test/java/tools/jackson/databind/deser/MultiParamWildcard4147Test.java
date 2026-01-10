package tools.jackson.databind.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to ensure multi-parameter generic types with wildcards
 * work correctly (regression test for #4147).
 *
 * The fix for #4118 initially broke Scala's Either type, which has
 * two type parameters where only one might be a wildcard. This test
 * ensures that such types continue to work correctly.
 */
class MultiParamWildcard4147Test extends DatabindTestUtil
{
    // Simulates Either<L, R> pattern
    static class Pair<L, R> {
        public L left;
        public R right;

        public Pair() {}
        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }
    }

    static class Container {
        public List<Pair<String, ?>> pairs;

        public Container() {}
        public Container(List<Pair<String, ?>> pairs) {
            this.pairs = pairs;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // for [databind#4147]
    @Test
    void multiParamWithWildcard() throws Exception {
        String json = a2q("{'pairs':[{'left':'test','right':123}]}");
        Container result = MAPPER.readValue(json, Container.class);

        assertNotNull(result.pairs);
        assertEquals(1, result.pairs.size());
        assertEquals("test", result.pairs.get(0).left);
        assertNotNull(result.pairs.get(0).right);
        // Right side should be deserialized as Integer (not LinkedHashMap)
        assertEquals(Integer.class, result.pairs.get(0).right.getClass());
        assertEquals(123, result.pairs.get(0).right);
    }

    // Additional test: both parameters are wildcards
    @Test
    void multiParamWithBothWildcards() throws Exception {
        String json = a2q("{'left':'hello','right':456}");

        Pair<?, ?> result = MAPPER.readValue(json, Pair.class);

        assertNotNull(result);
        assertEquals("hello", result.left);
        assertEquals(456, result.right);
    }
}
