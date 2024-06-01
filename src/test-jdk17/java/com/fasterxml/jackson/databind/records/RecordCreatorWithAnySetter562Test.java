package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#562] Allow @JsonAnySetter on Creator constructors
public class RecordCreatorWithAnySetter562Test
        extends DatabindTestUtil
{
    record RecordWithAnySetterCtor(int id,
                                   Map<String, Integer> additionalProperties) {
        @JsonCreator
        public RecordWithAnySetterCtor(@JsonProperty("regular") int id,
                                       @JsonAnySetter Map<String, Integer> additionalProperties
        ) {
            this.id = id;
            this.additionalProperties = additionalProperties;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, alternate constructors
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testRecordWithAnySetterCtor() throws Exception
    {
        // First, only regular property mapped
        RecordWithAnySetterCtor result = MAPPER.readValue(a2q("{'regular':13}"),
                RecordWithAnySetterCtor.class);
        assertEquals(13, result.id);
        assertEquals(0, result.additionalProperties.size());

        // Then with unknown properties
        result = MAPPER.readValue(a2q("{'regular':13, 'unknown':99, 'extra':-1}"),
                RecordWithAnySetterCtor.class);
        assertEquals(13, result.id);
        assertEquals(Integer.valueOf(99), result.additionalProperties.get("unknown"));
        assertEquals(Integer.valueOf(-1), result.additionalProperties.get("extra"));
        assertEquals(2, result.additionalProperties.size());
    }

}
