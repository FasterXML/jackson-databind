package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databinding#3897] This is failing test for `Record` class deserialization with single field annotated with
// `JsonProperty.Access.WRITE_ONLY`. Regression from Jackson 2.14.2
public class RecordDeserialization3897Test extends DatabindTestUtil {

    record Example(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String value
    ) {}

    // Passes in 2.14.2, but does not in 2.15.0
    @Test
    public void testRecordWithWriteOnly() throws Exception {
        final String JSON = a2q("{'value':'foo'}");

        Example result = newJsonMapper().readValue(JSON, Example.class);

        assertEquals("foo", result.value());
    }
}
