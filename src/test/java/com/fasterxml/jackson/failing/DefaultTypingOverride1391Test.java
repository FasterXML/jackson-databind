package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;

// [databind#1391]
public class DefaultTypingOverride1391Test extends BaseMapTest
{
    final static ObjectMapper WITH_OBJECT_AND_NON_CONCRETE = new ObjectMapper().enableDefaultTypingAsProperty(
            ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "$type");

    final static ObjectMapper PLAIN_OBJECT_MAPPER = new ObjectMapper();

    static class MyClass {

        private final SortedSet<String> treeSetStrings = new TreeSet<>();

        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public SortedSet<String> getTreeSetStrings() {
            return this.treeSetStrings;
        }
    }

    public void testCollectionTyping() throws Exception {

        final MyClass toSerialize = new MyClass();

        final String fromPlainObjectMapper = PLAIN_OBJECT_MAPPER.writeValueAsString(toSerialize);
        System.out.println("\nfrom plain ObjectMapper:\n" + fromPlainObjectMapper);

        final String fromObjectMapperWithDefaultTyping = WITH_OBJECT_AND_NON_CONCRETE.writeValueAsString(toSerialize);
        System.out.println("\nfrom ObjectMapper with default typing:\n" + fromObjectMapperWithDefaultTyping);

        assertEquals(fromPlainObjectMapper, fromObjectMapperWithDefaultTyping);
    }
}
