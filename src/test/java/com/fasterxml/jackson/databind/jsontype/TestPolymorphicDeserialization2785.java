package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestPolymorphicDeserialization2785 extends BaseMapTest {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "packetType")
    public interface A {
    }
    @JsonTypeName("myType")
    static class B implements A {
    }

    public void testDeserialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().copy();
        objectMapper.getSubtypeResolver().registerSubtypes(B.class);
        String json = "{ \"packetType\": \"myType\" }";
        objectMapper.readValue(json, A.class);
    }
}
