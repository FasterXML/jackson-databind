package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class UnknownPropertiesTest extends BaseMapTest
{

    static class ExtractFieldsNoDefaultConstructor {
        private final String s;
        private final int i;

        @JsonCreator
        public ExtractFieldsNoDefaultConstructor(@JsonProperty("s") String s, @JsonProperty("i") int i) {
            this.s = s;
            this.i = i;
        }

        public String getS() {
            return s;
        }

        public int getI() {
            return i;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testIgnoreProperty() throws Exception
    {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        ObjectMapper objectMapper = JsonMapper.builder(jsonFactory)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 1000000; i++) {
            stringBuilder.append(7);
        }
        final String test1000000 = stringBuilder.toString();
        ExtractFieldsNoDefaultConstructor ef =
                objectMapper.readValue(genJson(test1000000), ExtractFieldsNoDefaultConstructor.class);
    }

    private String genJson(String num) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("{\"s\":\"s\",\"n\":")
                .append(num)
                .append(",\"i\":1}");
        return stringBuilder.toString();
    }
}
