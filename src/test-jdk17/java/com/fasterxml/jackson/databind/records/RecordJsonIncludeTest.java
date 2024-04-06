package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to verify that `@JsonInclude` annotation works with Records on both constructor parameters and getters.
 */
public class RecordJsonIncludeTest extends DatabindTestUtil
{
    record AnnotatedParamRecordClass(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String omitFieldIfNull,
        String standardField
    ) {
    }

    record AnnotatedGetterRecordClass(
        String omitFieldIfNull,
        String standardField
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String omitFieldIfNull() {
            return omitFieldIfNull;
        }
    }

    private ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testJsonIncludeOnRecordParam() throws Exception
    {
        // Test with constructor parameter
        assertEquals(a2q("{'standardField':'def'}"),
            MAPPER.writeValueAsString(new AnnotatedParamRecordClass(null, "def")));
        // Test with getter
        assertEquals(a2q("{'standardField':'def'}"),
            MAPPER.writeValueAsString(new AnnotatedGetterRecordClass(null, "def")));
    }
}
