package com.fasterxml.jackson.databind.interop;

import java.io.*;

import com.fasterxml.jackson.databind.*;

/**
 * Simple test to ensure that we can make POJOs use Jackson
 * for JDK serialization, via {@link Externalizable}
 *
 * @since 2.1
 */
public class TestExternalizable extends BaseMapTest
{
    /* Not pretty, but needed to make ObjectMapper accessible from
     * static context (alternatively could use ThreadLocal).
     */
    static class MapperHolder {
        private final ObjectMapper mapper = new ObjectMapper();
        private final static MapperHolder instance = new MapperHolder();
        public static ObjectMapper mapper() { return instance.mapper; }
    }

    /**
     * Helper class we need to adapt {@link ObjectOutput} as
     * {@link OutputStream}
     */
    final static class ExternalizableInput extends InputStream
    {
        private final ObjectInput in;

        public ExternalizableInput(ObjectInput in) {
            this.in = in;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public boolean  markSupported() {
            return false;
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            return in.read(buffer);
        }

        @Override
        public int read(byte[] buffer, int offset, int len) throws IOException {
            return in.read(buffer, offset, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return in.skip(n);
        }
    }

    /**
     * Helper class we need to adapt {@link ObjectOutput} as
     * {@link OutputStream}
     */
    final static class ExternalizableOutput extends OutputStream
    {
        private final ObjectOutput out;

        public ExternalizableOutput(ObjectOutput out) {
            this.out = out;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        @Override
        public void write(int ch) throws IOException {
            out.write(ch);
        }

        @Override
        public void write(byte[] data) throws IOException {
            out.write(data);
        }

        @Override
        public void write(byte[] data, int offset, int len) throws IOException {
            out.write(data, offset, len);
        }
    }

//    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY)
    @SuppressWarnings("resource")
    static class MyPojo implements Externalizable
    {
        public int id;
        public String name;
        public int[] values;

        public MyPojo() { } // for deserialization
        public MyPojo(int id, String name, int[] values)
        {
            this.id = id;
            this.name = name;
            this.values = values;
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException
        {
//            MapperHolder.mapper().readValue(
            MapperHolder.mapper().readerForUpdating(this).readValue(new ExternalizableInput(in));
        }

        @Override
        public void writeExternal(ObjectOutput oo) throws IOException
        {
            MapperHolder.mapper().writeValue(new ExternalizableOutput(oo), this);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null) return false;
            if (o.getClass() != getClass()) return false;

            MyPojo other = (MyPojo) o;

            if (other.id != id) return false;
            if (!other.name.equals(name)) return false;

            if (other.values.length != values.length) return false;
            for (int i = 0, end = values.length; i < end; ++i) {
                if (values[i] != other.values[i]) return false;
            }
            return true;
        }
    }

    /*
    /**********************************************************
    /* Actual tests
    /**********************************************************
     */

    // Comparison, using JDK native
    static class MyPojoNative implements Serializable
    {
        private static final long serialVersionUID = 1L;

        public int id;
        public String name;
        public int[] values;

        public MyPojoNative(int id, String name, int[] values)
        {
            this.id = id;
            this.name = name;
            this.values = values;
        }
    }

    @SuppressWarnings("unused")
    public void testSerializeAsExternalizable() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream obs = new ObjectOutputStream(bytes);
        final MyPojo input = new MyPojo(13, "Foobar", new int[] { 1, 2, 3 } );
        obs.writeObject(input);
        obs.close();
        byte[] ser = bytes.toByteArray();

        // Ok: just verify it contains stuff it should
        byte[] json = MapperHolder.mapper().writeValueAsBytes(input);

        int ix = indexOf(ser, json);
        if (ix < 0) {
            fail("Serialization ("+ser.length+") does NOT contain JSON (of "+json.length+")");
        }

        // Sanity check:
        if (false) {
            bytes = new ByteArrayOutputStream();
            obs = new ObjectOutputStream(bytes);
            MyPojoNative p = new MyPojoNative(13, "Foobar", new int[] { 1, 2, 3 } );
            obs.writeObject(p);
            obs.close();
            System.out.println("Native size: "+bytes.size()+", vs JSON: "+ser.length);
        }

        // then read back!
        ObjectInputStream ins = new ObjectInputStream(new ByteArrayInputStream(ser));
        MyPojo output = (MyPojo) ins.readObject();
        ins.close();
        assertNotNull(output);

        assertEquals(input, output);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private int indexOf(byte[] full, byte[] fragment)
    {
        final byte first = fragment[0];
        for (int i = 0, end = full.length-fragment.length; i < end; ++i) {
            if (full[i] != first) continue;
            if (matches(full, i, fragment)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matches(byte[] full, int index, byte[] fragment)
    {
        for (int i = 1, end = fragment.length; i < end; ++i) {
            if (fragment[i] != full[index+i]) {
                return false;
            }
        }
        return true;
    }
}
