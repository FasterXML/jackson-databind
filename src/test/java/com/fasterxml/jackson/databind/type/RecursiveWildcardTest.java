package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class RecursiveWildcardTest extends BaseMapTest {
    public void test() throws JsonProcessingException {
        ObjectMapper mapper = newJsonMapper();

        mapper.readValue("[[[]]]", new TypeReference<Tree<?>>() {
        });
    }

    public static class Tree<T extends Tree<?>> {

        private final List<T> children;

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
