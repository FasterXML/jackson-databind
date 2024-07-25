package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoTypeInfo1654Test extends DatabindTestUtil {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    static class Value1654 {
        public int x;

        protected Value1654() {
        }

        public Value1654(int x) {
            this.x = x;
        }
    }

    static class Value1654UntypedContainer {
        @JsonDeserialize(contentUsing = Value1654Deserializer.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public List<Value1654> values;

        protected Value1654UntypedContainer() {
        }

        public Value1654UntypedContainer(Value1654... v) {
            values = Arrays.asList(v);
        }
    }

    static class Value1654Deserializer extends JsonDeserializer<Value1654> {
        @Override
        public Value1654 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.skipChildren();
            return new Value1654(13);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1654]
    @Test
    void noTypeInfoOverrideSer() throws Exception {
        Value1654UntypedContainer cont = new Value1654UntypedContainer(
                new Value1654(3),
                new Value1654(7)
        );
        assertEquals(a2q("{'values':[{'x':3},{'x': 7}] }"),
                MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]
    @Test
    void noTypeInfoOverrideDeser() throws Exception {
        // and then actual failing case
        final String noTypeJson = a2q(
                "{'values':[{'x':3},{'x': 7}] }"
        );
        Value1654UntypedContainer unResult = MAPPER.readValue(noTypeJson, Value1654UntypedContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(7, unResult.values.get(1).x);
    }
}
