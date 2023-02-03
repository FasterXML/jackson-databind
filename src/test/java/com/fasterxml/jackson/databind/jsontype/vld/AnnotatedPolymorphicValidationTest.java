package com.fasterxml.jackson.databind.jsontype.vld;

import java.io.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;

/**
 * Unit tests for verifying that "unsafe" base type(s) for polymorphic deserialization
 * are correctly handled wrt {@link MapperFeature#BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES}.
 */
public class AnnotatedPolymorphicValidationTest
    extends BaseMapTest
{
    static class WrappedPolymorphicUntyped {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS)
        public Object value;

        protected WrappedPolymorphicUntyped() { }
    }

    static class WrappedPolymorphicUntypedSer {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS)
        public java.io.Serializable value;

        protected WrappedPolymorphicUntypedSer() { }
    }

    static class NumbersAreOkValidator extends DefaultBaseTypeLimitingValidator
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean isUnsafeBaseType(MapperConfig<?> config, JavaType baseType)
        {
            // only override handling for `Object`
            if (baseType.hasRawClass(Object.class)) {
                return false;
            }
            return super.isUnsafeBaseType(config, baseType);
        }

        @Override
        protected boolean isSafeSubType(MapperConfig<?> config,
                JavaType baseType, JavaType subType) {
            return baseType.isTypeOrSubTypeOf(Number.class);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
            .build();

    public void testPolymorphicWithUnsafeBaseType() throws IOException
    {
        final String JSON = a2q("{'value':10}");
        // by default, we should NOT be allowed to deserialize due to unsafe base type
        try {
            /*w =*/ MAPPER.readValue(JSON, WrappedPolymorphicUntyped.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Configured");
            verifyException(e, "all subtypes of base type");
        }

        // but may with proper validator
        ObjectMapper customMapper = JsonMapper.builder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .polymorphicTypeValidator(new NumbersAreOkValidator())
                .build();

        WrappedPolymorphicUntyped w = customMapper.readValue(JSON, WrappedPolymorphicUntyped.class);
        assertEquals(Integer.valueOf(10), w.value);

        // but yet again, it is not opening up all types (just as an example)

        try {
            customMapper.readValue(JSON, WrappedPolymorphicUntypedSer.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Configured");
            verifyException(e, "all subtypes of base type");
            verifyException(e, "java.io.Serializable");
        }
    }
}
