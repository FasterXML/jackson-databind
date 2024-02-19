package com.fasterxml.jackson.databind.deser.std;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UUIDDeserializerTest {
  @Test
  void testCanDeserializeUUIDFromString() throws IOException {
    final UUID uuid = UUID.randomUUID();
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(uuid, deserializer._deserialize(uuid.toString(), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64() throws IOException {
    final UUID uuid = UUID.randomUUID();
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(uuid, deserializer._deserialize(Base64.getEncoder().encodeToString(getBytesFromUUID(uuid)), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64WithoutPadding() throws IOException {
    final UUID uuid = UUID.randomUUID();
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(uuid, deserializer._deserialize(Base64.getEncoder().withoutPadding().encodeToString(getBytesFromUUID(uuid)), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64Url() throws IOException {
    final UUID uuid = UUID.randomUUID();
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(uuid, deserializer._deserialize(Base64.getUrlEncoder().encodeToString(getBytesFromUUID(uuid)), null));
  }

  @Test
  void testCanDeserializeUUIDFromBase64UrlWithoutPadding() throws IOException {
    final UUID uuid = UUID.randomUUID();
    final UUIDDeserializer deserializer = new UUIDDeserializer();

    assertEquals(uuid, deserializer._deserialize(Base64.getUrlEncoder().withoutPadding().encodeToString(getBytesFromUUID(uuid)), null));
  }

  private static byte[] getBytesFromUUID(UUID uuid) {
    final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }
}