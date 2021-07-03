package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

/**
 * Unit test to prove that serialization does not
 * work in depth but in width. This causes elements
 * at the same level to be sometimes serialized as
 * IDs when they could have not yet been visited.
 */
public class TestCollectionCyclicReference
        extends BaseMapTest {
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    public void testSerialization() throws Exception {

        final Bean a = new Bean(1, "A");
        final Bean b = new Bean(2, "B");
        final Bean c = new Bean(3, "C");

        a.setNext(Arrays.asList(a, c));
        b.setNext(Arrays.asList(a, c));
        c.setNext(Arrays.asList(a, b));

        final ObjectMapper mapper = new ObjectMapper();

        testSerializationCollection(mapper, new TreeSet<>(Arrays.asList(a, b, c)));
        //testSerializationEnumSet(mapper, EnumSet.of(addEnum(BeanEnum.class, a), addEnum(BeanEnum.class, b)));
        testSerializationIndexedList(mapper, Arrays.asList(a, b, c));
        testSerializationIterable(mapper, new PriorityQueue<>(Arrays.asList(a, b, c)));
        testSerializationIterator(mapper, Arrays.asList(a, b, c).iterator());
    }

    public void testSerializationCollection(final ObjectMapper mapper, final Collection<Bean> collection)
            throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(collection));
    }

    public void testSerializationEnumSet(final ObjectMapper mapper, final EnumSet<?> enumSet)
            throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(enumSet));
    }

    public void testSerializationIndexedList(final ObjectMapper mapper, final List<Bean> list) throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(list));
    }

    public void testSerializationIterable(final ObjectMapper mapper, final Iterable<Bean> iterable)
            throws Exception {
        assertEquals(getExpectedResult(), mapper.writeValueAsString(iterable));
    }

    public void testSerializationIterator(final ObjectMapper mapper, final Iterator<Bean> iterator)
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

    /*
    /**********************************************************
    /* Types
    /**********************************************************
     */

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class
            , property = "id"
            , scope = Bean.class
    )
    static class Bean implements Comparable {
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
        public int compareTo(Object o) {
            if (o == null) {
                return -1;
            }
            return o instanceof Bean ? Integer.compare(_id, ((Bean) o).getId()) : 0;
        }
    }
}