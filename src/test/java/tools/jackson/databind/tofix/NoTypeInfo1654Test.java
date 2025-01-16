package tools.jackson.databind.tofix;

import java.util.*;

import org.junit.jupiter.api.Test;
import tools.jackson.core.testutil.failure.JacksonTestFailureExpected;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

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

    static class Value1654TypedContainer {
        public List<Value1654> values;

        protected Value1654TypedContainer() {
        }

        public Value1654TypedContainer(Value1654... v) {
            values = Arrays.asList(v);
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

    static class Value1654Deserializer extends ValueDeserializer<Value1654> {
        @Override
        public Value1654 deserialize(JsonParser p, DeserializationContext ctxt) {
            p.skipChildren();
            return new Value1654(13);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1654]
    @Test
    void noTypeElementOverride() throws Exception {
        // egular typed case
        String json = MAPPER.writeValueAsString(new Value1654TypedContainer(
                new Value1654(1),
                new Value1654(2),
                new Value1654(3)
        ));
        Value1654TypedContainer result = MAPPER.readValue(json, Value1654TypedContainer.class);
        assertEquals(3, result.values.size());
        assertEquals(2, result.values.get(1).x);
    }

    // [databind#1654]
    @JacksonTestFailureExpected
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
    @JacksonTestFailureExpected
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
