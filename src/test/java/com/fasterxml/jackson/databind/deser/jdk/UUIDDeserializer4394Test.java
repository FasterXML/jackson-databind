package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [databind#4394]
public class UUIDDeserializer4394Test
{
    private static final UUID TEST_UUID = UUID.fromString("a7161c6c-be14-4ae3-a3c4-f27c2b2c6ef4");

    private final TestableUUIDDeserializer UUID_DESERIALIZER = new TestableUUIDDeserializer();

    static class TestableUUIDDeserializer extends UUIDDeserializer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public UUID _deserialize(String id, DeserializationContext ctxt) throws IOException
        {
            return super._deserialize(id, ctxt);
        }
    }
  
    @Test
    void testCanDeserializeUUIDFromString() throws Exception {
        assertEquals(TEST_UUID, UUID_DESERIALIZER._deserialize(TEST_UUID.toString(), null));
    }

    @Test
    void testCanDeserializeUUIDFromBase64() throws Exception {
        assertEquals(TEST_UUID, UUID_DESERIALIZER._deserialize(Base64.getEncoder().encodeToString(getBytesFromUUID(TEST_UUID)), null));
    }

    @Test
    void testCanDeserializeUUIDFromBase64WithoutPadding() throws Exception {
        assertEquals(TEST_UUID, UUID_DESERIALIZER._deserialize(Base64.getEncoder().withoutPadding().encodeToString(getBytesFromUUID(TEST_UUID)), null));
    }

    @Test
    void testCanDeserializeUUIDFromBase64Url() throws Exception {
        assertEquals(TEST_UUID, UUID_DESERIALIZER._deserialize(Base64.getUrlEncoder().encodeToString(getBytesFromUUID(TEST_UUID)), null));
    }

    @Test
    void testCanDeserializeUUIDFromBase64UrlWithoutPadding() throws Exception {
        assertEquals(TEST_UUID, UUID_DESERIALIZER._deserialize(Base64.getUrlEncoder().withoutPadding().encodeToString(getBytesFromUUID(TEST_UUID)), null));
    }

    private static byte[] getBytesFromUUID(UUID uuid) {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
