package tools.jackson.databind.deser.inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MissingInjectableValueExcepion;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacksonInject3072Test extends DatabindTestUtil
{
    static class DtoWithOptional {
        @JacksonInject("id")
        String id;

        @JacksonInject(value = "optionalField", optional = OptBoolean.TRUE)
        String optionalField;

        public String getId() {
            return id;
        }

        public String getOptionalField() {
            return optionalField;
        }
    }

    static class DtoWithRequired {
        @JacksonInject(value = "requiredValue", optional = OptBoolean.FALSE)
        public String requiredField;
    }

    private final ObjectReader READER = newJsonMapper().readerFor(DtoWithOptional.class);

    @Test
    void testOptionalFieldFound() throws Exception {
        ObjectReader reader = READER
                .with(new InjectableValues.Std()
                        .addValue("id", "idValue")
                        .addValue("optionalField", "optionalFieldValue"));

        DtoWithOptional dto = reader.readValue("{}");

        assertEquals("idValue", dto.id);
        assertEquals("optionalFieldValue", dto.optionalField);
    }

    @Test
    void testOptionalFieldNotFound() throws Exception {
        ObjectReader reader = READER
                .with(new InjectableValues.Std()
                        .addValue("id", "idValue"));

        DtoWithOptional dto = reader.readValue("{}");

        assertEquals("idValue", dto.id);
        assertNull(dto.optionalField);
    }

    @Test
    void testMandatoryFieldNotFound() {
        MissingInjectableValueExcepion exception = assertThrows(
                MissingInjectableValueExcepion.class, () -> READER.readValue("{}"));

        assertThat(exception.getMessage())
            .startsWith("No 'injectableValues' configured, cannot inject value with id 'id'");
    }

    // Test for case of `optional = OptBoolean.FALSE`
    @Test
    void testRequiredAnnotatedField() throws Exception {
        // Should also fail even if DeserFeature disabled, if annotated
        ObjectReader reader = READER.forType(DtoWithRequired.class)
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE);

        MissingInjectableValueExcepion exception = assertThrows(
                MissingInjectableValueExcepion.class, () -> reader.readValue("{}"));

        assertThat(exception.getMessage())
            .startsWith("No 'injectableValues' configured, cannot inject value with id 'requiredValue'");

        // Also check the other code path, with non-null Injectables
        ObjectReader reader2 = reader.with(new InjectableValues.Std()
                .addValue("id", "idValue"));

        exception = assertThrows(
                MissingInjectableValueExcepion.class, () -> reader2.readValue("{}"));

        assertThat(exception.getMessage())
             .startsWith("No injectable value with id 'requiredValue' found (for property 'requiredField')");

        // And finally, work if value injected
        ObjectReader reader3 = reader.with(new InjectableValues.Std()
                .addValue("requiredValue", "FOO"));
        DtoWithRequired req = reader3.readValue("{}");
        assertEquals("FOO", req.requiredField);
    }

    @Test
    void testMandatoryFieldNotFoundWithInjectableValues() {
        ObjectReader reader = READER
                .with(new InjectableValues.Std());

        MissingInjectableValueExcepion exception = assertThrows(
                MissingInjectableValueExcepion.class, () -> reader.readValue("{}"));

        assertThat(exception.getMessage())
            .startsWith("No injectable value with id 'id' found (for property 'id')");
    }

    @Test
    void testMandatoryFieldNotFoundWithoutDeserializationFeature() throws Exception {
        ObjectReader reader = READER
                .with(new InjectableValues.Std()
                        .addValue("id", "idValue"))
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE);

        DtoWithOptional dto = reader.readValue("{}");

        assertEquals("idValue", dto.id);
        assertNull(dto.optionalField);
    }

    @Test
    void testMandatoryFieldNotFoundWithInjectableValuesWithoutDeserializationFeature() throws Exception {
        ObjectReader reader = READER
                .with(new InjectableValues.Std())
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE);

        DtoWithOptional dto = reader.readValue("{}");

        assertNull(dto.id);
        assertNull(dto.optionalField);
    }

    @Test
    void testOptionalFieldNotFoundWithoutInjectableValuesWithDeserializationFeature() throws Exception {
        ObjectReader reader = READER
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE);

        DtoWithOptional dto = reader.readValue("{}");

        assertNull(dto.id);
        assertNull(dto.optionalField);
    }
}
