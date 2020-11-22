package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// For [databind#2953], regression from 2.11 to 2.12.0-rc2
public class ExistingPropertyWithAnyGetter2953Test extends BaseMapTest
{
    public static final class SingleUnion {
        private final Base value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        private SingleUnion(Base value) {
            this.value = value;
        }

        @JsonValue
        private Base getValue() {
            return value;
        }

        public static SingleUnion foo(String value) {
            return new SingleUnion(new FooWrapper(value));
        }

        public <T> T accept(Visitor<T> visitor) {
            return value.accept(visitor);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof SingleUnion && equalTo((SingleUnion) other));
        }

        private boolean equalTo(SingleUnion other) {
            return this.value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.value);
        }

        @Override
        public String toString() {
            return "SingleUnion{value: " + value + '}';
        }

        public interface Visitor<T> {
            T visitFoo(String value);

            T visitUnknown(String unknownType);
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UnknownWrapper.class)
        @JsonSubTypes(@JsonSubTypes.Type(FooWrapper.class))
        @JsonIgnoreProperties(ignoreUnknown = true)
        private interface Base {
            <T> T accept(Visitor<T> visitor);
        }

        @JsonTypeName("foo")
        private static final class FooWrapper implements Base {
            private final String value;

            @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
            FooWrapper(@JsonSetter("foo") String value) {
                Objects.requireNonNull(value, "foo cannot be null");
                this.value = value;
            }

            @JsonProperty("foo")
            private String getValue() {
                return value;
            }

            @Override
            public <T> T accept(Visitor<T> visitor) {
                return visitor.visitFoo(value);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || (other instanceof FooWrapper && equalTo((FooWrapper) other));
            }

            private boolean equalTo(FooWrapper other) {
                return this.value.equals(other.value);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(this.value);
            }

            @Override
            public String toString() {
                return "FooWrapper{value: " + value + '}';
            }
        }

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXISTING_PROPERTY,
                property = "type",
                visible = true)
        private static final class UnknownWrapper implements Base {
            private final String type;

            private final Map<String, Object> value;

            @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
            private UnknownWrapper(@JsonProperty("type") String type) {
                this(type, new HashMap<String, Object>());
            }

            private UnknownWrapper(String type, Map<String, Object> value) {
                this.type = Objects.requireNonNull(type, "type cannot be null");
                this.value = Objects.requireNonNull(value, "value cannot be null");
            }

            @JsonProperty
            private String getType() {
                return type;
            }

            @JsonAnyGetter
            private Map<String, Object> getValue() {
                return value;
            }

            @JsonAnySetter
            private void put(String key, Object val) {
                value.put(key, val);
            }

            @Override
            public <T> T accept(Visitor<T> visitor) {
                return visitor.visitUnknown(type);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || (other instanceof UnknownWrapper && equalTo((UnknownWrapper) other));
            }

            private boolean equalTo(UnknownWrapper other) {
                return this.type.equals(other.type) && this.value.equals(other.value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(this.type, this.value);
            }

            @Override
            public String toString() {
                return "UnknownWrapper{type: " + type + ", value: " + value + '}';
            }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    public void testUnionSerialization_string() throws IOException {
        assertEquals(a2q("{'type':'foo','foo':'string value'}"),
                MAPPER.writeValueAsString(SingleUnion.foo("string value")));
    }

    public void testUnionDeserialization_string() throws IOException {
        assertEquals(SingleUnion.foo("string value"),
                MAPPER.readValue(a2q("{'type':'foo','foo':'string value'}"), SingleUnion.class));
    }

    public void testDefaultUnknownVariant() throws IOException {
        String originalJson = a2q("{'type':'notknown','notknown':'ignored value'}");
        SingleUnion deserialized = MAPPER.readValue(
                originalJson,
                SingleUnion.class);
        String type = deserialized.accept(new SingleUnion.Visitor<String>() {

            @Override
            public String visitFoo(String value) {
                throw new AssertionError("Unexpected foo");
            }

            @Override
            public String visitUnknown(String unknownType) {
                return unknownType;
            }
        });
        assertEquals("notknown", type);
        String serialized = MAPPER.writeValueAsString(deserialized);
        assertEquals(originalJson, serialized);
    }
}
