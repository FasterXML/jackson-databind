package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#562] Allow @JsonAnySetter on Creator constructors
// [databind#3439] Java Record @JsonAnySetter value is null after deserialization
public class RecordCreatorWithAnySetterTest
        extends DatabindTestUtil
{
    record RecordWithAnySetterCtor562(int id,
            Map<String, Integer> additionalProperties) {
        @JsonCreator
        public RecordWithAnySetterCtor562(@JsonProperty("regular") int id,
                @JsonAnySetter Map<String, Integer> additionalProperties
        ) {
            this.id = id;
            this.additionalProperties = additionalProperties;
        }
    }

    record TestRecord3439(
            @JsonProperty String field,
            @JsonAnySetter Map<String, Object> anySetter
        ) {}

    /*
    /**********************************************************************
    /* Test methods, alternate constructors
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#562]
    @Test
    public void testRecordWithAnySetterCtor() throws Exception
    {
        // First, only regular property mapped
        RecordWithAnySetterCtor562 result = MAPPER.readValue(a2q("{'regular':13}"),
                RecordWithAnySetterCtor562.class);
        assertEquals(13, result.id);
        assertEquals(0, result.additionalProperties.size());

        // Then with unknown properties
        result = MAPPER.readValue(a2q("{'regular':13, 'unknown':99, 'extra':-1}"),
                RecordWithAnySetterCtor562.class);
        assertEquals(13, result.id);
        assertEquals(Integer.valueOf(99), result.additionalProperties.get("unknown"));
        assertEquals(Integer.valueOf(-1), result.additionalProperties.get("extra"));
        assertEquals(2, result.additionalProperties.size());
    }

    // [databind#3439]
    @Test
    public void testJsonAnySetterOnRecord() throws Exception {
        String json = """
            {
                "field": "value",
                "unmapped1": "value1",
                "unmapped2": "value2"
            }
            """;

        TestRecord3439 result = MAPPER.readValue(json, TestRecord3439.class);

        assertEquals("value", result.field());
        assertEquals(Map.of("unmapped1", "value1", "unmapped2", "value2"),
                result.anySetter());
    }
}
