package com.fasterxml.jackson.databind.objectid;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6204]
/**
 * Tests for resolution of forward references within Collections and Maps: verifies
 * that ordering is retained regardless of the order in which references resolve, and
 * that resolving them does not take time quadratic in the number of pending references.
 */
public class ForwardReferenceResolutionOrder6204Test extends DatabindTestUtil
{
    /**
     * Object id type that counts {@code equals()} calls, to measure -- deterministically,
     * without relying on timing -- how much work resolution of forward references takes.
     */
    static class CountingId
    {
        public final static AtomicLong EQUALS_CALLS = new AtomicLong();

        private final String _value;

        @JsonCreator
        public CountingId(String value) { _value = value; }

        @JsonValue
        public String value() { return _value; }

        @Override
        public boolean equals(Object o) {
            EQUALS_CALLS.incrementAndGet();
            return (o instanceof CountingId) && _value.equals(((CountingId) o)._value);
        }

        @Override
        public int hashCode() { return _value.hashCode(); }

        @Override
        public String toString() { return _value; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Node
    {
        public CountingId id;
        public String name;
    }

    private final static int COUNT = 2000;

    private final ObjectMapper MAPPER = newJsonMapper();

    private final TypeReference<List<Node>> LIST_TYPE = new TypeReference<List<Node>>() { };
    private final TypeReference<Map<String,Node>> MAP_TYPE = new TypeReference<Map<String,Node>>() { };

    // Collection: N references, then the definitions of the very same ids in reverse
    // order, so that every resolution matches the most recently added pending reference
    @Test
    public void reverseOrderResolutionInCollection() throws Exception
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < COUNT; ++i) {
            sb.append('\'').append(i).append("',");
        }
        for (int i = COUNT; --i >= 0; ) {
            sb.append("{'id':'").append(i).append("','name':'n").append(i).append("'}");
            sb.append((i == 0) ? ']' : ',');
        }

        CountingId.EQUALS_CALLS.set(0);
        List<Node> result = MAPPER.readValue(a2q(sb.toString()), LIST_TYPE);
        long comparisons = CountingId.EQUALS_CALLS.get();

        // First half is the references in document order, second half the definitions
        // in reverse: same instances, mirrored
        assertEquals(2 * COUNT, result.size());
        for (int i = 0; i < COUNT; ++i) {
            assertEquals(String.valueOf(i), result.get(i).id.value(),
                    "Wrong entry at #"+i);
            assertSame(result.get(i), result.get(2 * COUNT - 1 - i),
                    "Entry #"+i+" not same instance as its mirror");
        }

        _assertNotQuadratic(comparisons);
    }

    // Same, for Map values
    @Test
    public void reverseOrderResolutionInMap() throws Exception
    {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < COUNT; ++i) {
            sb.append("'ref").append(i).append("':'").append(i).append("',");
        }
        for (int i = COUNT; --i >= 0; ) {
            sb.append("'def").append(i).append("':");
            sb.append("{'id':'").append(i).append("','name':'n").append(i).append("'}");
            sb.append((i == 0) ? '}' : ',');
        }

        CountingId.EQUALS_CALLS.set(0);
        Map<String,Node> result = MAPPER.readValue(a2q(sb.toString()), MAP_TYPE);
        long comparisons = CountingId.EQUALS_CALLS.get();

        assertEquals(2 * COUNT, result.size());
        for (int i = 0; i < COUNT; ++i) {
            Node ref = result.get("ref"+i);
            assertNotNull(ref, "No entry for 'ref"+i+"'");
            assertSame(ref, result.get("def"+i),
                    "Entry 'ref"+i+"' not same instance as 'def"+i+"'");
        }
        // Insertion order of the result Map must match the document
        List<String> keys = new ArrayList<>(result.keySet());
        assertEquals("ref0", keys.get(0));
        assertEquals("ref"+(COUNT-1), keys.get(COUNT-1));
        assertEquals("def"+(COUNT-1), keys.get(COUNT));
        assertEquals("def0", keys.get(2 * COUNT - 1));

        _assertNotQuadratic(comparisons);
    }

    // Ordering must be retained for the plain, small cases too
    @Test
    public void orderRetainedWithInterleavedReferences() throws Exception
    {
        // "1" and "0" are forward references, "a"/"b" plain values in between
        String json = a2q("['1',{'id':'a','name':'A'},'0',"
                +"{'id':'b','name':'B'},{'id':'1','name':'One'},{'id':'0','name':'Zero'}]");
        List<Node> result = MAPPER.readValue(json, LIST_TYPE);

        assertEquals(6, result.size());
        String[] expected = new String[] { "1", "a", "0", "b", "1", "0" };
        for (int i = 0; i < expected.length; ++i) {
            assertEquals(expected[i], result.get(i).id.value(), "Wrong entry at #"+i);
        }
        assertSame(result.get(0), result.get(4));
        assertSame(result.get(2), result.get(5));
    }

    // Repeated references to one and the same id must all resolve, in order
    @Test
    public void repeatedReferencesToSameId() throws Exception
    {
        String json = a2q("['x','x',{'id':'y','name':'Y'},'x',{'id':'x','name':'X'}]");
        List<Node> result = MAPPER.readValue(json, LIST_TYPE);

        assertEquals(5, result.size());
        String[] expected = new String[] { "x", "x", "y", "x", "x" };
        for (int i = 0; i < expected.length; ++i) {
            assertEquals(expected[i], result.get(i).id.value(), "Wrong entry at #"+i);
        }
        Node x = result.get(4);
        assertSame(x, result.get(0));
        assertSame(x, result.get(1));
        assertSame(x, result.get(3));
    }

    private void _assertNotQuadratic(long comparisons)
    {
        // Quadratic resolution needs ~COUNT * (COUNT+1) / 2 comparisons; linear
        // resolution a small multiple of COUNT. Bound generously in between.
        long max = 20L * COUNT;
        if (comparisons > max) {
            fail("Resolving "+COUNT+" forward references took "+comparisons
                    +" id comparisons, expected at most "+max);
        }
    }
}
