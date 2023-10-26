package com.fasterxml.jackson.databind.jdk17;

import java.util.Collections;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// for [databind#4175]
public class RecordPrivate4175Test extends BaseMapTest
{
    private static record PrivateTextRecord4175(String text) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    // for [databind#4175]
    public void testSerializePrivateTextRecord() throws Exception {
        PrivateTextRecord4175 textRecord = new PrivateTextRecord4175("anything");
        String json = MAPPER.writeValueAsString(textRecord);
        final Object EXP = Collections.singletonMap("text", "anything");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    public void testDeserializePrivateTextRecord() throws Exception {
        assertEquals(new PrivateTextRecord4175("anything"),
                MAPPER.readValue("{\"text\":\"anything\"}", PrivateTextRecord4175.class));
    }
}
