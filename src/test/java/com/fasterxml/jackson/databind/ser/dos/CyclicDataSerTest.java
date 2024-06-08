package com.fasterxml.jackson.databind.ser.dos;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.StreamWriteConstraints;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicDataSerTest
    extends DatabindTestUtil
{
    static class CyclicBean
    {
        CyclicBean _next;
        final String _name;

        public CyclicBean(CyclicBean next, String name) {
            _next = next;
            _name = name;
        }

        public CyclicBean getNext() { return _next; }
        public String getName() { return _name; }

        public void assignNext(CyclicBean n) { _next = n; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testLinkedAndCyclic() throws Exception {
        CyclicBean bean = new CyclicBean(null, "last");
        bean.assignNext(bean);
        try {
            writeAndMap(MAPPER, bean);
            fail("expected InvalidDefinitionException");
        } catch (InvalidDefinitionException idex) {
            assertTrue(idex.getMessage().startsWith("Direct self-reference leading to cycle"),
                "InvalidDefinitionException message is as expected?");
        }
    }

    @Test
    public void testListWithSelfReference() throws Exception {
        List<Object> list = new ArrayList<>();
        list.add(list);
        try {
            writeAndMap(MAPPER, list);
            fail("expected DatabindException");
        } catch (DatabindException e) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamWriteConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue(e.getMessage().startsWith(exceptionPrefix),
                "DatabindException message is as expected?");
        }
    }
}
