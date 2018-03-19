package com.fasterxml.jackson.databind.deser.creators;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// for [databind#1392] (regression in 2.7 due to separation of array-delegating creator)
public class ArrayDelegatorCreatorForCollectionTest extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
    abstract static class UnmodifiableSetMixin {

        @JsonCreator
        public UnmodifiableSetMixin(Set<?> s) {}
    }

    public void testUnmodifiable() throws Exception
    {
        Class<?> unmodSetType = Collections.unmodifiableSet(Collections.<String>emptySet()).getClass();
        ObjectMapper mapper = ObjectMapper.builder()
                .enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .addMixIn(unmodSetType, UnmodifiableSetMixin.class)
                .build();
        final String EXPECTED_JSON = "[\""+unmodSetType.getName()+"\",[]]";
        Set<?> foo = mapper.readValue(EXPECTED_JSON, Set.class);
        assertTrue(foo.isEmpty());
    }
}
