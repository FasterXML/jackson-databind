package com.fasterxml.jackson.databind.jsontype.vld;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that {@link MapperFeature#BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES}
 * is honored for "default typing" as well, and not just for annotation-based
 * polymorphic handling, as long as the default (lax) {@link PolymorphicTypeValidator}
 * is used: regardless of which method was used to enable default typing,
 * or in which order configuration was applied.
 */
@SuppressWarnings("deprecation")
public class DefaultTypingBlockUnsafeBaseTypesTest
    extends DatabindTestUtil
{
    static class ObjectWrapper {
        public Object value;
    }

    static class SerializableWrapper {
        public java.io.Serializable value;
    }

    private final static String FILE_AS_ARRAY = a2q("{'value':['java.io.File','/tmp/stuff']}");
    private final static String FILE_AS_PROPERTY = a2q("{'value':{'@class':'java.io.File'}}");

    /*
    /**********************************************************************
    /* Test methods, legacy `enableDefaultTyping()` variants
    /**********************************************************************
     */

    @Test
    public void testEnableDefaultTypingNoArgs() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        mapper.enableDefaultTyping();
        _verifyBlocked(mapper, FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
    }

    @Test
    public void testEnableDefaultTypingWithApplicability() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        _verifyBlocked(mapper, FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
        _verifyBlocked(mapper, FILE_AS_ARRAY, SerializableWrapper.class, "java.io.Serializable");
    }

    @Test
    public void testEnableDefaultTypingWithInclusion() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                JsonTypeInfo.As.WRAPPER_ARRAY);
        _verifyBlocked(mapper, FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
    }

    @Test
    public void testEnableDefaultTypingAsProperty() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                "@class");
        _verifyBlocked(mapper, FILE_AS_PROPERTY, ObjectWrapper.class, "java.lang.Object");
    }

    /*
    /**********************************************************************
    /* Test methods, ordering and other ways to enable
    /**********************************************************************
     */

    // Feature enabled only after default typing was enabled
    @Test
    public void testFeatureEnabledAfterDefaultTyping() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.enableDefaultTyping();
        mapper.configure(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES, true);
        _verifyBlocked(mapper, FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
    }

    // Default typing enabled via Builder, before the feature
    @Test
    public void testBuilderDefaultTypingBeforeFeature() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance)
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build();
        _verifyBlocked(mapper, FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
    }

    // Default typing enabled with default PTV (as returned by `ObjectMapper`)
    @Test
    public void testActivateWithMapperDefaultValidator() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator());
        _verifyBlocked(mapper, FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
    }

    // ... and rebuilt mapper must retain blocking
    @Test
    public void testRebuiltMapper() throws Exception
    {
        JsonMapper mapper = blockingMapper();
        mapper.enableDefaultTyping();
        JsonMapper rebuilt = mapper.rebuild().build();
        _verifyBlocked(rebuilt, FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
        _verifyBlocked(mapper.copy(), FILE_AS_ARRAY, ObjectWrapper.class, "java.lang.Object");
    }

    /*
    /**********************************************************************
    /* Test methods, cases that must remain unaffected
    /**********************************************************************
     */

    // Without the feature, legacy behavior remains (for backwards compatibility)
    @Test
    public void testFeatureDisabledNotBlocked() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.enableDefaultTyping();
        ObjectWrapper w = mapper.readValue(FILE_AS_ARRAY, ObjectWrapper.class);
        assertEquals(new java.io.File("/tmp/stuff"), w.value);
    }

    // Explicitly configured (non-default) validator is not replaced
    @Test
    public void testExplicitValidatorNotOverridden() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(java.io.File.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .activateDefaultTyping(ptv)
                .build();
        ObjectWrapper w = mapper.readValue(FILE_AS_ARRAY, ObjectWrapper.class);
        assertEquals(new java.io.File("/tmp/stuff"), w.value);

        // and same for mapper-wide default PTV used via legacy methods
        ObjectMapper mapper2 = jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .polymorphicTypeValidator(ptv)
                .build();
        mapper2.enableDefaultTyping();
        w = mapper2.readValue(FILE_AS_ARRAY, ObjectWrapper.class);
        assertEquals(new java.io.File("/tmp/stuff"), w.value);
    }

    // Safe (non-blocked) base types still work with the feature enabled
    @Test
    public void testSafeBaseTypeStillWorks() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        String json = mapper.writeValueAsString(new NumberWrapper(42));
        NumberWrapper w = mapper.readValue(json, NumberWrapper.class);
        assertEquals(Integer.valueOf(42), w.value);
    }

    static class NumberWrapper {
        public Number value;

        protected NumberWrapper() { }
        NumberWrapper(Number v) { value = v; }
    }

    // Validator returned via DefaultTypeResolverBuilder must reflect the feature
    @Test
    public void testDefaultTyperValidator() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        mapper.enableDefaultTyping();
        DeserializationConfig config = mapper.getDeserializationConfig();
        assertEquals(DefaultBaseTypeLimitingValidator.class,
                ((StdTypeResolverBuilder) config.getDefaultTyper(null)).subTypeValidator(config).getClass());
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private JsonMapper blockingMapper() {
        return jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build();
    }

    private void _verifyBlocked(ObjectMapper mapper, String json, Class<?> type,
            String baseType) throws Exception
    {
        try {
            Object result = mapper.readValue(json, type);
            fail("Should not pass, got: "+result);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Configured `PolymorphicTypeValidator`");
            verifyException(e, "denied resolution of all subtypes of base type");
            verifyException(e, baseType);
        }
    }
}
