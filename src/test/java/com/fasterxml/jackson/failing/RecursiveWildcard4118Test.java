package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests for [databind#4118]
class RecursiveWildcard4118Test extends DatabindTestUtil {
    static class Tree<T extends Tree<?>> {

        final List<T> children;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Tree(List<T> children) {
            if (!children.stream().allMatch(c -> c instanceof Tree<?>)) {
                throw new IllegalArgumentException("Incorrect type");
            }
            this.children = children;
        }
    }

    static class TestAttribute4118<T extends TestAttribute4118<?>> {

        public List<T> attributes;

        public TestAttribute4118() {
        }

        public TestAttribute4118(List<T> attributes) {
            this.attributes = attributes;
        }
    }

    static class TestObject4118 {

        public List<TestAttribute4118<?>> attributes = new ArrayList<>();

        public TestObject4118() {
        }

        public TestObject4118(List<TestAttribute4118<?>> attributes) {
            this.attributes = attributes;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // for [databind#4118]
    @Test
    void recursiveWildcard4118() throws Exception {
        Tree<?> tree = MAPPER.readValue("[[[]]]", new TypeReference<Tree<?>>() {
        });

        assertEquals(1, tree.children.size());
        assertEquals(1, tree.children.get(0).children.size());
        assertEquals(0, tree.children.get(0).children.get(0).children.size());
    }

    // for [databind#4118]
    @Test
    void deserWildcard4118() throws Exception {
        // Given
        TestAttribute4118<?> a = new TestAttribute4118<>(null);
        TestAttribute4118<?> b = new TestAttribute4118<>(_listOf(a));
        TestAttribute4118<?> c = new TestAttribute4118<>(_listOf(b));
        TestObject4118 test = new TestObject4118(_listOf(c));

        String serialized = MAPPER.writeValueAsString(test);

        // When
        TestObject4118 deserialized = MAPPER.readValue(serialized, TestObject4118.class);

        // Then
        assertType(deserialized.attributes.get(0).attributes.get(0), TestAttribute4118.class);
    }

    private <T> List<T> _listOf(T elem) {
        return Arrays.asList(elem);
    }
}
