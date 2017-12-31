package com.fasterxml.jackson.databind.deser.creators.jdk8;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;

public class EnumNamingTest
{
    enum SurprisingEnum32 {
        @JsonProperty("customValue")
        ENUM_NAME;
    }
    
    // for [module-parameter-names#32]
    @Test
    public void testCustomEnumName() throws Exception
    {
        final String EXP = "\"customValue\"";
        
        // First, verify default handling

        String json = new ObjectMapper()
            .writeValueAsString(SurprisingEnum32.ENUM_NAME);
        assertEquals(EXP, json);

        // and then with parameter names module
        final ObjectMapper mapperWithNames = new ObjectMapper();
        json = mapperWithNames.writeValueAsString(SurprisingEnum32.ENUM_NAME);
        assertEquals(EXP, json);

        // plus read back:
        SurprisingEnum32 value = mapperWithNames.readValue(json, SurprisingEnum32.class);
        assertEquals(SurprisingEnum32.ENUM_NAME, value);
    }
}
