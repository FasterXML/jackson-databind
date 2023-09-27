package com.fasterxml.jackson.databind.type;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;

public class RecursiveWildcardTest extends BaseMapTest
{
    static class Tree<T extends Tree<?>> {

        final List<T> children;

        @SuppressWarnings("DataFlowIssue")
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Tree(List<T> children) {
            if (!children.stream().allMatch(c -> c instanceof Tree<?>)) {
                throw new IllegalArgumentException("Incorrect type");
            }
            this.children = children;
        }
    }

    static class TestAttribute<T extends TestAttribute<?>> {

        public List<T> attributes;

        public TestAttribute() { }

        public TestAttribute(List<T> attributes) {
            this.attributes = attributes;
        }
    }

    static class TestObject {

        public List<TestAttribute<?>> attributes = new ArrayList<>();

        public TestObject() { }

        public TestObject(List<TestAttribute<?>> attributes) {
            this.attributes = attributes;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testRecursiveWildcard() throws Exception
    {
        Tree<?> tree = MAPPER.readValue("[[[]]]", new TypeReference<Tree<?>>() {
        });

        assertEquals(1, tree.children.size());
        assertEquals(1, tree.children.get(0).children.size());
        assertEquals(0, tree.children.get(0).children.get(0).children.size());
    }

    public void testWildcard() throws Exception
    {
        TestAttribute a = new TestAttribute(null);
        TestAttribute b = new TestAttribute(_listOf(a));
        TestAttribute c = new TestAttribute(_listOf(b));
        TestObject test = new TestObject(_listOf(c));

        String serialized = MAPPER.writeValueAsString(test);
        System.out.println(serialized);

        TestObject deserialized = MAPPER.readValue(serialized, TestObject.class);
        System.out.println(deserialized.attributes.get(0).attributes.get(0).getClass().getName());
    }

    private <T> List<T> _listOf(T elem) {
        ArrayList<T> list = new ArrayList<>();
        list.add(elem);
        return list;
    }
}
