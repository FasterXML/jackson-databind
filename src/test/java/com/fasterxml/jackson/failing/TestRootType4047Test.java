package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

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

    private final String EVENT_JSON = "{\"event\":{\"id\":1,\"name\":\"foo\"}}";

    public void testValueToTree() throws Exception 
    {
        // Arrange
        final ObjectMapper WRAP_ROOT_MAPPER = jsonMapperBuilder()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .build();
        Event value = new Event();
        value.id = 1L;
        value.name = "foo";

        // (1) works
        assertEquals(EVENT_JSON,
                WRAP_ROOT_MAPPER.writeValueAsString(value));

        // (2) fails w/ {"id":1,"name":"foo"}
        assertEquals(EVENT_JSON,
                WRAP_ROOT_MAPPER.valueToTree(value).toString());
    }


    public void testTreeToValue() throws Exception 
    {
        // Arrange
        final ObjectMapper UNWRAP_MAPPER = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .build();

        Event value = new Event();
        value.id = 1L;
        value.name = "foo";

        // Act & Assert

        // (1) works
        Event event1 = UNWRAP_MAPPER.readValue(EVENT_JSON, Event.class);
        assertEquals(value.id, event1.id);
        assertEquals(value.name, event1.name);

        // (2) fails (but shouldn't)
        try {
            UNWRAP_MAPPER.treeToValue(
                    UNWRAP_MAPPER.readTree(EVENT_JSON),
                    Event.class);
        } catch (MismatchedInputException e) {
            verifyException(e, "Root name ('event') does not match expected ('JsonNode')");
            throw e; // <-- should not throw
        }
    }
}
