package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class TestGeneratorUsingMapper extends BaseMapTest
{
    final static class Pojo
    {
        public int getX() { return 4; }
    }

    /*
    /**********************************************************
    /* Tests for data binding integration
    /**********************************************************
     */

    public void testPojoWriting()
        throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);
        gen.writeObject(new Pojo());
        gen.close();
        // trimming needed if main-level object has leading space
        String act = sw.toString().trim();
        assertEquals("{\"x\":4}", act);
    }

    public void testPojoWritingFailing()
        throws IOException
    {
        // regular factory can't do it, without a call to setCodec()
        JsonFactory jf = new JsonFactory();
        try {
            StringWriter sw = new StringWriter();
            JsonGenerator gen = jf.createJsonGenerator(sw);
            gen.writeObject(new Pojo());
            gen.close();
            fail("Expected an exception: got sw '"+sw.toString()+"'");
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
    }

}
