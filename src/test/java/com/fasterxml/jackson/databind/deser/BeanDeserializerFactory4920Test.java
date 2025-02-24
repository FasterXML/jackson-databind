package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

/**
 * Unit test for [databind#4920]: Creator properties are ignored on abstract types when collecting bean properties,
 * breaking {@link com.fasterxml.jackson.databind.jsontype.impl.AsExternalTypeDeserializer}.
 */
public class BeanDeserializerFactory4920Test {

    @Test
    void testDeserializeAbstract() throws Exception {
        ObjectMapper objectMapper = jsonMapperBuilder().build();

        //language=JSON
        String json = "{ \"value\": \"1234567890\", \"type\": \"" + StringValue.class.getName() + "\" }";

        TypedData actual = objectMapper.readValue(json, TypedData.class);

        Assertions.assertNotNull(actual);
        Assertions.assertInstanceOf(StringValue.class, actual.getValue());
        Assertions.assertEquals("1234567890", ((StringValue) actual.getValue()).getValue());
    }

    private interface TypedData {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
        @JsonTypeIdResolver(ValueTypeIdResolver.class)
        Value getValue();

        String getType();

        @JsonCreator
        static TypedData immutableOf(@JsonProperty("value") Value value, @JsonProperty("type") String type) {
            return new TypedData.Immutable(value, type);
        }

        final class Immutable implements TypedData {

            private final Value value;
            private final String type;

            public Immutable(Value value, String type) {
                this.value = value;
                this.type = type;
            }

            @Override
            public Value getValue() {
                return value;
            }

            @Override
            public String getType() {
                return type;
            }
        }

        final class ValueTypeIdResolver extends TypeIdResolverBase {
            @Override
            public String idFromValue(Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String idFromValueAndType(Object value, Class<?> suggestedType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public JsonTypeInfo.Id getMechanism() {
                return JsonTypeInfo.Id.CUSTOM;
            }

            @Override
            public JavaType typeFromId(DatabindContext context, String id) throws IOException {
                Class<?> type;
                try {
                    type = Class.forName(id);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }

                return context.constructType(type);
            }
        }
    }

    private interface Value {
    }

    private static final class StringValue implements Value {

        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static final class LongValue implements Value {

        private final long value;

        public LongValue(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }
    }
}