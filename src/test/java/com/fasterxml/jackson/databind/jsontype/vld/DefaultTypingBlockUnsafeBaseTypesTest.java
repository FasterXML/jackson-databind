package com.fasterxml.jackson.databind.jsontype.vld;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that {@link MapperFeature#BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES}
 * applies to Default Typing using the default validator, regardless of
 * how it was enabled ([GHSA-x6mh-m44j-q5vw]).
 */
@SuppressWarnings("deprecation")
public class DefaultTypingBlockUnsafeBaseTypesTest
    extends DatabindTestUtil
{
    static class ObjectWrapper {
        public Object value;
    }

    static class NumberWrapper {
        public Number value;
    }

    private final static String FILE_JSON = a2q("{'value':['java.io.File','/tmp/stuff']}");

    @Test
    public void testLegacyEnableDefaultTyping() throws Exception
    {
        _verifyBlocked(blockingMapper().enableDefaultTyping());
        _verifyBlocked(blockingMapper().enableDefaultTyping(DefaultTyping.NON_FINAL));
        _verifyBlocked(blockingMapper().enableDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE,
                JsonTypeInfo.As.WRAPPER_ARRAY));
        _verifyBlocked(blockingMapper().enableDefaultTypingAsProperty(
                DefaultTyping.OBJECT_AND_NON_CONCRETE, "@class"),
                a2q("{'value':{'@class':'java.io.File'}}"));
    }

    @Test
    public void testOtherWaysAndOrdering() throws Exception
    {
        JsonMapper mapper = blockingMapper();
        _verifyBlocked(mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator()));
        _verifyBlocked(mapper.rebuild().build());
        _verifyBlocked(mapper.copy());

        // Feature enabled after Default Typing
        _verifyBlocked(newJsonMapper().enableDefaultTyping()
                .configure(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES, true));
        _verifyBlocked(jsonMapperBuilder()
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance)
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build());
    }

    @Test
    public void testUnaffectedCases() throws Exception
    {
        // Feature not enabled: legacy behavior retained
        _verifyAllowed(newJsonMapper().enableDefaultTyping());

        // Explicit (non-default) validator not overridden
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(File.class)
                .build();
        _verifyAllowed(jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .activateDefaultTyping(ptv)
                .build());
        _verifyAllowed(jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .polymorphicTypeValidator(ptv)
                .build()
                .enableDefaultTyping());

        // Safe base type still allowed
        NumberWrapper w = blockingMapper().enableDefaultTyping()
                .readValue(a2q("{'value':['java.lang.Integer',42]}"), NumberWrapper.class);
        assertEquals(Integer.valueOf(42), w.value);
    }

    private JsonMapper blockingMapper() {
        return jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build();
    }

    private void _verifyBlocked(ObjectMapper mapper) throws Exception {
        _verifyBlocked(mapper, FILE_JSON);
    }

    private void _verifyBlocked(ObjectMapper mapper, String json) throws Exception {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue(json, ObjectWrapper.class));
        verifyException(e, "denied resolution of all subtypes of base type `java.lang.Object`");
    }

    private void _verifyAllowed(ObjectMapper mapper) throws Exception {
        assertEquals(new File("/tmp/stuff"), mapper.readValue(FILE_JSON, ObjectWrapper.class).value);
    }
}
