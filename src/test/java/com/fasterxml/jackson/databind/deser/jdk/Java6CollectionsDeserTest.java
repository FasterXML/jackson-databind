package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Deque;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to ensure that we can handle 1.6-only types, even if
 * registrations are done without direct refs
 */
public class Java6CollectionsDeserTest
{
    // for [databind#216]
    @Test
    public void test16Types() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        Deque<?> dq = mapper.readValue("[1]", Deque.class);
        assertNotNull(dq);
        assertEquals(1, dq.size());
        assertTrue(dq instanceof Deque<?>);

        NavigableSet<?> ns = mapper.readValue("[ true ]", NavigableSet.class);
        assertEquals(1, ns.size());
        assertTrue(ns instanceof NavigableSet<?>);
    }
}
