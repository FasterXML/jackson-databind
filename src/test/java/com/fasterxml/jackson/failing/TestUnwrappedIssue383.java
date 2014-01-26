package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestUnwrappedIssue383 extends BaseMapTest
{
    // [Issue#383]
    static class RecursivePerson {
        public String name;
        public int age;
        @JsonUnwrapped(prefix="child.") public RecursivePerson child;
    }

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testRecursiveUsage() throws Exception
    {
        final String JSON = "{ 'name': 'Bob', 'age': 45, 'gender': 0, 'child.name': 'Bob jr', 'child.age': 15 }";
        RecursivePerson p = MAPPER.readValue(aposToQuotes(JSON), RecursivePerson.class);
        assertNotNull(p);
        assertEquals("Bob", p.name);
        assertNotNull(p.child);
        assertEquals("Bob jr", p.child.name);
    }
}
