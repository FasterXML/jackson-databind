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

/**
 * Test for Map values with {@code @JsonTypeInfo(use = Id.NONE)} override,
 * extending issue #1654 coverage to Map types.
 */
class NoTypeInfo1654MapTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    static class Value1654 {
        public int x;

        protected Value1654() { }

        public Value1654(int x) {
            this.x = x;
        }
    }

    static class Value1654TypedMapContainer {
        public Map<String, Value1654> values;

        protected Value1654TypedMapContainer() { }

        public Value1654TypedMapContainer(Map<String, Value1654> v) {
            values = v;
        }
    }

    static class Value1654UntypedMapContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Map<String, Value1654> values;

        protected Value1654UntypedMapContainer() { }

        public Value1654UntypedMapContainer(Map<String, Value1654> v) {
            values = v;
        }
    }

    static class Value1654UsingCustomSerDeserUntypedMapContainer {
        @JsonDeserialize(contentUsing = Value1654Deserializer.class)
        @JsonSerialize(contentUsing = Value1654Serializer.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Map<String, Value1654> values;

        protected Value1654UsingCustomSerDeserUntypedMapContainer() { }

        public Value1654UsingCustomSerDeserUntypedMapContainer(Map<String, Value1654> v) {
            values = v;
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

    // [databind#1654]: no override, default polymorphic type id for Map values
    @Test
    void withoutNoTypeElementOverrideSerAndDeser() throws Exception {
        // regular typed case
        Map<String, Value1654> map = new LinkedHashMap<>();
        map.put("first", new Value1654(1));
        map.put("second", new Value1654(2));

        String json = MAPPER.writeValueAsString(new Value1654TypedMapContainer(map));
        String typeId = Value1654.class.getName();
        typeId = "'@type':'" + typeId.substring(typeId.lastIndexOf('.') + 1) + "'";
        assertEquals(a2q("{'values':{'first':{"+typeId+",'x':1},'second':{"+typeId+",'x':2}}}"), json);

        Value1654TypedMapContainer result = MAPPER.readValue(json, Value1654TypedMapContainer.class);
        assertEquals(2, result.values.size());
        assertEquals(2, result.values.get("second").x);
    }

    // [databind#1654]: override, no polymorphic type id for Map values, serialization
    @Test
    void withNoTypeInfoDefaultSer() throws Exception {
        Map<String, Value1654> map = new LinkedHashMap<>();
        map.put("first", new Value1654(3));
        map.put("second", new Value1654(7));

        Value1654UntypedMapContainer cont = new Value1654UntypedMapContainer(map);
        assertEquals(a2q("{'values':{'first':{'x':3},'second':{'x':7}}}"),
                MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]: override, no polymorphic type id for Map values, deserialization
    @Test
    void withNoTypeInfoDefaultDeser() throws Exception {
        final String noTypeJson = a2q(
                "{'values':{'first':{'x':3},'second':{'x':7}}}"
        );
        Value1654UntypedMapContainer unResult = MAPPER.readValue(noTypeJson,
                Value1654UntypedMapContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(7, unResult.values.get("second").x);
    }

    // [databind#1654]: override, no polymorphic type id for Map values, custom serialization
    @Test
    void withNoTypeInfoOverrideSer() throws Exception {
        Map<String, Value1654> map = new LinkedHashMap<>();
        map.put("first", new Value1654(1));
        map.put("second", new Value1654(2));

        Value1654UsingCustomSerDeserUntypedMapContainer cont =
                new Value1654UsingCustomSerDeserUntypedMapContainer(map);
        assertEquals(a2q("{'values':{'first':{'v':1},'second':{'v':2}}}"),
                MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]: override, no polymorphic type id for Map values, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeser() throws Exception {
        final String noTypeJson = a2q(
                "{'values':{'first':{'v':3},'second':{'v':7}}}"
        );
        Value1654UsingCustomSerDeserUntypedMapContainer unResult = MAPPER.readValue(noTypeJson,
                Value1654UsingCustomSerDeserUntypedMapContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(3, unResult.values.get("first").x);
        assertEquals(7, unResult.values.get("second").x);
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
