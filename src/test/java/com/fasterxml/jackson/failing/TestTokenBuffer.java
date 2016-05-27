package com.fasterxml.jackson.failing;

import java.io.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.util.TokenBuffer;

public class TestTokenBuffer extends BaseMapTest
{
    /*
    /**********************************************************
    /* Basic TokenBuffer tests
    /**********************************************************
     */

    public void testParentSiblingContext() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec

        // {"a":{},"b":{"c":"cval"}}
        
        buf.writeStartObject();
        buf.writeFieldName("a");
        buf.writeStartObject();
        buf.writeEndObject();

        buf.writeFieldName("b");
        buf.writeStartObject();
        buf.writeFieldName("c");
        //This assertion fails (because of 'a')
        assertEquals("b", buf.getOutputContext().getParent().getCurrentName());
        buf.writeString("cval");
        buf.writeEndObject();
        buf.writeEndObject();
        buf.close();
    }
    
}
