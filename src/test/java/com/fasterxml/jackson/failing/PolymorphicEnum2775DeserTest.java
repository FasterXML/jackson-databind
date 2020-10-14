package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PolymorphicEnum2775DeserTest extends BaseMapTest
{
    // [databind#2775]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME
// work-around:
//            , include = JsonTypeInfo.As.WRAPPER_ARRAY
            )
    @JsonSubTypes(@JsonSubTypes.Type(TestEnum2775.class))
    interface Base2775 {}

    @JsonTypeName("Test")
    enum TestEnum2775 implements Base2775 {
        VALUE;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2775]
    public void testIssue2775() throws Exception
    {
        final Base2775 testValue = TestEnum2775.VALUE;
        String json = MAPPER.writeValueAsString(testValue);
//System.err.println("JSON: "+json);

        Base2775 deserializedValue = MAPPER.readerFor(Base2775.class)
                .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .readValue(json);
        assertEquals(testValue, deserializedValue);
    }

}
