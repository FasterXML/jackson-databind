package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

public class ExternalTypeId999Test extends BaseMapTest
{
    public static interface Payload { }

    @JsonTypeName("foo")
    public static class FooPayload implements Payload { }

    @JsonTypeName("bar")
    public static class BarPayload implements Payload { }

    public static class Message<P extends Payload>
    {
        final String type;

        @JsonTypeInfo(visible = true, use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(FooPayload.class),
                @JsonSubTypes.Type(BarPayload.class) })
        private final P payload;

        @JsonCreator
        public Message(@JsonProperty("type") String type,
                @JsonProperty("payload") P payload)
        {
            if (payload == null) {
                throw new RuntimeException("'payload' is null");
            }
            if (type == null) {
                throw new RuntimeException("'type' is null");
            }
            this.type = type;
            this.payload = payload;
        }
    }


    public void testExternalTypeId() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        Message<?> msg = objectMapper.readValue(
                "{ \"type\": \"foo\", \"payload\": {} }",
                new TypeReference<Message<FooPayload>>() { });
        assertNotNull(msg);
    }
}
