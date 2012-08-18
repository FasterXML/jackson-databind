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
    
    static class MyPojo implements Externalizable
    {
        public int id;
        public String name;
        public int[] values;

        protected MyPojo() { } // for deserialization
        public MyPojo(int id, String name, int[] values)
        {
            this.id = id;
            this.name = name;
            this.values = values;
        }

        public void readExternal(ObjectInput in) throws IOException
        {
            
        }

        public void writeExternal(ObjectOutput oo) throws IOException
        {
            MapperHolder.mapper().writeValue(new ExternalizableOutput(oo), this);
        }
    }

    /*
    /**********************************************************
    /* Actual tests
    /**********************************************************
     */
    
    public void testSerializeAsExternalizable() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream obs = new ObjectOutputStream(bytes);
        final MyPojo input = new MyPojo(13, "Foobar", new int[] { 1, 2, 3 } );
        obs.writeObject(input);
        obs.close();
        byte[] b = bytes.toByteArray();

        // Ok: just verify it contains stuff 
        byte[] json = MapperHolder.mapper().writeValueAsBytes(input);
        
        System.out.println("Length: "+b.length+" vs "+json.length);
        System.out.println();
        System.out.println("Raw: ["+bytes.toString("ISO-8859-1")+"]");
    }
}
