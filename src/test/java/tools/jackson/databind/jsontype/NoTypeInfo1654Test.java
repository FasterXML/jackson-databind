package tools.jackson.databind.jsontype;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for [databind#1654]: {@code @JsonTypeInfo(use = Id.NONE)} override
 * across Collection, Map and Reference (Optional, AtomicReference) types.
 */
class NoTypeInfo1654Test extends DatabindTestUtil
{
    // Shared value type and custom ser/deser

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    static class Value1654 {
        public int x;

        protected Value1654() { }

        public Value1654(int x) {
            this.x = x;
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

    // Collection-specific types

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

    // Map-specific types

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

    // Reference-specific types (Optional, AtomicReference)

    static class Value1654TypedOptionalContainer {
        public Optional<Value1654> value;

        protected Value1654TypedOptionalContainer() { }

        public Value1654TypedOptionalContainer(Value1654 v) {
            value = Optional.ofNullable(v);
        }
    }

    static class Value1654UntypedOptionalContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Optional<Value1654> value;

        protected Value1654UntypedOptionalContainer() { }

        public Value1654UntypedOptionalContainer(Value1654 v) {
            value = Optional.ofNullable(v);
        }
    }

    static class Value1654UsingCustomSerDeserUntypedOptionalContainer {
        @JsonDeserialize(contentUsing = Value1654Deserializer.class)
        @JsonSerialize(contentUsing = Value1654Serializer.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Optional<Value1654> value;

        protected Value1654UsingCustomSerDeserUntypedOptionalContainer() { }

        public Value1654UsingCustomSerDeserUntypedOptionalContainer(Value1654 v) {
            value = Optional.ofNullable(v);
        }
    }

    static class Value1654TypedAtomicRefContainer {
        public AtomicReference<Value1654> value;

        protected Value1654TypedAtomicRefContainer() { }

        public Value1654TypedAtomicRefContainer(Value1654 v) {
            value = new AtomicReference<>(v);
        }
    }

    static class Value1654UsingCustomSerDeserUntypedAtomicRefContainer {
        @JsonDeserialize(contentUsing = Value1654Deserializer.class)
        @JsonSerialize(contentUsing = Value1654Serializer.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public AtomicReference<Value1654> value;

        protected Value1654UsingCustomSerDeserUntypedAtomicRefContainer() { }

        public Value1654UsingCustomSerDeserUntypedAtomicRefContainer(Value1654 v) {
            value = new AtomicReference<>(v);
        }
    }

    /*
    /**********************************************************************
    /* Test methods, Collection
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // no override, default polymorphic type id
    @Test
    void withoutNoTypeElementOverrideSerAndDeserCollection() throws Exception {
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

    // override, no polymorphic type id, serialization
    @Test
    void withNoTypeInfoDefaultSerCollection() throws Exception {
        Value1654UntypedContainer cont = new Value1654UntypedContainer(
                new Value1654(3),
                new Value1654(7)
        );
        assertEquals(a2q("{'values':[{'x':3},{'x':7}]}"),
                MAPPER.writeValueAsString(cont));
    }

    // override, no polymorphic type id, deserialization
    @Test
    void withNoTypeInfoDefaultDeserCollection() throws Exception {
        final String noTypeJson = a2q(
                "{'values':[{'x':3},{'x':7}]}"
        );
        Value1654UntypedContainer unResult = MAPPER.readValue(noTypeJson,
                Value1654UntypedContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(7, unResult.values.get(1).x);
    }

    // override, no polymorphic type id, custom serialization
    @Test
    void withNoTypeInfoOverrideSerCollection() throws Exception {
        Value1654UsingCustomSerDeserUntypedContainer cont = new Value1654UsingCustomSerDeserUntypedContainer(
                new Value1654(1),
                new Value1654(2)
        );
        assertEquals(a2q("{'values':[{'v':1},{'v':2}]}"),
                MAPPER.writeValueAsString(cont));
    }

    // override, no polymorphic type id, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeserCollection() throws Exception {
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

    /*
    /**********************************************************************
    /* Test methods, Map
    /**********************************************************************
     */

    // no override, default polymorphic type id for Map values
    @Test
    void withoutNoTypeElementOverrideSerAndDeserMap() throws Exception {
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

    // override, no polymorphic type id for Map values, serialization
    @Test
    void withNoTypeInfoDefaultSerMap() throws Exception {
        Map<String, Value1654> map = new LinkedHashMap<>();
        map.put("first", new Value1654(3));
        map.put("second", new Value1654(7));

        Value1654UntypedMapContainer cont = new Value1654UntypedMapContainer(map);
        assertEquals(a2q("{'values':{'first':{'x':3},'second':{'x':7}}}"),
                MAPPER.writeValueAsString(cont));
    }

    // override, no polymorphic type id for Map values, deserialization
    @Test
    void withNoTypeInfoDefaultDeserMap() throws Exception {
        final String noTypeJson = a2q(
                "{'values':{'first':{'x':3},'second':{'x':7}}}"
        );
        Value1654UntypedMapContainer unResult = MAPPER.readValue(noTypeJson,
                Value1654UntypedMapContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(7, unResult.values.get("second").x);
    }

    // override, no polymorphic type id for Map values, custom serialization
    @Test
    void withNoTypeInfoOverrideSerMap() throws Exception {
        Map<String, Value1654> map = new LinkedHashMap<>();
        map.put("first", new Value1654(1));
        map.put("second", new Value1654(2));

        Value1654UsingCustomSerDeserUntypedMapContainer cont =
                new Value1654UsingCustomSerDeserUntypedMapContainer(map);
        assertEquals(a2q("{'values':{'first':{'v':1},'second':{'v':2}}}"),
                MAPPER.writeValueAsString(cont));
    }

    // override, no polymorphic type id for Map values, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeserMap() throws Exception {
        final String noTypeJson = a2q(
                "{'values':{'first':{'v':3},'second':{'v':7}}}"
        );
        Value1654UsingCustomSerDeserUntypedMapContainer unResult = MAPPER.readValue(noTypeJson,
                Value1654UsingCustomSerDeserUntypedMapContainer.class);
        assertEquals(2, unResult.values.size());
        assertEquals(3, unResult.values.get("first").x);
        assertEquals(7, unResult.values.get("second").x);
    }

    // override, no polymorphic type id, custom serialization (single Map value)
    @Test
    void singleWithNoTypeInfoOverrideSerMap() throws Exception {
        SingleValue1654UsingCustomSerDeserUntyped wrapper = new SingleValue1654UsingCustomSerDeserUntyped(
                new Value1654(42));
        assertEquals(a2q("{'value':{'v':42}}"),
                MAPPER.writeValueAsString(wrapper));
    }

    // override, no polymorphic type id, custom deserialization (single Map value)
    @Test
    void singleWithNoTypeInfoOverrideDeserMap() throws Exception {
        String noTypeJson = a2q("{'value':{'v':42}}");
        SingleValue1654UsingCustomSerDeserUntyped result = MAPPER.readValue(noTypeJson,
                SingleValue1654UsingCustomSerDeserUntyped.class);
        assertEquals(42,result.value.x);
    }

    /*
    /**********************************************************************
    /* Test methods, Reference types (Optional, AtomicReference)
    /**********************************************************************
     */

    // no override, default polymorphic type id for Optional
    @Test
    void withoutNoTypeElementOverrideSerAndDeserOptional() throws Exception {
        Value1654TypedOptionalContainer cont = new Value1654TypedOptionalContainer(new Value1654(42));
        String json = MAPPER.writeValueAsString(cont);
        String typeId = Value1654.class.getName();
        typeId = "'@type':'" + typeId.substring(typeId.lastIndexOf('.') + 1) + "'";
        assertEquals(a2q("{'value':{"+typeId+",'x':42}}"), json);

        Value1654TypedOptionalContainer result = MAPPER.readValue(json, Value1654TypedOptionalContainer.class);
        assertTrue(result.value.isPresent());
        assertEquals(42, result.value.get().x);
    }

    // override, no polymorphic type id for Optional, serialization
    @Test
    void withNoTypeInfoDefaultSerOptional() throws Exception {
        Value1654UntypedOptionalContainer cont = new Value1654UntypedOptionalContainer(new Value1654(42));
        assertEquals(a2q("{'value':{'x':42}}"), MAPPER.writeValueAsString(cont));
    }

    // override, no polymorphic type id for Optional, deserialization
    @Test
    void withNoTypeInfoDefaultDeserOptional() throws Exception {
        String noTypeJson = a2q("{'value':{'x':42}}");
        Value1654UntypedOptionalContainer result = MAPPER.readValue(noTypeJson,
                Value1654UntypedOptionalContainer.class);
        assertTrue(result.value.isPresent());
        assertEquals(42, result.value.get().x);
    }

    // override, no polymorphic type id for Optional, custom serialization
    @Test
    void withNoTypeInfoOverrideSerOptional() throws Exception {
        Value1654UsingCustomSerDeserUntypedOptionalContainer cont =
                new Value1654UsingCustomSerDeserUntypedOptionalContainer(new Value1654(42));
        assertEquals(a2q("{'value':{'v':42}}"), MAPPER.writeValueAsString(cont));
    }

    // override, no polymorphic type id for Optional, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeserOptional() throws Exception {
        String noTypeJson = a2q("{'value':{'v':42}}");
        Value1654UsingCustomSerDeserUntypedOptionalContainer result = MAPPER.readValue(noTypeJson,
                Value1654UsingCustomSerDeserUntypedOptionalContainer.class);
        assertTrue(result.value.isPresent());
        assertEquals(42, result.value.get().x);
    }

    // no override, default polymorphic type id for AtomicReference
    @Test
    void withoutNoTypeElementOverrideSerAndDeserAtomicRef() throws Exception {
        Value1654TypedAtomicRefContainer cont = new Value1654TypedAtomicRefContainer(new Value1654(42));
        String json = MAPPER.writeValueAsString(cont);
        String typeId = Value1654.class.getName();
        typeId = "'@type':'" + typeId.substring(typeId.lastIndexOf('.') + 1) + "'";
        assertEquals(a2q("{'value':{"+typeId+",'x':42}}"), json);

        Value1654TypedAtomicRefContainer result = MAPPER.readValue(json, Value1654TypedAtomicRefContainer.class);
        assertEquals(42, result.value.get().x);
    }

    // override, no polymorphic type id for AtomicReference, custom serialization
    @Test
    void withNoTypeInfoOverrideSerAtomicRef() throws Exception {
        Value1654UsingCustomSerDeserUntypedAtomicRefContainer cont =
                new Value1654UsingCustomSerDeserUntypedAtomicRefContainer(new Value1654(42));
        assertEquals(a2q("{'value':{'v':42}}"), MAPPER.writeValueAsString(cont));
    }

    // override, no polymorphic type id for AtomicReference, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeserAtomicRef() throws Exception {
        String noTypeJson = a2q("{'value':{'v':42}}");
        Value1654UsingCustomSerDeserUntypedAtomicRefContainer result = MAPPER.readValue(noTypeJson,
                Value1654UsingCustomSerDeserUntypedAtomicRefContainer.class);
        assertEquals(42, result.value.get().x);
    }
}
