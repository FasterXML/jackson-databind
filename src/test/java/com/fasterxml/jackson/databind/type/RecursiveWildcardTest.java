package com.fasterxml.jackson.databind.type;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RecursiveWildcardTest extends BaseMapTest
{
    public void test() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();

        Tree<?> tree = mapper.readValue("[[[]]]", new TypeReference<Tree<?>>() {
        });

        assertEquals(1, tree.children.size());
        assertEquals(1, tree.children.get(0).children.size());
        assertEquals(0, tree.children.get(0).children.get(0).children.size());
    }

    public static class Tree<T extends Tree<?>> {

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
}
