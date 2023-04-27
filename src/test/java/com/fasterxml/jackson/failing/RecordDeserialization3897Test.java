package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;

// [databinding#3897] record class breaks deserialization when there's only a single field, and marked Access.WRITE_ONLY
public class RecordDeserialization3897Test extends BaseMapTest {

    public record Example(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String value
    ) {}

    public void testRecordWithWriteOnly() throws Exception {
        final String JSON = a2q("{'value':'foo'}");

        Example result = newJsonMapper().readValue(JSON, Example.class);
        
        assertEquals("foo", result.value());
    }
}
