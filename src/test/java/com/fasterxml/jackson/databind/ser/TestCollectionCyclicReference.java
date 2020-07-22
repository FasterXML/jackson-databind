package com.fasterxml.jackson.databind.ser;

import java.util.*;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.*;

/**
 * Simple unit tests to verify that it is possible to handle
 * potentially cyclic structures, as long as object graph itself
 * is not cyclic. This is the case for directed hierarchies like
 * trees and DAGs.
 */
public class TestCollectionCyclicReference
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */


    @JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class
        , property = "id"
        , scope = Bean.class
    )
    static class Bean {
        final int _id;
        Bean _next;
        final String _name;

        public Bean(int id, Bean next, String name) {
            _id = id;
            _next = next;
            _name = name;
        }

        public int getId() { return _id; }

        public Bean getNext() { return _next; }

        public String getName() { return _name; }

        public void assignNext(Bean n) { _next = n; }
    }

    /*
    /**********************************************************
    /* Types
    /**********************************************************
     */

    public void testLinked() throws Exception {
        Bean sameChild = new Bean(3, null, "sameChild");
        Bean first = new Bean(1, sameChild, "first");
        Bean second = new Bean(2, sameChild, "second");
        sameChild.assignNext(first);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.HANDLE_CIRCULAR_REFERENCE_INDIVIDUALLY_FOR_COLLECTIONS);
        final String result = mapper.writeValueAsString(Arrays.asList(first, second));

        final StringBuilder expected = new StringBuilder();
        expected.append("[");
        expected.append("{\"id\":1,\"name\":\"first\",\"next\":");
        expected.append(
            "{\"id\":3,\"name\":\"sameChild\",\"next\":1}}"); // 1 has been visited this iteration => next is a reference
        expected.append(",{\"id\":2,\"name\":\"second\",\"next\":");
        expected.append(
            "{\"id\":3,\"name\":\"sameChild\",\"next\":"); // 1 has noy been visited this iteration => next is fully serialized
        expected
            .append("{\"id\":1,\"name\":\"first\",\"next\":3}"); // 3 has been visited this iteration => next is a reference
        expected.append("}");
        expected.append("}");
        expected.append("]");

        assertEquals(result, expected.toString());
    }
}
