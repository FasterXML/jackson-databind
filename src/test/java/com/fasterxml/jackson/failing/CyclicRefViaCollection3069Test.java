package com.fasterxml.jackson.failing;

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
// 02-Jul-2021, tatu: not sure if this is valid, but adding for further
//   inspection
public class CyclicRefViaCollection3069Test
    extends BaseMapTest
{
    // [databind#3069]
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class
            , property = "id"
            , scope = Bean.class
    )
    static class Bean implements Comparable<Bean>
    {
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#3069]
    public void testSerializationCollection() throws Exception
    {
        testSerializationCollection(MAPPER, new TreeSet<>(abc()));
        //testSerializationEnumSet(MAPPER, EnumSet.of(addEnum(BeanEnum.class, a), addEnum(BeanEnum.class, b)));
    }

    public void testSerializationList() throws Exception
    {
        testSerializationIndexedList(MAPPER, abc());
    }

    public void testSerializationIterable() throws Exception
    {
        testSerializationIterable(MAPPER, new PriorityQueue<>(abc()));
    }

    public void testSerializationIterator() throws Exception
    {
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
}
