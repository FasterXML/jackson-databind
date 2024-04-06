package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#3877]: allow configuration of per-type strict type handling
public class OverrideStrictTypeInfoHandling3877Test extends DatabindTestUtil {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
    */

    @JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.DEFAULT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DoDefaultCommand.class, name = "do-default")})
    interface DefaultCommand {}

    static class DoDefaultCommand implements DefaultCommand {}

    @JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DoTrueCommand.class, name = "do-true")})
    interface TrueCommand {}

    static class DoTrueCommand implements TrueCommand {}

    @JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.FALSE)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DoFalseCommand.class, name = "do-false")})
    interface FalseCommand {}

    static class DoFalseCommand implements FalseCommand {}

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    private final ObjectMapper ENABLED_MAPPER = JsonMapper.builder().enable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
    private final ObjectMapper DISABLED_MAPPER = JsonMapper.builder().disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
    private final ObjectMapper DEFAULT_MAPPER = JsonMapper.builder().build();

    @Test
    public void testMissingTypeId() throws Exception {
        // super types fail on missing-id no matter what
        verifyFailureMissingTypeId("{}", FalseCommand.class, ENABLED_MAPPER);
        verifyFailureMissingTypeId("{}", FalseCommand.class, DEFAULT_MAPPER);
        verifyFailureMissingTypeId("{}", FalseCommand.class, DISABLED_MAPPER);
        verifyFailureMissingTypeId("{}", TrueCommand.class, ENABLED_MAPPER);
        verifyFailureMissingTypeId("{}", TrueCommand.class, DEFAULT_MAPPER);
        verifyFailureMissingTypeId("{}", TrueCommand.class, DISABLED_MAPPER);
        verifyFailureMissingTypeId("{}", DefaultCommand.class, ENABLED_MAPPER);
        verifyFailureMissingTypeId("{}", DefaultCommand.class, DEFAULT_MAPPER);
        verifyFailureMissingTypeId("{}", DefaultCommand.class, DISABLED_MAPPER);

        // overrides : to require type id
        verifySuccessWithNonNullAndType("{}", DoFalseCommand.class, ENABLED_MAPPER);
        verifySuccessWithNonNullAndType("{}", DoFalseCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType("{}", DoFalseCommand.class, DISABLED_MAPPER);
        // overrides : do not require type id
        verifyFailureMissingTypeId("{}", DoTrueCommand.class, ENABLED_MAPPER);
        verifyFailureMissingTypeId("{}", DoTrueCommand.class, DEFAULT_MAPPER);
        verifyFailureMissingTypeId("{}", DoTrueCommand.class, DISABLED_MAPPER);
        // overrides : defaults
        verifyFailureMissingTypeId("{}", DoDefaultCommand.class, ENABLED_MAPPER);
        verifyFailureMissingTypeId("{}", DoDefaultCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType("{}", DoDefaultCommand.class, DISABLED_MAPPER);
    }

    @Test
    public void testSuccessWhenTypeIdIsProvided() throws Exception {
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-false'}"), FalseCommand.class, ENABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-false'}"), FalseCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-false'}"), FalseCommand.class, DISABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-false'}"), DoFalseCommand.class, ENABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-false'}"), DoFalseCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-false'}"), DoFalseCommand.class, DISABLED_MAPPER);

        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-true'}"), TrueCommand.class, ENABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-true'}"), TrueCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-true'}"), TrueCommand.class, DISABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-true'}"), DoTrueCommand.class, ENABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-true'}"), DoTrueCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-true'}"), DoTrueCommand.class, DISABLED_MAPPER);

        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-default'}"), DefaultCommand.class, ENABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-default'}"), DefaultCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-default'}"), DefaultCommand.class, DISABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-default'}"), DoDefaultCommand.class, ENABLED_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-default'}"), DoDefaultCommand.class, DEFAULT_MAPPER);
        verifySuccessWithNonNullAndType(a2q("{'@type': 'do-default'}"), DoDefaultCommand.class, DISABLED_MAPPER);
    }

    private <T> void verifySuccessWithNonNullAndType(String json, Class<T> clazz, ObjectMapper om) throws Exception {
        T bean = om.readValue(json, clazz);
        assertNotNull(bean);
        assertInstanceOf(clazz, bean);
    }

    private void verifyFailureMissingTypeId(String json, Class<?> clazz, ObjectMapper om) throws Exception {
        try {
            om.readValue(json, clazz);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property '@type'");
        }
    }
}
