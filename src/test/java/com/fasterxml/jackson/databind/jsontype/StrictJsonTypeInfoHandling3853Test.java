package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class StrictJsonTypeInfoHandling3853Test extends BaseMapTest {

    @JsonTypeInfo(use = Id.NAME)
    interface Command {
    }

    @JsonTypeName("do-something")
    static class DoSomethingCommand implements Command {
    }

    public void testDefaultHasStrictTypeHandling() throws Exception {
        ObjectMapper om = new ObjectMapper();
        om.registerSubtypes(DoSomethingCommand.class);

        // This should pass in all scenarios
        verifyDeserializationWithFullTypeInfo(om);
        // and throw an exception if the target was a super-type in all cases
        verifyInvalidTypeIdWithSuperclassTarget(om);

        // Default is to disallow the deserialization without a type if the target
        // is a concrete sub-type
        verifyInvalidTypeIdWithConcreteTarget(om);
    }

    public void testExplicitNonStrictTypeHandling() throws Exception {
        ObjectMapper om = JsonMapper.builder().disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
        om.registerSubtypes(DoSomethingCommand.class);

        // This should pass in all scenarios
        verifyDeserializationWithFullTypeInfo(om);
        // and throw an exception if the target was a super-type in all cases
        verifyInvalidTypeIdWithSuperclassTarget(om);

        // Default is to allow the deserialization without a type if the target
        // is a concrete sub-type
        verifyDeserializationWithConcreteTarget(om);
    }

    public void testStrictTypeHandling() throws Exception {
        ObjectMapper om = JsonMapper.builder().enable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();
        om.registerSubtypes(DoSomethingCommand.class);

        // This should pass in all scenarios
        verifyDeserializationWithFullTypeInfo(om);
        // and throw an exception if the target was a super-type in all cases
        verifyInvalidTypeIdWithSuperclassTarget(om);

        // With strict mode enabled, fail if there's no type information on the
        // JSON
        verifyInvalidTypeIdWithConcreteTarget(om);

    }

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
        assertType(cmd, DoSomethingCommand.class);
    }

    private void verifyDeserializationWithFullTypeInfo(ObjectMapper om) throws Exception {
        Command cmd = om.readValue("{\"@type\":\"do-something\"}", Command.class);
        assertType(cmd, DoSomethingCommand.class);
        cmd = om.readValue("{\"@type\":\"do-something\"}", DoSomethingCommand.class);
        assertType(cmd, DoSomethingCommand.class);
    }
}
