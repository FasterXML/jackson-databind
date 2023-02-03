package com.fasterxml.jackson.databind.jsontype.vld;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests to verify working of customizable {@PolymorphicTypeValidator},
 * see [databind#2195], regarding verification of base type checks.
 *
 * @since 2.10
 */
public class ValidatePolymBaseTypeTest extends BaseMapTest
{
    // // // Value types

    static abstract class BaseValue {
        public int x = 3;
    }

    static class BadValue extends BaseValue { }
    static class GoodValue extends BaseValue { }

    // // // Wrapper types

    static final class AnnotatedGoodWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public GoodValue value;

        protected AnnotatedGoodWrapper() { value = new GoodValue(); }
    }

    static final class AnnotatedBadWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public BadValue value;

        protected AnnotatedBadWrapper() { value = new BadValue(); }
    }

    static final class DefTypeGoodWrapper {
        public GoodValue value;

        protected DefTypeGoodWrapper() { value = new GoodValue(); }
    }

    static final class DefTypeBadWrapper {
        public BadValue value;

        protected DefTypeBadWrapper() { value = new BadValue(); }
    }

    // // // Validator implementations

    static class BaseTypeValidator extends PolymorphicTypeValidator {
        private static final long serialVersionUID = 1L;

        @Override
        public Validity validateBaseType(MapperConfig<?> ctxt, JavaType baseType) {
            final Class<?> raw = baseType.getRawClass();
            if (raw == BadValue.class) {
                return Validity.DENIED;
            }
            if (raw == GoodValue.class) {
                return Validity.ALLOWED;
            }
            // defaults to denied, then:
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(MapperConfig<?> ctxt, JavaType baseType, String subClassName) {
            return Validity.DENIED;
        }

        @Override
        public Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType, JavaType subType) {
            return Validity.DENIED;
        }
    }

    // // // Mappers with Default Typing

    // // // Mappers without Default Typing (explicit annotation needed)

    private final ObjectMapper MAPPER_ANNOTATED = jsonMapperBuilder()
            .polymorphicTypeValidator(new BaseTypeValidator())
            .build();

    private final ObjectMapper MAPPER_DEF_TYPING = jsonMapperBuilder()
            // Since GoodBalue, BadValue not abstraction need to use non-final
            .activateDefaultTyping(new BaseTypeValidator(), DefaultTyping.NON_FINAL)
            .build();

    /*
    /**********************************************************************
    /* Test methods: annotated
    /**********************************************************************
     */

    public void testAnnotedGood() throws Exception {
        final String json = MAPPER_ANNOTATED.writeValueAsString(new AnnotatedGoodWrapper());
        // should work ok
        assertNotNull(MAPPER_DEF_TYPING.readValue(json, AnnotatedGoodWrapper.class));
    }

    public void testAnnotedBad() throws Exception {
        final String json = MAPPER_ANNOTATED.writeValueAsString(new AnnotatedBadWrapper());
        // should fail
        try {
            MAPPER_ANNOTATED.readValue(json, AnnotatedBadWrapper.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Configured `PolymorphicTypeValidator`");
            verifyException(e, "denied resolution of");
            verifyException(e, "all subtypes of base type");
        }
    }

    /*
    /**********************************************************************
    /* Test methods: default typing
    /**********************************************************************
     */

    public void testDefaultGood() throws Exception {
        final String json = MAPPER_DEF_TYPING.writeValueAsString(new DefTypeGoodWrapper());
        // should work ok
        assertNotNull(MAPPER_DEF_TYPING.readValue(json, DefTypeGoodWrapper.class));
    }

    public void testDefaultBad() throws Exception {
        final String json = MAPPER_DEF_TYPING.writeValueAsString(new DefTypeBadWrapper());
        // should fail
        try {
            MAPPER_DEF_TYPING.readValue(json, DefTypeBadWrapper.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Configured `PolymorphicTypeValidator`");
            verifyException(e, "denied resolution of");
            verifyException(e, "all subtypes of base type");
        }
    }
}
