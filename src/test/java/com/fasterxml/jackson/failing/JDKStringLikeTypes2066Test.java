package com.fasterxml.jackson.failing;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JDKStringLikeTypes2066Test extends BaseMapTest
{
    static class UUIDWrapper {
        public UUID value;
    }

    static class StrictUUIDWrapper {
        @JsonFormat(lenient=OptBoolean.FALSE)
        public UUID value;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [databind#2066]
    public void testUUIDLeniencyDefault() throws Exception
    {
        final ObjectMapper MAPPER = objectMapper();

        // By default, empty String OK
        assertNull(MAPPER.readValue(quote(""), UUID.class));

        UUIDWrapper w = MAPPER.readValue("{\"value\":\"\"}", UUIDWrapper.class);
        assertNull(w.value);
    }

    // [databind#2066]
    public void testUUIDLeniencyGlobal() throws Exception
    {
        final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
                .defaultLeniency(Boolean.FALSE)
                .build();
        try {
            STRICT_MAPPER.readValue(quote(""), UUID.class);
            fail("Should not pass");
        } catch (Exception e) {
            _verifyBadUUID(e);
        }

        try {
            STRICT_MAPPER.readValue("{\"value\":\"\"}", UUIDWrapper.class);
            fail("Should not pass");
        } catch (Exception e) {
            _verifyBadUUID(e);
        }
    }

    // [databind#2066]
    public void testUUIDLeniencyByType() throws Exception
    {
        final ObjectMapper STRICT_MAPPER = jsonMapperBuilder().build();
        STRICT_MAPPER.configOverride(UUID.class)
            .setFormat(JsonFormat.Value.forLeniency(false));

        try {
            STRICT_MAPPER.readValue(quote(""), UUID.class);
            fail("Should not pass");
        } catch (Exception e) {
            _verifyBadUUID(e);
        }

        try {
            STRICT_MAPPER.readValue("{\"value\":\"\"}", UUIDWrapper.class);
            fail("Should not pass");
        } catch (Exception e) {
            _verifyBadUUID(e);
        }
    }

    // [databind#2066]
    public void testUUIDLeniencyByProperty() throws Exception
    {
        final ObjectMapper MAPPER = objectMapper();
        try {
            MAPPER.readValue("{\"value\":\"\"}", StrictUUIDWrapper.class);
            fail("Should not pass");
        } catch (Exception e) {
            _verifyBadUUID(e);
        }
    }

    private void _verifyBadUUID(Exception e) {
        verifyException(e, "foobar"); // TODO: real exception message we want
    }
}
