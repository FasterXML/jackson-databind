package com.fasterxml.jackson.databind.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Simple {@link InputStream} implementation that exposes currently
 * available content of a {@link ByteBuffer}.
 */
public class ByteBufferBackedInputStream extends InputStream
{
    protected final ByteBuffer _buffer;

    public ByteBufferBackedInputStream(ByteBuffer buf) {
        _buffer = buf;
    }

    @Override
    public int available() {
        return _buffer.remaining();
    }
    
    @Override
    public int read() throws IOException {
        return _buffer.hasRemaining() ? (_buffer.get() & 0xFF) : -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException
    {
        if (!_buffer.hasRemaining()) {
            return -1;
        }
        len = Math.min(len, _buffer.remaining());
        _buffer.get(bytes, off, len);
        return len;
    }
}