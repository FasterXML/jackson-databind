package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RecordSerializationOrderTest extends BaseMapTest
{
    record NestedRecordOne(String id, String email, NestedRecordTwo nestedRecordTwo) {}
    record NestedRecordTwo(String id, String passport) {}

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, alternate constructors
    /**********************************************************************
     */

    public void testSerializationOrder() throws Exception {
        NestedRecordTwo nestedRecordTwo = new NestedRecordTwo("2", "111110");
        NestedRecordOne nestedRecordOne = new NestedRecordOne("1", "test@records.com", nestedRecordTwo);
        final String output = MAPPER.writeValueAsString(nestedRecordOne);
        final String expected = "{\"id\":\"1\",\"email\":\"test@records.com\",\"nestedRecordTwo\":{\"id\":\"2\",\"passport\":\"111110\"}}";
        assertEquals(expected, output);
    }
}
