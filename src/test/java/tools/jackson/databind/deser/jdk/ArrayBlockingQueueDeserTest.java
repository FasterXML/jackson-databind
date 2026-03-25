package tools.jackson.databind.deser.jdk;

import java.util.concurrent.ArrayBlockingQueue;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ArrayBlockingQueueDeserializer}.
 */
public class ArrayBlockingQueueDeserTest extends DatabindTestUtil
{
    public static class StringQueueBean {
        public ArrayBlockingQueue<String> items;
    }

    public static class IntQueueBean {
        public ArrayBlockingQueue<Integer> numbers;
    }

    public static class PojoItem {
        public String name;
        public int value;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Tests for basic deserialization
    /**********************************************************
     */

    @Test
    void testEmptyQueue() throws Exception
    {
        ArrayBlockingQueue<String> result = MAPPER.readValue("[]",
                new TypeReference<ArrayBlockingQueue<String>>() { });
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testSingleElementQueue() throws Exception
    {
        ArrayBlockingQueue<String> result = MAPPER.readValue("[\"hello\"]",
                new TypeReference<ArrayBlockingQueue<String>>() { });
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("hello", result.poll());
    }

    @Test
    void testMultipleElementQueue() throws Exception
    {
        ArrayBlockingQueue<Integer> result = MAPPER.readValue("[1, 2, 3, 4, 5]",
                new TypeReference<ArrayBlockingQueue<Integer>>() { });
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(Integer.valueOf(1), result.poll());
        assertEquals(Integer.valueOf(2), result.poll());
        assertEquals(Integer.valueOf(3), result.poll());
        assertEquals(Integer.valueOf(4), result.poll());
        assertEquals(Integer.valueOf(5), result.poll());
    }

    @Test
    void testQueueWithStrings() throws Exception
    {
        ArrayBlockingQueue<String> result = MAPPER.readValue(
                a2q("['a','b','c']"),
                new TypeReference<ArrayBlockingQueue<String>>() { });
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.poll());
        assertEquals("b", result.poll());
        assertEquals("c", result.poll());
    }

    /*
    /**********************************************************
    /* Tests with POJOs
    /**********************************************************
     */

    @Test
    void testQueueWithPojo() throws Exception
    {
        ArrayBlockingQueue<PojoItem> result = MAPPER.readValue(
                a2q("[{'name':'first','value':1},{'name':'second','value':2}]"),
                new TypeReference<ArrayBlockingQueue<PojoItem>>() { });
        assertNotNull(result);
        assertEquals(2, result.size());
        PojoItem item = result.poll();
        assertEquals("first", item.name);
        assertEquals(1, item.value);
    }

    /*
    /**********************************************************
    /* Tests with bean properties
    /**********************************************************
     */

    @Test
    void testQueueAsProperty() throws Exception
    {
        StringQueueBean result = MAPPER.readValue(
                a2q("{'items':['x','y','z']}"), StringQueueBean.class);
        assertNotNull(result);
        assertNotNull(result.items);
        assertEquals(3, result.items.size());
        assertEquals("x", result.items.poll());
    }

    @Test
    void testEmptyQueueAsProperty() throws Exception
    {
        StringQueueBean result = MAPPER.readValue(
                a2q("{'items':[]}"), StringQueueBean.class);
        assertNotNull(result);
        assertNotNull(result.items);
        assertEquals(0, result.items.size());
    }

    @Test
    void testIntQueueAsProperty() throws Exception
    {
        IntQueueBean result = MAPPER.readValue(
                a2q("{'numbers':[10,20,30]}"), IntQueueBean.class);
        assertNotNull(result);
        assertNotNull(result.numbers);
        assertEquals(3, result.numbers.size());
        assertEquals(Integer.valueOf(10), result.numbers.poll());
        assertEquals(Integer.valueOf(20), result.numbers.poll());
        assertEquals(Integer.valueOf(30), result.numbers.poll());
    }

    /*
    /**********************************************************
    /* Tests for round-trip serialization
    /**********************************************************
     */

    @Test
    void testRoundTrip() throws Exception
    {
        ArrayBlockingQueue<String> orig = new ArrayBlockingQueue<>(5);
        orig.add("alpha");
        orig.add("beta");
        orig.add("gamma");

        String json = MAPPER.writeValueAsString(orig);
        ArrayBlockingQueue<String> result = MAPPER.readValue(json,
                new TypeReference<ArrayBlockingQueue<String>>() { });
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("alpha", result.poll());
        assertEquals("beta", result.poll());
        assertEquals("gamma", result.poll());
    }

    @Test
    void testNullElementsInQueueFails() throws Exception
    {
        // ArrayBlockingQueue does NOT allow null elements; deserialization
        // should fail (ABQ constructor rejects nulls)
        try {
            MAPPER.readValue(
                    a2q("[null, 'hello', null]"),
                    new TypeReference<ArrayBlockingQueue<String>>() { });
            fail("Should not pass with null elements in ArrayBlockingQueue");
        } catch (NullPointerException e) {
            // Expected: ArrayBlockingQueue constructor rejects null elements
        }
    }

    @Test
    void testLargeQueue() throws Exception
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) sb.append(',');
            sb.append(i);
        }
        sb.append(']');

        ArrayBlockingQueue<Integer> result = MAPPER.readValue(sb.toString(),
                new TypeReference<ArrayBlockingQueue<Integer>>() { });
        assertNotNull(result);
        assertEquals(100, result.size());
        // Verify first and last
        assertEquals(Integer.valueOf(0), result.peek());
    }
}
