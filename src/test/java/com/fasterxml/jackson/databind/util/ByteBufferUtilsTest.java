package com.fasterxml.jackson.databind.util;

import java.nio.ByteBuffer;

import com.fasterxml.jackson.databind.BaseMapTest;

public class ByteBufferUtilsTest extends BaseMapTest
{
    public void testByteBufferInput() throws Exception {
        byte[] input = new byte[] { 1, 2, 3 };
        ByteBufferBackedInputStream wrapped = new ByteBufferBackedInputStream(ByteBuffer.wrap(input));
        assertEquals(3, wrapped.available());
        assertEquals(1, wrapped.read());
        byte[] buffer = new byte[10];
        assertEquals(2, wrapped.read(buffer, 0, 5));
        wrapped.close();
    }

    public void testByteBufferOutput() throws Exception {
        ByteBuffer b = ByteBuffer.wrap(new byte[10]);
        ByteBufferBackedOutputStream wrappedOut = new ByteBufferBackedOutputStream(b);
        wrappedOut.write(1);
        wrappedOut.write(new byte[] { 2, 3 });
        assertEquals(3, b.position());
        assertEquals(7, b.remaining());
        wrappedOut.close();
    }
}
