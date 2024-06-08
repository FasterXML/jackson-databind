package com.fasterxml.jackson.databind.deser.filter;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

// [databind#4200]: Nulls.FAIL not taken into account with DELEGATING creator
public class NullConversionsForContent4200Test
{
    static class DelegatingWrapper4200 {
        private final Map<String, String> value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        DelegatingWrapper4200(@JsonSetter(contentNulls = Nulls.FAIL)
            Map<String, String> value)
        {
            this.value = value;
        }

        public Map<String, String> getValue() {
            return value;
        }
    }

    static class SetterWrapper4200 {
        private Map<String, String> value;

        public Map<String, String> getValue() {
            return value;
        }

        @JsonSetter(contentNulls = Nulls.FAIL)
        public void setValue(Map<String, String> value) {
            this.value = value;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDelegatingCreatorNulls4200() throws Exception
    {
        try {
            MAPPER.readValue(a2q("{'foo': null}"), DelegatingWrapper4200.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value");
        }
    }

    @Test
    public void testSetterNulls4200() throws Exception
    {
        try {
            MAPPER.readValue(a2q("{'value':{'foo': null}}"),
                    SetterWrapper4200.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value");
        }
    }
}
