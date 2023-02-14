package com.fasterxml.jackson.databind.ser.jdk;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.databind.*;

public class UUIDSerializationTest extends BaseMapTest
{
    static class UUIDWrapperVanilla {
        public UUID uuid;

        public UUIDWrapperVanilla(UUID u) { uuid = u; }
    }

    static class UUIDWrapperBinary {
        // default with JSON is String, for use of (base64-encoded) Binary:
        @JsonFormat(shape = JsonFormat.Shape.BINARY)
        public UUID uuid;

        public UUIDWrapperBinary(UUID u) { uuid = u; }
    }

    private final ObjectMapper MAPPER = sharedMapper();

    // Verify that efficient UUID codec won't mess things up:
    public void testBasicUUIDs() throws IOException
    {
        // first, couple of generated UUIDs:
        for (String value : new String[] {
                "76e6d183-5f68-4afa-b94a-922c1fdb83f8",
                "540a88d1-e2d8-4fb1-9396-9212280d0a7f",
                "2c9e441d-1cd0-472d-9bab-69838f877574",
                "591b2869-146e-41d7-8048-e8131f1fdec5",
                "82994ac2-7b23-49f2-8cc5-e24cf6ed77be",
                "00000007-0000-0000-0000-000000000000"
        }) {
            UUID uuid = UUID.fromString(value);
            String json = MAPPER.writeValueAsString(uuid);
            assertEquals(q(uuid.toString()), json);

            // Also, wrt [#362], should convert cleanly
            String str = MAPPER.convertValue(uuid, String.class);
            assertEquals(value, str);
        }

        // then use templating; note that these are not exactly valid UUIDs
        // wrt spec (type bits etc), but JDK UUID should deal ok
        final String TEMPL = "00000000-0000-0000-0000-000000000000";
        final String chars = "123456789abcdef";

        for (int i = 0; i < chars.length(); ++i) {
            String value = TEMPL.replace('0', chars.charAt(i));
            UUID uuid = UUID.fromString(value);
            String json = MAPPER.writeValueAsString(uuid);
            assertEquals(q(uuid.toString()), json);
        }
    }

    public void testShapeOverrides() throws Exception
    {
        final String nullUUIDStr = "00000000-0000-0000-0000-000000000000";
        final UUID nullUUID = UUID.fromString(nullUUIDStr);

        // First, see that Binary per-property override works:
        assertEquals("{\"uuid\":\"AAAAAAAAAAAAAAAAAAAAAA==\"}",
                MAPPER.writeValueAsString(new UUIDWrapperBinary(nullUUID)));

        // but that without one we'd get String
        assertEquals("{\"uuid\":\""+nullUUIDStr+"\"}",
                MAPPER.writeValueAsString(new UUIDWrapperVanilla(nullUUID)));

        // but can also override by type
        final ObjectMapper m = jsonMapperBuilder()
                .withConfigOverride(UUID.class,
                        cfg -> cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.BINARY))
                )
                .build();
        assertEquals("{\"uuid\":\"AAAAAAAAAAAAAAAAAAAAAA==\"}",
                m.writeValueAsString(new UUIDWrapperVanilla(nullUUID)));
    }
}
