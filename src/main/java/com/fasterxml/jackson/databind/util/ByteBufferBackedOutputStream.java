package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Simple {@link OutputStream} implementation that appends content
 * written in given {@link ByteBuffer} instance.
 */
public class ByteBufferBackedOutputStream extends OutputStream
{
    protected final ByteBuffer _buffer;

    public ByteBufferBackedOutputStream(ByteBuffer buf) {
        this._buffer = buf;
    }

    @Override
    public void write(int b) throws IOException {
        _buffer.put((byte) b);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        _buffer.put(bytes, off, len);
    }
}
