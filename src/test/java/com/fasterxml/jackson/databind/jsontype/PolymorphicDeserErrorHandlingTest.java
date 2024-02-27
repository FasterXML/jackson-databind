package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class PolymorphicDeserErrorHandlingTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
            property = "clazz")
    abstract static class BaseForUnknownClass {
    }

    static class BaseUnknownWrapper {
        public BaseForUnknownClass value;
    }

    // [databind#2668]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Child1.class, name = "child1"),
        @JsonSubTypes.Type(value = Child2.class, name = "child2")
    })
    static class Parent2668 {
    }

    static class Child1 extends Parent2668 {
        public String bar;
    }

    static class Child2 extends Parent2668 {
        public String baz;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnknownClassAsSubtype() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(BaseUnknownWrapper.class)
                .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        BaseUnknownWrapper w = reader.readValue(a2q
                ("{'value':{'clazz':'com.foobar.Nothing'}}'"));
        assertNotNull(w);
    }

    // [databind#2668]
    @Test
    public void testSubType2668() throws Exception
    {
        String json = "{\"type\": \"child2\", \"baz\":\"1\"}"; // JSON for Child2

        try {
            /*Child1 c =*/ MAPPER.readValue(json, Child1.class); // Deserializing into Child1
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "not subtype of");
        }
    }
}
