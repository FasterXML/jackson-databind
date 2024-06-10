package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class AbsentNodeViaCreator3214Test extends DatabindTestUtil
{
    static class Pojo3214
    {
        JsonNode fromCtor = TextNode.valueOf("x");
        JsonNode fromSetter = TextNode.valueOf("x");

        @JsonCreator
        public Pojo3214(@JsonProperty("node") JsonNode n) {
            this.fromCtor = n;
        }

        public void setNodeFromSetter(JsonNode nodeFromSetter) {
            this.fromSetter = nodeFromSetter;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3214]
    @Test
    public void testNullFromMissingNodeParameter() throws Exception
    {
        Pojo3214 p = MAPPER.readValue("{}", Pojo3214.class);
        if (p.fromCtor != null) {
            fail("Expected null to be passed, got instance of "+p.fromCtor.getClass());
        }
    }
}
