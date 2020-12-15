package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// For [databind#2978]
public class TestDoubleJsonCreator extends BaseMapTest
{
    static final class UnionExample {
        private final Base value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        private UnionExample(Base value) {
            this.value = value;
        }

        @JsonValue
        private Base getValue() {
            return value;
        }

        public static UnionExample double_(AliasDouble value) {
            return new UnionExample(new DoubleWrapper(value));
        }

        public <T> T accept(Visitor<T> visitor) {
            return value.accept(visitor);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof UnionExample && equalTo((UnionExample) other));
        }

        private boolean equalTo(UnionExample other) {
            return this.value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.value);
        }

        @Override
        public String toString() {
            return "UnionExample{value: " + value + '}';
        }

        public interface Visitor<T> {
            T visitDouble(AliasDouble value);

            T visitUnknown(String unknownType);
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UnknownWrapper.class)
        @JsonSubTypes(@JsonSubTypes.Type(UnionExample.DoubleWrapper.class))
        @JsonIgnoreProperties(ignoreUnknown = true)
        private interface Base {
            <T> T accept(Visitor<T> visitor);
        }

        @JsonTypeName("double")
        private static final class DoubleWrapper implements Base {
            private final AliasDouble value;

            @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
            DoubleWrapper(@JsonSetter("double") AliasDouble value) {
                Objects.requireNonNull(value, "double cannot be null");
                this.value = value;
            }

            @JsonProperty("double")
            private AliasDouble getValue() {
                return value;
            }

            @Override
            public <T> T accept(Visitor<T> visitor) {
                return visitor.visitDouble(value);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || (other instanceof DoubleWrapper && equalTo((DoubleWrapper) other));
            }

            private boolean equalTo(DoubleWrapper other) {
                return this.value.equals(other.value);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(this.value);
            }

            @Override
            public String toString() {
                return "DoubleWrapper{value: " + value + '}';
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
                Objects.requireNonNull(type, "type cannot be null");
                Objects.requireNonNull(value, "value cannot be null");
                this.type = type;
                this.value = value;
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

    static final class AliasDouble {
        private final double value;

        private AliasDouble(double value) {
            this.value = value;
        }

        @JsonValue
        public double get() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                    || (other instanceof AliasDouble
                    && Double.doubleToLongBits(this.value) == Double.doubleToLongBits(((AliasDouble) other).value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static AliasDouble of(double value) {
            return new AliasDouble(value);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2978]
    public void testDeserializationTypeFieldLast() throws IOException {
        UnionExample expected = UnionExample.double_(AliasDouble.of(2.0D));
        UnionExample actual = MAPPER.readValue(
                a2q("{'double': 2.0,'type':'double'}"),
                new TypeReference<UnionExample>() {});
        assertEquals(expected, actual);
    }

    // [databind#2978]
    public void testDeserializationTypeFieldFirst() throws IOException {
        UnionExample expected = UnionExample.double_(AliasDouble.of(2.0D));
        UnionExample actual = MAPPER.readValue(
                a2q("{'type':'double','double': 2.0}"),
                new TypeReference<UnionExample>() {});
        assertEquals(expected, actual);
    }
}
