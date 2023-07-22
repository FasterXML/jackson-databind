package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

// [databind#4047] : ObjectMapper.valueToTree will ignore the configuration SerializationFeature.WRAP_ROOT_VALUE
public class TestRootType4047Test extends BaseMapTest {

    @JsonRootName("event")
    static class Event {
        public Long id;
        public String name;
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    private final String WRAPPED_EVENT_JSON = "{\"event\":{\"id\":1,\"name\":\"foo\"}}";
    private final String UNWRAPPED_EVENT_JSON = "{\"id\":1,\"name\":\"foo\"}";

    public void testValueToTree() throws Exception {
        // Arrange
        final ObjectMapper WRAP_ROOT_MAPPER = jsonMapperBuilder()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .build();
        Event value = new Event();
        value.id = 1L;
        value.name = "foo";

        // (1) works
        assertEquals(WRAPPED_EVENT_JSON,
                WRAP_ROOT_MAPPER.writeValueAsString(value));

        // (2) fails w/ {"id":1,"name":"foo"}
        assertEquals(WRAPPED_EVENT_JSON,
                WRAP_ROOT_MAPPER.valueToTree(value).toString());
    }

    public void testTreeToValue() throws Exception {
        // Arrange
        final ObjectMapper UNWRAP_MAPPER = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .build();

        /*
         * (1) both fails with
         * com.fasterxml.jackson.databind.exc.MismatchedInputException: Root name ('event') does not match expected ('JsonNode') for type `com.fasterxml.jackson.databind.JsonNode`
         *  at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 2] (through reference chain: com.fasterxml.jackson.databind.JsonNode["event"])
         */
        UNWRAP_MAPPER.readTree(WRAPPED_EVENT_JSON);
        UNWRAP_MAPPER.readTree(UNWRAPPED_EVENT_JSON);
    }
}
