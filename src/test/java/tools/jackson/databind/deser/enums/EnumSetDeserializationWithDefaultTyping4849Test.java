package tools.jackson.databind.deser.enums;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

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

        DefaultTypeResolverBuilder resolverBuilder
                = new DefaultTypeResolverBuilder(validator, DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY) {
            @Override
            public boolean useForType(JavaType t) {
                return true;
            }
        };

        StdTypeResolverBuilder stdTypeResolverBuilder = resolverBuilder
                .init(JsonTypeInfo.Value.construct(JsonTypeInfo.Id.CLASS, JsonTypeInfo.As.PROPERTY,
                        "", Object.class, false, null),
                        null);

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
