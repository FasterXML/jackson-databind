package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/** Test(s) for [Issue#2610]. Polymorphic deserialization with external type property and ignored field. */
public class TestPolymorphicWithIgnoredField extends BaseMapTest {
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Wrapper {
        private String type;

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type",
                defaultImpl = Default.class
        )
        private Default data;

        @JsonCreator
        public Wrapper(
                @JsonProperty(value = "type", required = true) String type,
                @JsonProperty(value = "data", required = true) Default data
        ) {
            this.type = type;
            this.data = data;
        }

        String getType() {
            return type;
        }

        Default getData() {
            return data;
        }
    }

    static class Default {}

    public void testDeserialization() throws IOException {
        String data = "[{\"type\": \"test\",\"data\": {},\"additional\": {}}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Wrapper> result = mapper.readValue(data, new TypeReference<List<Wrapper>>() {});

        assertEquals(1, result.size());

        Wrapper item = result.get(0);
        assertEquals("test", item.getType());
        assertNotNull(item.getData());
    }
}
