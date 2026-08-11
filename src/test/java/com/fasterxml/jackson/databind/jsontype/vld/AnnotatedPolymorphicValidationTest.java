package com.fasterxml.jackson.databind.jsontype.vld;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for verifying that "unsafe" base type(s) for polymorphic deserialization
 * are correctly handled wrt {@link MapperFeature#BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES}.
 */
public class AnnotatedPolymorphicValidationTest
    extends DatabindTestUtil
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

    // [databind#6156]
    static class WrappedPolymorphicComparable {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS)
        public Comparable<?> value;

        protected WrappedPolymorphicComparable() { }
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

    // [databind#6156]
    static class ComparablesAreOkValidator extends DefaultBaseTypeLimitingValidator
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean isUnsafeBaseType(MapperConfig<?> config, JavaType baseType)
        {
            // only override handling for `Comparable`
            if (baseType.hasRawClass(Comparable.class)) {
                return false;
            }
            return super.isUnsafeBaseType(config, baseType);
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

    @Test
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

    // [databind#6156]: `Comparable` is too wide a base type to allow
    @Test
    public void testPolymorphicWithComparableBaseType() throws IOException
    {
        final String JSON = a2q("{'value':['java.io.File','/tmp/stuff']}");

        try {
            /*w =*/ MAPPER.readValue(JSON, WrappedPolymorphicComparable.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Configured");
            verifyException(e, "all subtypes of base type");
            verifyException(e, "java.lang.Comparable");
        }

        // but may be allowed with custom validator that overrides base type check
        ObjectMapper customMapper = JsonMapper.builder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .polymorphicTypeValidator(new ComparablesAreOkValidator())
                .build();
        WrappedPolymorphicComparable w = customMapper.readValue(JSON,
                WrappedPolymorphicComparable.class);
        assertEquals(new java.io.File("/tmp/stuff"), w.value);
    }
}
