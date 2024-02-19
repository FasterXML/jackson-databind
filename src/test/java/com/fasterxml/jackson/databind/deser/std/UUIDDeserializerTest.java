package com.fasterxml.jackson.databind.deser.std;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UUIDDeserializerTest {
  private static final UUID TEST_UUID = UUID.fromString("a7161c6c-be14-4ae3-a3c4-f27c2b2c6ef4");

  @Test
  void testCanDeserializeUUIDFromString() throws IOException {
    final UUIDDeserializer deserializer = new UUIDDeserializer();
    assertEquals(TEST_UUID, deserializer._deserialize(TEST_UUID.toString(), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64() throws IOException {
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(TEST_UUID, deserializer._deserialize(Base64.getEncoder().encodeToString(getBytesFromUUID(TEST_UUID)), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64WithoutPadding() throws IOException {
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(TEST_UUID, deserializer._deserialize(Base64.getEncoder().withoutPadding().encodeToString(getBytesFromUUID(TEST_UUID)), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64Url() throws IOException {
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(TEST_UUID, deserializer._deserialize(Base64.getUrlEncoder().encodeToString(getBytesFromUUID(TEST_UUID)), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64UrlWithoutPadding() throws IOException {
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(TEST_UUID, deserializer._deserialize(Base64.getUrlEncoder().withoutPadding().encodeToString(getBytesFromUUID(TEST_UUID)), null));
  }

  private static byte[] getBytesFromUUID(UUID uuid) {
    final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }
}