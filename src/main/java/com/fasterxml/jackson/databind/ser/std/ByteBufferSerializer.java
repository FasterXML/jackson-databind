package com.fasterxml.jackson.databind.ser.std;

import java.io.*;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;

public class ByteBufferSerializer extends StdScalarSerializer<ByteBuffer>
{
    public final static ByteBufferSerializer instance = new ByteBufferSerializer();

    public ByteBufferSerializer() { super(ByteBuffer.class); }

    @Override
    public void serialize(ByteBuffer bbuf, JsonGenerator gen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // first, simple case when wrapping an array...
        if (bbuf.hasArray()) {
            gen.writeBinary(bbuf.array(), 0, bbuf.limit());
            return;
        }
        // the other case is more complicated however. Best to handle with InputStream wrapper.
        // But should we rewind it; and/or make a copy?
        ByteBuffer copy = bbuf.asReadOnlyBuffer();
        if (copy.position() > 0) {
            copy.rewind();
        }
        InputStream in = new ByteBufferBackedInputStream(copy);
        gen.writeBinary(in, copy.remaining());
        in.close();
    }

    public class ByteBufferBackedInputStream extends InputStream
    {
        protected final ByteBuffer _buffer;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            _buffer = buf;
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
}
