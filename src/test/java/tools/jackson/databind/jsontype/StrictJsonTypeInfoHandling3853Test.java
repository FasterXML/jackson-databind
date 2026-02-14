package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class StrictJsonTypeInfoHandling3853Test extends DatabindTestUtil {

    @JsonTypeInfo(use = Id.NAME)
    interface Command {
    }

    @JsonTypeName("do-something")
    static class DoSomethingCommand implements Command {
    }

    // [databind#3877]
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

    @Test
    public void testDefaultHasStrictTypeHandling() throws Exception {
        ObjectMapper om = jsonMapperBuilder()
                .registerSubtypes(DoSomethingCommand.class)
                .build();

        // This should pass in all scenarios
        verifyDeserializationWithFullTypeInfo(om);
        // and throw an exception if the target was a super-type in all cases
        verifyInvalidTypeIdWithSuperclassTarget(om);

        // Default is to disallow the deserialization without a type if the target
        // is a concrete sub-type
        verifyInvalidTypeIdWithConcreteTarget(om);
    }

    @Test
    public void testExplicitNonStrictTypeHandling() throws Exception {
        ObjectMapper om = jsonMapperBuilder()
                .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
                .registerSubtypes(DoSomethingCommand.class)
                .build();

        // This should pass in all scenarios
        verifyDeserializationWithFullTypeInfo(om);
        // and throw an exception if the target was a super-type in all cases
        verifyInvalidTypeIdWithSuperclassTarget(om);

        // Default is to allow the deserialization without a type if the target
        // is a concrete sub-type
        verifyDeserializationWithConcreteTarget(om);
    }

    @Test
    public void testStrictTypeHandling() throws Exception {
        ObjectMapper om = jsonMapperBuilder()
                .enable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
                .registerSubtypes(DoSomethingCommand.class)
                .build();

        // This should pass in all scenarios
        verifyDeserializationWithFullTypeInfo(om);
        // and throw an exception if the target was a super-type in all cases
        verifyInvalidTypeIdWithSuperclassTarget(om);

        // With strict mode enabled, fail if there's no type information on the
        // JSON
        verifyInvalidTypeIdWithConcreteTarget(om);
    }

    // [databind#3877]
    @Test
    public void testMissingTypeId() throws Exception {
        final ObjectMapper ENABLED_MAPPER = JsonMapper.builder().enable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
        final ObjectMapper DISABLED_MAPPER = JsonMapper.builder().disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
        final ObjectMapper DEFAULT_MAPPER = JsonMapper.builder().build();

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

    // [databind#3877]
    @Test
    public void testSuccessWhenTypeIdIsProvided() throws Exception {
        final ObjectMapper ENABLED_MAPPER = JsonMapper.builder().enable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
        final ObjectMapper DISABLED_MAPPER = JsonMapper.builder().disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
        final ObjectMapper DEFAULT_MAPPER = JsonMapper.builder().build();

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

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void verifyInvalidTypeIdWithSuperclassTarget(ObjectMapper om) throws Exception {
        try {
            om.readValue("{}", Command.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property '@type'");
        }
    }

    private void verifyInvalidTypeIdWithConcreteTarget(ObjectMapper om) throws Exception {
        try {
            om.readValue("{}", DoSomethingCommand.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property '@type'");
        }
    }

    private void verifyDeserializationWithConcreteTarget(ObjectMapper om) throws Exception {
        DoSomethingCommand cmd = om.readValue("{}", DoSomethingCommand.class);
        assertInstanceOf(DoSomethingCommand.class, cmd);
    }

    private void verifyDeserializationWithFullTypeInfo(ObjectMapper om) throws Exception {
        Command cmd = om.readValue("{\"@type\":\"do-something\"}", Command.class);
        assertInstanceOf(DoSomethingCommand.class, cmd);
        cmd = om.readValue("{\"@type\":\"do-something\"}", DoSomethingCommand.class);
        assertInstanceOf(DoSomethingCommand.class, cmd);
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
