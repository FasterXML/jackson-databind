package com.fasterxml.jackson.failing;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test to prove that serialization does not
 * work in depth but in width. This causes elements
 * at the same level to be sometimes serialized as
 * IDs when they could have not yet been visited.
 */
class CyclicRefViaCollection3069Test extends DatabindTestUtil {
    // [databind#3069]
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class
            , property = "id"
            , scope = Bean.class
    )
    static class Bean implements Comparable<Bean> {
        final int _id;
        final String _name;
        Collection<Bean> _next;

        public Bean(int id, String name) {
            _id = id;
            _name = name;
        }

        public int getId() {
            return _id;
        }

        public Collection<Bean> getNext() {
            return _next;
        }

        public void setNext(final Collection<Bean> n) {
            _next = n;
        }

        public String getName() {
            return _name;
        }

        @Override
        public int compareTo(Bean o) {
            if (o == null) {
                return -1;
            }
            return Integer.compare(_id, ((Bean) o).getId());
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3069]
    @Test
    void serializationCollection() throws Exception {
        testSerializationCollection(MAPPER, new TreeSet<>(abc()));
    }

    @Test
    void serializationList() throws Exception {
        testSerializationIndexedList(MAPPER, abc());
    }

    @Test
    void serializationIterable() throws Exception {
        testSerializationIterable(MAPPER, new PriorityQueue<>(abc()));
    }

    @Test
    void serializationIterator() throws Exception {
        testSerializationIterator(MAPPER, abc().iterator());
    }

    private List<Bean> abc() {
        final Bean a = new Bean(1, "A");
        final Bean b = new Bean(2, "B");
        final Bean c = new Bean(3, "C");

        a.setNext(Arrays.asList(a, c));
        b.setNext(Arrays.asList(a, c));
        c.setNext(Arrays.asList(a, b));

        return Arrays.asList(a, b, c);
    }

    private void testSerializationCollection(final ObjectMapper mapper, final Collection<Bean> collection)
            throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(collection));
    }

    private void testSerializationIndexedList(final ObjectMapper mapper, final List<Bean> list) throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(list));
    }

    private void testSerializationIterable(final ObjectMapper mapper, final Iterable<Bean> iterable)
            throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(iterable));
    }

    private void testSerializationIterator(final ObjectMapper mapper, final Iterator<Bean> iterator)
            throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(iterator));
    }

    private String getExpectedResult() {

        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("{\"id\":1,\"name\":\"A\",\"next\":[");
        builder.append("1, {\"id\":3,\"name\":\"C\",\"next\":[");
        builder.append("1, {\"id\":2,\"name\":\"B\",\"next\":[");
        builder.append("1, 3");
        builder.append("]}");
        builder.append("]}");
        builder.append("]}");
        builder.append(", {\"id\":2,\"name\":\"B\",\"next\":[");
        builder.append("{\"id\":1,\"name\":\"A\",\"next\":[");
        builder.append("1, {\"id\":3,\"name\":\"C\",\"next\":[1, 2]}");
        builder.append("]}");
        builder.append(", {\"id\":3,\"name\":\"C\",\"next\":[");
        builder.append("{\"id\":1,\"name\":\"A\",\"next\":[1, 3]}, 2");
        builder.append("]}");
        builder.append("]}");
        builder.append(", {\"id\":3,\"name\":\"C\",\"next\":[");
        builder.append("{\"id\":1,\"name\":\"A\",\"next\":[1, 3]}");
        builder.append(", {\"id\":2,\"name\":\"B\",\"next\":[");
        builder.append("{\"id\":1,\"name\":\"A\",\"next\":[1, 3]}, 3");
        builder.append("]}");
        builder.append("]}");
        builder.append("]");
        return builder.toString();
    }
}
