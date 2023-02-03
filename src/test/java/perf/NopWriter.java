package perf;

import java.io.*;

public class NopWriter extends Writer
{
    protected int size = 0;

    public NopWriter() { }

    @Override
    public void write(int b) throws IOException { ++size; }

    @Override
    public void write(char[] b) throws IOException { size += b.length; }

    @Override
    public void write(char[] b, int offset, int len) throws IOException { size += len; }

    public int size() { return size; }

    @Override
    public void close() throws IOException { }

    @Override
    public void flush() throws IOException { }
}
