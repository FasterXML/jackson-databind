package com.fasterxml.jackson.databind.records;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#4175]
public class RecordPrivate4175Test extends DatabindTestUtil
{
    private static record PrivateTextRecord4175(String text) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    // for [databind#4175]
    @Test
    public void testSerializePrivateTextRecord() throws Exception {
        PrivateTextRecord4175 textRecord = new PrivateTextRecord4175("anything");
        String json = MAPPER.writeValueAsString(textRecord);
        final Object EXP = Collections.singletonMap("text", "anything");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    @Test
    public void testDeserializePrivateTextRecord() throws Exception {
        assertEquals(new PrivateTextRecord4175("anything"),
                MAPPER.readValue("{\"text\":\"anything\"}", PrivateTextRecord4175.class));
    }
}
