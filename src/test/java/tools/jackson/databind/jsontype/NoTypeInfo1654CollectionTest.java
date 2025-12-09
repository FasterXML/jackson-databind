package tools.jackson.databind.jsontype;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoTypeInfo1654CollectionTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    static class Value1654 {
        public int x;

        protected Value1654() { }

        public Value1654(int x) {
            this.x = x;
        }
    }

    static class Value1654TypedContainer {
        public List<Value1654> values;

        protected Value1654TypedContainer() { }

        public Value1654TypedContainer(Value1654... v) {
            values = Arrays.asList(v);
        }
    }

    static class Value1654UntypedContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public List<Value1654> values;

        protected Value1654UntypedContainer() { }

        public Value1654UntypedContainer(Value1654... v) {
            values = Arrays.asList(v);
        }
    }

    static class Value1654UsingCustomSerDeserUntypedContainer {
        @JsonDeserialize(contentUsing = Value1654Deserializer.class)
        @JsonSerialize(contentUsing = Value1654Serializer.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public List<Value1654> values;

        protected Value1654UsingCustomSerDeserUntypedContainer() { }

        public Value1654UsingCustomSerDeserUntypedContainer(Value1654... v) {
            values = Arrays.asList(v);
        }
    }

    static class SingleValue1654UsingCustomSerDeserUntyped {
        @JsonDeserialize(using = Value1654Deserializer.class)
        @JsonSerialize(using = Value1654Serializer.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Value1654 value;

        protected SingleValue1654UsingCustomSerDeserUntyped() { }

        public SingleValue1654UsingCustomSerDeserUntyped(Value1654 v) {
            value = v;
        }
    }

    static class Value1654Deserializer extends ValueDeserializer<Value1654> {
        @Override
        public Value1654 deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode n = ctxt.readTree(p);
            if (!n.has("v")) {
                ctxt.reportInputMismatch(Value1654.class, "Bad JSON input (no 'v'): " + n);
            }
            return new Value1654(n.path("v").intValue());
        }
    }


    static class Value1654Serializer extends ValueSerializer<Value1654> {
        @Override
        public void serialize(Value1654 value, JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writeStartObject(value);
            gen.writeNumberProperty("v", value.x);
            gen.writeEndObject();
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1654]: no override, default polymorphic type id
    @Test
    void withoutNoTypeElementOverrideSerAndDeser() throws Exception {
        // regular typed case
        String json = MAPPER.writeValueAsString(new Value1654TypedContainer(
                new Value1654(1),
                new Value1654(2)
        ));
        String typeId = Value1654.class.getName();
        typeId = "'@type':'" + typeId.substring(typeId.lastIndexOf('.') + 1) + "'";
        assertEquals(a2q("{'values':[{"+typeId+",'x':1},{"+typeId+",'x':2}]}"), json);

        Value1654TypedContainer result = MAPPER.readValue(json, Value1654TypedContainer.class);
        assertEquals(2, result.values.size());
        assertEquals(2, result.values.get(1).x);
    }

    // [databind#1654]: override, no polymorphic type id, serialization
    @Test
    void withNoTypeInfoDefaultSer() throws Exception {
        Value1654UntypedContainer cont = new Value1654UntypedContainer(
                new Value1654(3),
                new Value1654(7)
        );
        assertEquals(a2q("{'values':[{'x':3},{'x':7}]}"),
                MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]: override, no polymorphic type id, deserialization
    @Test
    void withNoTypeInfoDefaultDeser() throws Exception {
        final String noTypeJson = a2q(
                "{'values':[{'x':3},{'x':7}]}"
        );
        Value1654UntypedContainer unResult = MAPPER.readValue(noTypeJson,
                Value1654UntypedContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(7, unResult.values.get(1).x);
    }

    // [databind#1654]: override, no polymorphic type id, custom serialization
    @Test
    void withNoTypeInfoOverrideSer() throws Exception {
        Value1654UsingCustomSerDeserUntypedContainer cont = new Value1654UsingCustomSerDeserUntypedContainer(
                new Value1654(1),
                new Value1654(2)
        );
        assertEquals(a2q("{'values':[{'v':1},{'v':2}]}"),
                MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]: override, no polymorphic type id, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeser() throws Exception {
        final String noTypeJson = a2q(
                "{'values':[{'v':3},{'v':7}]}"
        );
        Value1654UsingCustomSerDeserUntypedContainer unResult = MAPPER.readValue(noTypeJson,
                Value1654UsingCustomSerDeserUntypedContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(3, unResult.values.get(0).x);
        assertEquals(7, unResult.values.get(1).x);
    }

    // // And then validation for individual value, not in Container

    // override, no polymorphic type id, custom serialization
    @Test
    void singleWithNoTypeInfoOverrideSer() throws Exception {
        SingleValue1654UsingCustomSerDeserUntyped wrapper = new SingleValue1654UsingCustomSerDeserUntyped(
                new Value1654(42));
        assertEquals(a2q("{'value':{'v':42}}"),
                MAPPER.writeValueAsString(wrapper));
    }

    // override, no polymorphic type id, custom deserialization
    @Test
    void singleWithNoTypeInfoOverrideDeser() throws Exception {
        String noTypeJson = a2q("{'value':{'v':42}}");
        SingleValue1654UsingCustomSerDeserUntyped result = MAPPER.readValue(noTypeJson,
                SingleValue1654UsingCustomSerDeserUntyped.class);
        assertEquals(42,result.value.x);
    }
}
