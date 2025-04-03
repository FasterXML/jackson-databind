package tools.jackson.databind.testutil;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class MockDataInput implements DataInput
{
    private final InputStream _input;

    public MockDataInput(byte[] data) {
        _input = new ByteArrayInputStream(data);
    }

    public MockDataInput(String utf8Data) {
        _input = new ByteArrayInputStream(utf8Data.getBytes(StandardCharsets.UTF_8));
    }

    public MockDataInput(InputStream in) {
        _input = in;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return (int) _input.skip(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readByte() throws IOException {
        int ch = _input.read();
        if (ch < 0) {
            throw new EOFException("End-of-input for readByte()");
        }
        return (byte) ch;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public char readChar() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readInt() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readLong() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public float readFloat() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double readDouble() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readUTF() throws IOException {
        throw new UnsupportedOperationException();
    }
}
