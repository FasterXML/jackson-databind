package com.fasterxml.jackson.databind.jsontype;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// For [databind#2978], [databind#4138]
public class TestDoubleJsonCreator extends DatabindTestUtil
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

    // [databind#4138]
    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(Value4138Impl.class)
    })
    abstract static class Value4138Base {
        public abstract Object[] getAllowedValues();
    }

    @JsonTypeName("type1")
    static class Value4138Impl extends Value4138Base {
        Object[] allowedValues;

        protected Value4138Impl() { }

        public Value4138Impl(Object... allowedValues) {
            this.allowedValues = allowedValues;
        }

        @Override
        public Object[] getAllowedValues() {
            return allowedValues;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2978]
    @Test
    public void testDeserializationTypeFieldLast() throws Exception {
        UnionExample expected = UnionExample.double_(AliasDouble.of(2.0D));
        UnionExample actual = MAPPER.readValue(
                a2q("{'double': 2.0,'type':'double'}"),
                new TypeReference<UnionExample>() {});
        assertEquals(expected, actual);
    }

    // [databind#2978]
    @Test
    public void testDeserializationTypeFieldFirst() throws Exception {
        UnionExample expected = UnionExample.double_(AliasDouble.of(2.0D));
        UnionExample actual = MAPPER.readValue(
                a2q("{'type':'double','double': 2.0}"),
                new TypeReference<UnionExample>() {});
        assertEquals(expected, actual);
    }

    // [databind#4138]
    @Test
    public void testDeserializeFPAsObject() throws Exception
    {
        final String JSON = "{\"allowedValues\": [ 1.5, 2.5 ], \"type\": \"type1\"}";
        // By default, should get Doubles
        Value4138Base value = MAPPER.readValue(JSON, Value4138Base.class);
        assertEquals(2, value.getAllowedValues().length);
        assertEquals(Double.class, value.getAllowedValues()[0].getClass());
        assertEquals(Double.class, value.getAllowedValues()[1].getClass());

        // but can be changed
        value = MAPPER.readerFor(Value4138Base.class)
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readValue(JSON);
        assertEquals(2, value.getAllowedValues().length);
        assertEquals(BigDecimal.class, value.getAllowedValues()[0].getClass());
        assertEquals(BigDecimal.class, value.getAllowedValues()[1].getClass());
    }
}
