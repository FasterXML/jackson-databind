package com.fasterxml.jackson.databind.deser.enums;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4849] Not able to deserialize Enum with default typing after upgrading 2.15.4 -> 2.17.1
public class EnumSetDeserializationWithDefaultTyping4849Test
    extends DatabindTestUtil
{
    enum TestEnum4849 {
        TEST_ENUM_VALUE
    }

    private final ObjectMapper MAPPER = configureMapper4849();

    private ObjectMapper configureMapper4849()
    {
        final PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.fasterxml.jackson")
                .allowIfSubType("java")
                .build();

        @SuppressWarnings("serial")
        ObjectMapper.DefaultTypeResolverBuilder resolverBuilder
                = new ObjectMapper.DefaultTypeResolverBuilder(NON_FINAL, validator) {
            @Override
            public boolean useForType(JavaType t) {
                return true;
            }
        };

        StdTypeResolverBuilder stdTypeResolverBuilder = resolverBuilder
                .init(JsonTypeInfo.Id.CLASS, null)
                .inclusion(PROPERTY);

        return jsonMapperBuilder()
                .setDefaultTyping(stdTypeResolverBuilder)
                .build();
    }

    @Test
    public void testSerializationDeserializationRoundTrip4849()
            throws Exception
    {
        // Given
        EnumSet<TestEnum4849> input = EnumSet.of(TestEnum4849.TEST_ENUM_VALUE);
        // When : Serialize and deserialize
        String inputJson = MAPPER.writeValueAsString(input);
        Object inputDeserialized = MAPPER.readValue(inputJson, Object.class);
        // Then
        assertEquals(input, inputDeserialized);
    }

    @Test
    public void testHardCodedDeserializationFromPreviousJackson4849()
        throws Exception
    {
        // Given : Hard-coded output from Jackson 2.15.4
        String input = String.format("[\"java.util.EnumSet<%s>\",[\"%s\"]]",
                TestEnum4849.class.getName(),
                TestEnum4849.TEST_ENUM_VALUE.name());
        // When
        Object deserialized = MAPPER.readValue(input, Object.class);
        // Then
        assertEquals(EnumSet.of(TestEnum4849.TEST_ENUM_VALUE), deserialized);
    }
}
