package com.fasterxml.jackson.databind.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.records.Jdk8ConstructorParameterNameAnnotationIntrospector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#4452] : JsonProperty not serializing field names properly on JsonCreator in record #4452
class RecordCreatorSerialization4452Test {

    private final ObjectMapper OBJECT_MAPPER =
            JsonMapper.builder()
                    .annotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector())
                    .build();

    public record PlainTestObject(
            @JsonProperty("strField") String testFieldName,
            @JsonProperty("intField") Integer testOtherField
    ) { }

    public record CreatorTestObject(
            String testFieldName,
            Integer testOtherField
    ) {
        @JsonCreator
        public CreatorTestObject(
                @JsonProperty("strField") String testFieldName,
                @JsonProperty("someOtherIntField") Integer testOtherIntField,
                @JsonProperty("intField") Integer testOtherField)
        {
            this(testFieldName, testOtherField + testOtherIntField);
        }
    }

    // supposed to pass, and yes it does
    @Test
    public void testPlain()
            throws Exception
    {
        String result = OBJECT_MAPPER.writeValueAsString(new PlainTestObject("test", 1));
        assertEquals("{\"strField\":\"test\",\"intField\":1}", result);
    }

    // Should pass but doesn't
    // It did pass in 2.15 or earlier versions, but it fails in 2.16 or later
    @Test
    public void testWithCreator()
            throws Exception
    {
        String result = OBJECT_MAPPER
                .writeValueAsString(new CreatorTestObject("test", 2, 1));
        /*
        Serializes to:

            {"testFieldName":"test","testOtherField":3}

         */
        assertTrue(result.contains("intField"));
        assertTrue(result.contains("strField"));
    }

}