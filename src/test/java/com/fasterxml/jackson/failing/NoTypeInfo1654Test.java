package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class NoTypeInfo1654Test extends BaseMapTest
{
    // [databind#1654]

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    static class Value1654 {
        public int x;

        protected Value1654() { }
        public Value1654(int x) { this.x = x; }
    }

    static class Value1654TypedContainer {
        public List<Value1654> values;

        protected Value1654TypedContainer() { }
        public Value1654TypedContainer(Value1654... v) {
            values = Arrays.asList(v);
        }
    }

    static class Value1654UntypedContainer {
        @JsonDeserialize(contentUsing = Value1654Deserializer.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public List<Value1654> values;
    }

    static class Value1654Deserializer extends JsonDeserializer<Value1654> {
        @Override
        public Value1654 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.skipChildren();
            return new Value1654(13);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [databind#1654]
    public void testNoTypeElementOverride() throws Exception
    {
        final ObjectMapper mapper = newObjectMapper();

        // First: regular typed case
        String json = mapper.writeValueAsString(new Value1654TypedContainer(
                new Value1654(1),
                new Value1654(2),
                new Value1654(3)
        ));
        Value1654TypedContainer result = mapper.readValue(json, Value1654TypedContainer.class);
        assertEquals(3, result.values.size());
        assertEquals(2, result.values.get(1).x);

        // and then actual failing case
        final String noTypeJson = aposToQuotes(
                "{'values':[{'x':3},{'x': 7}] }"
                );
        Value1654UntypedContainer unResult = mapper.readValue(noTypeJson, Value1654UntypedContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(7, unResult.values.get(1).x);
    }
}
