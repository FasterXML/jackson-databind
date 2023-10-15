package com.fasterxml.jackson.databind.jsontype.deftyping;

import static com.fasterxml.jackson.databind.BaseMapTest.jsonMapperBuilder;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import org.junit.Test;

/**
 * [databind#3569]: Unable to deserialize enum object with default-typed
 * {@link com.fasterxml.jackson.annotation.JsonTypeInfo.As#WRAPPER_ARRAY} and {@link JsonCreator} together.
 */
public class AsWrapperArrayEnumDeser3569Test
{
    static class Foo<T> {
        public T item;
    }

    enum Bar {
        ENABLED,
        DISABLED,
        HIDDEN;

        @JsonCreator
        public static Bar fromValue(String value) {
            String upperVal = value.toUpperCase();
            for (Bar enumValue : Bar.values()) {
                if (enumValue.name().equals(upperVal)) {
                    return enumValue;
                }
            }
            throw new IllegalArgumentException("Bad input [" + value + "]");
        }
    }

    @Test
    public void testEnumAsWrapperArrayWithCreator() throws JsonProcessingException {
        ObjectMapper objectMapper = jsonMapperBuilder()
                .activateDefaultTyping(
                        new DefaultBaseTypeLimitingValidator(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.WRAPPER_ARRAY)
                .build();

        Foo<Bar> expected = new Foo<>();
        expected.item = Bar.ENABLED;

        // First, serialize
        String serialized = objectMapper.writeValueAsString(expected);

        // Then, deserialize with TypeReference
        assertNotNull(objectMapper.readValue(serialized, new TypeReference<Foo<Bar>>() {}));
        // And, also try as described in [databind#3569]
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(Foo.class, new Class[]{Bar.class});
        assertNotNull(objectMapper.readValue(serialized, javaType));
    }
}
