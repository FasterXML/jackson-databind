package com.fasterxml.jackson.databind.tofix;

import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

// [databind#5312] Include.NON_DEFAULT regression for objects with @JsonValue
public class JsonIncludeNonDefaultOnRecord5312Test
{
    record StringValue(String value) {
        @Override
        @JsonValue
        public String value() {
            return value;
        }
    }

    record Pojo1(StringValue value) { }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    record Pojo2(StringValue value) { }

    record Pojo3(@JsonInclude(JsonInclude.Include.NON_DEFAULT) StringValue value) { }


    @JacksonTestFailureExpected
    @Test
    void testSerialization()
        throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(NON_DEFAULT);

        //might be relevant for analysis, but does not affect test outcome
        MutableConfigOverride objectStringConfigOverride = objectMapper.configOverride(String.class);
        objectStringConfigOverride.setIncludeAsProperty(JsonInclude.Value.construct(NON_NULL, NON_NULL));

        //FAIL on jackson 2.18.2 / 2.20.0
        Assertions.assertEquals("{\"value\":\"\"}", objectMapper.writeValueAsString(new Pojo1(new StringValue(""))));
        //PASS
        Assertions.assertEquals("{\"value\":\"\"}", objectMapper.writeValueAsString(new Pojo2(new StringValue(""))));
        //FAIL on jackson 2.18.2 / 2.20.0
        Assertions.assertEquals("{\"value\":\"\"}", objectMapper.writeValueAsString(new Pojo3(new StringValue(""))));
    }
}
