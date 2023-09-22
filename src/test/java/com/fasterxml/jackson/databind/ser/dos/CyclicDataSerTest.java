package com.fasterxml.jackson.databind.ser.dos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.StreamWriteConstraints;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicDataSerTest
    extends BaseMapTest
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

    public void testLinkedAndCyclic() throws Exception {
        CyclicBean bean = new CyclicBean(null, "last");
        bean.assignNext(bean);
        try {
            writeAndMap(MAPPER, bean);
            fail("expected InvalidDefinitionException");
        } catch (InvalidDefinitionException idex) {
            assertTrue("InvalidDefinitionException message is as expected?",
                    idex.getMessage().startsWith("Direct self-reference leading to cycle"));
        }
    }

    public void testListWithSelfReference() throws Exception {
        List<Object> list = new ArrayList<>();
        list.add(list);
        try {
            writeAndMap(MAPPER, list);
            fail("expected DatabindException");
        } catch (DatabindException e) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamWriteConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue("DatabindException message is as expected?",
                    e.getMessage().startsWith(exceptionPrefix));
        }
    }
}
