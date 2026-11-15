package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#6421]: `EXTERNAL_PROPERTY` type ids cannot be used for an
// `@JsonUnwrapped` value and must fail with a clear definition error.
class ExternalTypeIdOnUnwrappedValue6421Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
    @JsonSubTypes({ @JsonSubTypes.Type(value = A.class, name = "a") })
    static abstract class Base { }

    static class A extends Base {
        public int x;
    }

    static class Outer {
        public String name;

        @JsonUnwrapped
        public Base value;
    }

    static abstract class PropertyBase { }

    static class PropertyA extends PropertyBase {
        public int x;
    }

    static class PropertyOuter {
        public String name;

        @JsonUnwrapped
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
        @JsonSubTypes({ @JsonSubTypes.Type(value = PropertyA.class, name = "a") })
        public PropertyBase value;
    }

    private static final String JSON = """
            {"name":"n","kind":"a","x":3}
            """;

    @Test
    void externalTypeIdOnUnwrappedValueLenient() {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        _verifyInvalidDefinition(mapper, Outer.class);
    }

    @Test
    void externalTypeIdOnUnwrappedValueStrict() {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        _verifyInvalidDefinition(mapper, Outer.class);
    }

    @Test
    void externalTypeIdDeclaredOnUnwrappedProperty() {
        _verifyInvalidDefinition(newJsonMapper(), PropertyOuter.class);
    }

    private void _verifyInvalidDefinition(ObjectMapper mapper, Class<?> targetType) {
        InvalidDefinitionException ex = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue(JSON, targetType));
        verifyException(ex, "@JsonUnwrapped");
        verifyException(ex, "EXTERNAL_PROPERTY");
    }
}
