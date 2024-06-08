package com.fasterxml.jackson.databind.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.FiveMinuteUser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteBufferUtilsTest extends DatabindTestUtil
{
    private final FiveMinuteUser TEST_USER = new FiveMinuteUser("Bob", "Burger",
            true, FiveMinuteUser.Gender.MALE,
            new byte[] { 1, 2, 3, 4, 5 });

    @Test
    public void testByteBufferInput() throws Exception {
        byte[] input = new byte[] { 1, 2, 3 };
        try (ByteBufferBackedInputStream wrapped = new ByteBufferBackedInputStream(ByteBuffer.wrap(input))) {
            assertEquals(3, wrapped.available());
            assertEquals(1, wrapped.read());
            byte[] buffer = new byte[10];
            assertEquals(2, wrapped.read(buffer, 0, 5));
            // And now ought to get -1
            assertEquals(-1, wrapped.read(buffer, 0, 5));
        }
    }

    @Test
    public void testByteBufferOutput() throws Exception {
        ByteBuffer b = ByteBuffer.wrap(new byte[10]);
        try (ByteBufferBackedOutputStream wrappedOut = new ByteBufferBackedOutputStream(b)) {
            wrappedOut.write(1);
            wrappedOut.write(new byte[] { 2, 3 });
            assertEquals(3, b.position());
            assertEquals(7, b.remaining());
        }
    }

    @Test
    public void testReadFromByteBuffer() throws Exception
    {
        final ObjectMapper mapper = sharedMapper();
        byte[] bytes = mapper.writeValueAsBytes(TEST_USER);
        ByteBuffer bb = ByteBuffer.wrap(bytes);

        try (InputStream in = new ByteBufferBackedInputStream(bb)) {
            FiveMinuteUser result = mapper.readValue(in, FiveMinuteUser.class);
            assertEquals(TEST_USER, result);
        }
    }

    @Test
    public void testWriteToByteBuffer() throws Exception
    {
        final ObjectMapper mapper = sharedMapper();
        byte[] bytes = mapper.writeValueAsBytes(TEST_USER);

        // Does require ByteBuffer that is big enough so
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        assertEquals(0, bb.position());
        assertEquals(bytes.length, bb.limit());
        try (OutputStream out = new ByteBufferBackedOutputStream(bb)) {
            mapper.writeValue(out, TEST_USER);
            assertEquals(0, bb.remaining());
            assertEquals(bytes.length, bb.position());
            assertEquals(bytes.length, bb.limit());
        }
    }
}
