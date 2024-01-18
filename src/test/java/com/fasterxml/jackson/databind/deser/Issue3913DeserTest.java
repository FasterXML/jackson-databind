package com.fasterxml.jackson.databind.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

public class Issue3913DeserTest
{
    // [databind#3913]
    static class MyResponse {
        List<Base> list;

        public List<Base> getList() {
            return list;
        }

        public void setList(List<Base> list) {
            this.list = list;
        }
    }

    interface Base {

        String getType();

        String getMissingInJson();

        @JsonCreator
        static Base unmarshall(
                @JsonProperty("missingInJson") String missingInJson,
                @JsonProperty("type") String type
        ) {
            switch (type) {
                case "impl":
                    return new Impl(type, missingInJson);
                default:
                    return null;
            }
        }
    }

    final static class Impl implements Base {
        private String type;
        private String missingInJson;

        public Impl() {
        }

        public Impl(String type, String missingInJson) {
            this.type = type;
            this.missingInJson = missingInJson;
        }

        @Override public String getType() {
            return type;
        }

        @Override public String getMissingInJson() {
            return missingInJson;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setMissingInJson(String missingInJson) {
            this.missingInJson = missingInJson;
        }
    }

    // [databind#3913]
    @Test
    public void testDeserialization() throws JsonProcessingException {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        String rawResponse = "{\"list\":[{\"type\":\"impl\",\"unmappedKey\":\"unusedValue\"}]}";
        MyResponse myResponse = mapper.readValue(rawResponse, MyResponse.class);
        assertNotNull(myResponse);
        assertEquals(1, myResponse.list.size());
        assertEquals("impl", myResponse.list.get(0).getType());
        assertNull(myResponse.list.get(0).getMissingInJson());
    }
}
