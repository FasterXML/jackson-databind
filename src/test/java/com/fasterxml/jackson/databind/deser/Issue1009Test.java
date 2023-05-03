package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Issue1009Test extends BaseMapTest {

    public void testDeserialization() throws JsonProcessingException {
        String rawResponse = "{\"list\":[{\"type\":\"impl\",\"unmappedKey\":\"unusedValue\"}]}";
        MyResponse myResponse = objectMapper().readValue(rawResponse, MyResponse.class);
        assertNotNull(myResponse);
        assertEquals(1, myResponse.list.size());
        assertEquals("impl", myResponse.list.get(0).getType());
        assertNull(myResponse.list.get(0).getMissingInJson());
    }

    protected ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return om;
    }

    static class MyResponse {
        private List<Base> list;

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
            return switch (type) {
                case "impl" -> new Impl(type, missingInJson);
                default -> null;
            };
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
}
