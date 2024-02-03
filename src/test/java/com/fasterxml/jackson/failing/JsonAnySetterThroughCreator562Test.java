package com.fasterxml.jackson.failing;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class JsonAnySetterThroughCreator562Test {

    static class MyClass {
        public String field;
        public HashMap<String, String> anySetter;

        @JsonCreator
        public MyClass(
            @JsonProperty("field") String field,
            @JsonAnySetter HashMap<String, String> anySetter
        ) {
            this.field = field;
            this.anySetter = anySetter;
        }
    }

    @Test
    void testJsonAnySetterOnRecord() throws Exception {
        String json =
            "{\n" +
                "    \"field\": \"value\",\n" +
                "    \"unmapped1\": \"value1\",\n" +
                "    \"unmapped2\": \"value2\"\n" +
                "}";
        HashMap<String, String> expected = new HashMap<>();
        expected.put("unmapped1", "value1");
        expected.put("unmapped2", "value2");


        ObjectMapper objectMapper = newJsonMapper();

        MyClass deserialized = objectMapper.readValue(json, MyClass.class);

        assertEquals("value", deserialized.field);
        assertEquals(expected, deserialized.anySetter);
    }
}
