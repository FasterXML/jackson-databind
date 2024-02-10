package com.fasterxml.jackson.databind.deser.creators;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AnySetterForCreator562Test extends DatabindTestUtil
{
    // [databind#562]
    static class POJO562
    {
        String a;

        Map<String,Object> stuff;

        @JsonCreator
        public POJO562(@JsonProperty("a") String a,
            @JsonAnySetter Map<String, Object> leftovers
        ) {
            this.a = a;
            stuff = leftovers;
        }
    }

    // [databind#562]: failing cacse
    static class MultipleAny562
    {
        @JsonCreator
        public MultipleAny562(@JsonProperty("a") String a,
            @JsonAnySetter Map<String, Object> leftovers,
            @JsonAnySetter Map<String, Object> leftovers2) {
            throw new Error("Should never get here!");
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#562]
    @Test
    public void testAnySetterViaCreator562() throws Exception
    {
        Map<String, Object> expected = new HashMap<>();
        expected.put("b", Integer.valueOf(42));
        expected.put("c", Integer.valueOf(111));

        POJO562 pojo = MAPPER.readValue(a2q(
                "{'a':'value', 'b':42, 'c': 111}"
                ),
                POJO562.class);

        assertEquals("value", pojo.a);
        assertEquals(expected, pojo.stuff);
    }

    // [databind#562]
    @Test
    public void testAnySetterViaCreator562FailForDup() throws Exception
    {
        try {
            MAPPER.readValue("{}", MultipleAny562.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Invalid type definition");
            verifyException(e, "More than one 'any-setter'");
        }
    }
}
