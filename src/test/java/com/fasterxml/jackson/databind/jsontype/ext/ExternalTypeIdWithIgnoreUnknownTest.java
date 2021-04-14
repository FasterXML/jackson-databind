package com.fasterxml.jackson.databind.jsontype.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

public class ExternalTypeIdWithIgnoreUnknownTest extends BaseMapTest
{
    // [databind#2611]
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Wrapper2611 {
        private String type;

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type",
                defaultImpl = Default2611.class
        )
        private Default2611 data;

        @JsonCreator
        public Wrapper2611(
                @JsonProperty(value = "type", required = true) String type,
                @JsonProperty(value = "data", required = true) Default2611 data
        ) {
            this.type = type;
            this.data = data;
        }

        String getType() {
            return type;
        }

        Default2611 getData() {
            return data;
        }
    }

    static class Default2611 {}

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2611]
    public void testDeserialization() throws Exception
    {
        final String data = a2q("[{'type': 'test','data': {},'additional': {}}]");

        List<Wrapper2611> result = MAPPER.readValue(data, new TypeReference<List<Wrapper2611>>() {});

        assertEquals(1, result.size());

        Wrapper2611 item = result.get(0);
        assertEquals("test", item.getType());
        assertNotNull(item.getData());
    }
}
