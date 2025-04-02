package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RecordWithReadOnly5049Test extends DatabindTestUtil
{
    record ReadOnly5049(
            @JsonProperty(value = "a", access = JsonProperty.Access.READ_ONLY) String a,
            @JsonProperty(value = "b", access = JsonProperty.Access.READ_ONLY) String b) {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testRoundtrip() throws Exception
    {
        String string = MAPPER.writeValueAsString(new ReadOnly5049 ("hello", "world"));
        assertNotNull(MAPPER.readerFor(ReadOnly5049.class).readValue(string));
    } 
}
