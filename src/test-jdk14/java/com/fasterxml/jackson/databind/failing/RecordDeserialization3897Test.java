package com.fasterxml.jackson.databind.failing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;

// [databinding#3897] This is failing test for `Record` class deserialization with single field annotated with
// `JsonProperty.Access.WRITE_ONLY`. Regression from Jackson 2.14.2
public class RecordDeserialization3897Test extends BaseMapTest {

    record Example(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String value
    ) {}

    // Passes in 2.14.2, but does not in 2.15.0 
    public void testRecordWithWriteOnly() throws Exception {
        final String JSON = a2q("{'value':'foo'}");

        Example result = newJsonMapper().readValue(JSON, Example.class);

        assertEquals("foo", result.value());
    }
}
