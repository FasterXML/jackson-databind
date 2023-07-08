package com.fasterxml.jackson.databind.ser.dos;

import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.CyclicTypeSerTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicDataSerTest
    extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testLinkedAndCyclic() throws Exception {
        CyclicTypeSerTest.Bean bean = new CyclicTypeSerTest.Bean(null, "last");
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
            fail("expected JsonMappingException");
        } catch (JsonMappingException jmex) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamWriteConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue("JsonMappingException message is as expected?",
                    jmex.getMessage().startsWith(exceptionPrefix));
        }
    }
}
