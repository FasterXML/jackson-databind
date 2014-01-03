package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Simple {@link OutputStream} implementation that appends content
 * written in given {@link ByteBuffer} instance.
 */
public class ByteBufferBackedOutputStream extends OutputStream {
    protected final ByteBuffer _b;

    public ByteBufferBackedOutputStream(ByteBuffer buf) { _b = buf; }

    @Override public void write(int b) throws IOException { _b.put((byte) b); }
    @Override public void write(byte[] bytes, int off, int len) throws IOException { _b.put(bytes, off, len); }
}
