package com.fasterxml.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Problem with recursive definition of unwrapping
public class TestUnwrappedRecursive383 extends DatabindTestUtil
{
    // [databind#383]
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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testRecursiveUsage() throws Exception
    {
        final String JSON = "{ 'name': 'Bob', 'age': 45, 'gender': 0, 'child.name': 'Bob jr', 'child.age': 15 }";
        RecursivePerson p = MAPPER.readValue(a2q(JSON), RecursivePerson.class);
        assertNotNull(p);
        assertEquals("Bob", p.name);
        assertNotNull(p.child);
        assertEquals("Bob jr", p.child.name);
    }
}
