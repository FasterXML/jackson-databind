package com.fasterxml.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class ReadOnlyDeserFailOnUnknown2719Test
{
    // [databind#2719]
    static class UserWithReadOnly {
        @JsonProperty(value = "username", access = JsonProperty.Access.READ_ONLY)
        public String name;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        public String password;
        public String login;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testFailOnIgnore() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(UserWithReadOnly.class);

        // First, fine to get 'login'
        UserWithReadOnly result = r.readValue(a2q("{'login':'foo'}"));
        assertEquals("foo", result.login);

        // but not 'password'
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            r.readValue(a2q("{'login':'foo', 'password':'bar'}"));
            fail("Should fail");
        } catch (MismatchedInputException e) {
            verifyException(e, "Ignored field");
        }

        // or 'username'
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            r.readValue(a2q("{'login':'foo', 'username':'bar'}"));
            fail("Should fail");
        } catch (MismatchedInputException e) {
            verifyException(e, "Ignored field");
        }
    }
}
