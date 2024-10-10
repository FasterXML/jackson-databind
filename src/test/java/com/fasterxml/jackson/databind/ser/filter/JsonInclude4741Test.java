package com.fasterxml.jackson.databind.ser.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonInclude4741Test
        extends DatabindTestUtil
{

    private ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testSerialization() throws JsonProcessingException
    {
        MyString input = new MyString();
        input.setValue("");

        String json = MAPPER.writeValueAsString(input);
        MyString output = MAPPER.readValue(json, MyString.class);

        assertEquals(input.getValue(), output.getValue());
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class MyString {
        private String value = null;

        // Getter
        public String getValue() {
            return value;
        }

        // Setter
        public void setValue(String value) {
            this.value = value;
        }
    }
}
