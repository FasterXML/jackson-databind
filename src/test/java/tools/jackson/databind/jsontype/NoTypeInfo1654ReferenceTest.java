package tools.jackson.databind.jsontype;

import java.util.Optional;
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
 * Test for Reference types (Optional, AtomicReference) with
 * {@code @JsonTypeInfo(use = Id.NONE)} override.
 */
class NoTypeInfo1654ReferenceTest extends DatabindTestUtil
{
    // [databind#1654]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    static class Value1654 {
        public int x;

        protected Value1654() { }

        public Value1654(int x) {
            this.x = x;
        }
    }

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

    // [databind#1654]: no override, default polymorphic type id for Optional
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

    // [databind#1654]: override, no polymorphic type id for Optional, serialization
    @Test
    void withNoTypeInfoDefaultSerOptional() throws Exception {
        Value1654UntypedOptionalContainer cont = new Value1654UntypedOptionalContainer(new Value1654(42));
        assertEquals(a2q("{'value':{'x':42}}"), MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]: override, no polymorphic type id for Optional, deserialization
    @Test
    void withNoTypeInfoDefaultDeserOptional() throws Exception {
        String noTypeJson = a2q("{'value':{'x':42}}");
        Value1654UntypedOptionalContainer result = MAPPER.readValue(noTypeJson,
                Value1654UntypedOptionalContainer.class);
        assertTrue(result.value.isPresent());
        assertEquals(42, result.value.get().x);
    }

    // [databind#1654]: override, no polymorphic type id for Optional, custom serialization
    @Test
    void withNoTypeInfoOverrideSerOptional() throws Exception {
        Value1654UsingCustomSerDeserUntypedOptionalContainer cont =
                new Value1654UsingCustomSerDeserUntypedOptionalContainer(new Value1654(42));
        assertEquals(a2q("{'value':{'v':42}}"), MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]: override, no polymorphic type id for Optional, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeserOptional() throws Exception {
        String noTypeJson = a2q("{'value':{'v':42}}");
        Value1654UsingCustomSerDeserUntypedOptionalContainer result = MAPPER.readValue(noTypeJson,
                Value1654UsingCustomSerDeserUntypedOptionalContainer.class);
        assertTrue(result.value.isPresent());
        assertEquals(42, result.value.get().x);
    }

    // [databind#1654]: no override, default polymorphic type id for AtomicReference
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

    // [databind#1654]: override, no polymorphic type id for AtomicReference, custom serialization
    @Test
    void withNoTypeInfoOverrideSerAtomicRef() throws Exception {
        Value1654UsingCustomSerDeserUntypedAtomicRefContainer cont =
                new Value1654UsingCustomSerDeserUntypedAtomicRefContainer(new Value1654(42));
        assertEquals(a2q("{'value':{'v':42}}"), MAPPER.writeValueAsString(cont));
    }

    // [databind#1654]: override, no polymorphic type id for AtomicReference, custom deserialization
    @Test
    void withNoTypeInfoOverrideDeserAtomicRef() throws Exception {
        String noTypeJson = a2q("{'value':{'v':42}}");
        Value1654UsingCustomSerDeserUntypedAtomicRefContainer result = MAPPER.readValue(noTypeJson,
                Value1654UsingCustomSerDeserUntypedAtomicRefContainer.class);
        assertEquals(42, result.value.get().x);
    }
}
