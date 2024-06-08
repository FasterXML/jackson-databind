package com.fasterxml.jackson.databind.jsontype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#2331]
public class GenericNestedType2331Test extends DatabindTestUtil
{
    static class SuperNode<T> { }
    static class SuperTestClass { }

    @SuppressWarnings("serial")
    static class Node<T extends SuperTestClass & Cloneable> extends SuperNode<Node<T>> implements Serializable {

        public List<Node<T>> children;

        public Node() {
            children = new ArrayList<Node<T>>();
        }

        /**
         * The Wildcard here seems to be the Issue.
         * If we remove this full getter, everything is working as expected.
         */
        public List<? extends SuperNode<Node<T>>> getChildren() {
            return children;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testGeneric2331() throws Exception {
        Node root = new Node();
        root.children.add(new Node());

        String json = newJsonMapper().writeValueAsString(root);
        assertNotNull(json);
    }
}
