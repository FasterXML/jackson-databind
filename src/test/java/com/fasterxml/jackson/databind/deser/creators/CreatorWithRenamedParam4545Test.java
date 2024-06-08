package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.fail;

public class CreatorWithRenamedParam4545Test
    extends DatabindTestUtil
{
    static class Payload4545 {
        private final String key1;
        private final String key2;

        @JsonCreator
        public Payload4545(
                @ImplicitName("key1")
                @JsonProperty("key")
                String key1, // NOTE: the mismatch `key` / `key1` is important

                @ImplicitName("key2")
                @JsonProperty("key2")
                String key2
        ) {
            this.key1 = key1;
            this.key2 = key2;
        }

        public String getKey1() {
            return key1;
        }

        public String getKey2() {
            return key2;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
            .annotationIntrospector(new ImplicitNameIntrospector())
            .build();

    // [databind#4545]
    @Test
    public void testCreatorWithRename4545() throws Exception
    {
        String jsonPayload = a2q("{ 'key1': 'val1', 'key2': 'val2'}");

        try {
            MAPPER.readValue(jsonPayload, Payload4545.class);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized");
            verifyException(e, "key1");
        }
    }
}
