package tools.jackson.databind.deser.jdk;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Base64Variants;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static tools.jackson.databind.testutil.JacksonTestUtilBase.q;
import static tools.jackson.databind.testutil.JacksonTestUtilBase.verifyException;

// Tests for [databind#4394]
public class UUIDDeserializationTest
{
    private static final UUID TEST_UUID = UUID.fromString("a7161c6c-be14-4ae3-a3c4-f27c2b2c6ef4");

    private final TestableUUIDDeserializer UUID_DESERIALIZER = new TestableUUIDDeserializer();

    static class TestableUUIDDeserializer extends UUIDDeserializer
    {
        @Override
        public UUID _deserialize(String id, DeserializationContext ctxt)
        {
            return super._deserialize(id, ctxt);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();
    
    @Test
    public void testUUID() throws Exception
    {
        final String NULL_UUID = "00000000-0000-0000-0000-000000000000";
        final ObjectReader r = MAPPER.readerFor(UUID.class);

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
            assertEquals(uuid,
                    r.without(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                        .readValue(q(value)));
        }
        // then use templating; note that these are not exactly valid UUIDs
        // wrt spec (type bits etc), but JDK UUID should deal ok
        final String TEMPL = NULL_UUID;
        final String chars = "123456789abcdefABCDEF";

        for (int i = 0; i < chars.length(); ++i) {
            String value = TEMPL.replace('0', chars.charAt(i));
            assertEquals(UUID.fromString(value).toString(),
                    r.readValue(q(value)).toString());
        }

        // also: see if base64 encoding works as expected
        String base64 = Base64Variants.getDefaultVariant().encode(new byte[16]);
        assertEquals(UUID.fromString(NULL_UUID),
                r.readValue(q(base64)));
    }

    @Test
    public void testUUIDInvalid() throws Exception
    {
        // and finally, exception handling too [databind#1000], for invalid cases
        try {
            MAPPER.readValue(q("abcde"), UUID.class);
            fail("Should fail on invalid UUID string");
        } catch (InvalidFormatException e) {
            verifyException(e, "UUID has to be represented by standard");
        }
        try {
            MAPPER.readValue(q("76e6d183-5f68-4afa-b94a-922c1fdb83fx"), UUID.class);
            fail("Should fail on invalid UUID string");
        } catch (InvalidFormatException e) {
            verifyException(e, "non-hex character 'x'");
        }
        // should also test from-bytes version, but that's trickier... leave for now.
    }

    @Test
    public void testUUIDAux() throws Exception
    {
        final UUID value = UUID.fromString("76e6d183-5f68-4afa-b94a-922c1fdb83f8");

        // first, null should come as null
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writePOJO(null);
            assertNull(MAPPER.readValue(buf.asParser(ObjectReadContext.empty()), UUID.class));
        }

        // then, UUID itself come as is:
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writePOJO(value);
            assertSame(value, MAPPER.readValue(buf.asParser(ObjectReadContext.empty()), UUID.class));

            // and finally from byte[]
            // oh crap; JDK UUID just... sucks. Not even byte[] accessors or constructors? Huh?
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeLong(value.getMostSignificantBits());
            out.writeLong(value.getLeastSignificantBits());
            out.close();
            byte[] data = bytes.toByteArray();
            assertEquals(16, data.length);

            // Let's create fresh TokenBuffer, not reuse one
            try (TokenBuffer buf2 = TokenBuffer.forGeneration()) {
                buf2.writePOJO(data);
    
                UUID value2 = MAPPER.readValue(buf2.asParser(), UUID.class);
    
                assertEquals(value, value2);
            }
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
