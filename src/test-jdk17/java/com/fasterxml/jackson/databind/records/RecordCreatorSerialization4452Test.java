package com.fasterxml.jackson.databind.records;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4452] : JsonProperty not serializing field names properly on JsonCreator in record #4452
class RecordCreatorSerialization4452Test {

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

    private final ObjectMapper OBJECT_MAPPER =
            JsonMapper.builder()
                    .annotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector())
                    .build();

    @Test
    public void testPlain() throws Exception
    {
        String result = OBJECT_MAPPER.writeValueAsString(new PlainTestObject("test", 1));
        assertEquals("{\"strField\":\"test\",\"intField\":1}", result);
    }

    @Test
    public void testWithCreator() throws Exception
    {
        String json = OBJECT_MAPPER
                .writeValueAsString(new CreatorTestObject("test", 2, 1));
        //System.err.println("JSON: "+json);

        @SuppressWarnings("unchecked")
        Map<String, Object> asMap = (Map<String, Object>) OBJECT_MAPPER.readValue(json, Map.class);
        assertEquals(new HashSet<>(Arrays.asList("intField", "strField")), asMap.keySet());
    }
}
