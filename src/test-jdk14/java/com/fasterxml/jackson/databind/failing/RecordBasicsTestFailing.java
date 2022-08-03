package com.fasterxml.jackson.databind.failing;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.records.RecordBasicsTest;

/**
 * Tests in this class were moved from {@link RecordBasicsTest}.
 */
public class RecordBasicsTestFailing extends BaseMapTest {
    // [databind#2992]
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record SnakeRecord(String myId, String myValue){}

    private final ObjectMapper MAPPER = newJsonMapper();
  
    /*
    /**********************************************************************
    /* Test methods, naming strategy
    /**********************************************************************
     */

    // [databind#2992]
    // [databind#3102]: fails on JDK 16 which finally blocks mutation
    // of Record fields.
    public void testNamingStrategy() throws Exception
    {
        SnakeRecord input = new SnakeRecord("123", "value");
        String json = MAPPER.writeValueAsString(input);
        SnakeRecord output = MAPPER.readValue(json, SnakeRecord.class);
        assertEquals(input, output);
    }
}
