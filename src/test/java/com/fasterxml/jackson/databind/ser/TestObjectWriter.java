package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for checking features added to {@link ObjectWriter}, such
 * as adding of explicit pretty printer.
 */
public class TestObjectWriter
    extends BaseMapTest
{
    final ObjectMapper MAPPER = new ObjectMapper();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    static class PolyBase {
    }

    @JsonTypeName("A")
    static class ImplA extends PolyBase {
        public int value;
        
        public ImplA(int v) { value = v; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testPrettyPrinter() throws Exception
    {
        ObjectWriter writer = MAPPER.writer();
        HashMap<String, Integer> data = new HashMap<String,Integer>();
        data.put("a", 1);
        
        // default: no indentation
        assertEquals("{\"a\":1}", writer.writeValueAsString(data));

        // and then with standard
        writer = writer.withDefaultPrettyPrinter();

        // pretty printer uses system-specific line feeds, so we do that as well.
        String lf = System.getProperty("line.separator");
        assertEquals("{" + lf + "  \"a\" : 1" + lf + "}", writer.writeValueAsString(data));

        // and finally, again without indentation
        writer = writer.with((PrettyPrinter) null);
        assertEquals("{\"a\":1}", writer.writeValueAsString(data));
    }

    public void testPrefetch() throws Exception
    {
        ObjectWriter writer = MAPPER.writer();
        assertFalse(writer.hasPrefetchedSerializer());
        writer = writer.forType(String.class);
        assertTrue(writer.hasPrefetchedSerializer());
    }

    public void testObjectWriterFeatures() throws Exception
    {
        ObjectWriter writer = MAPPER.writer()
                .without(JsonGenerator.Feature.QUOTE_FIELD_NAMES);                
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", 1);
        assertEquals("{a:1}", writer.writeValueAsString(map));
        // but can also reconfigure
        assertEquals("{\"a\":1}", writer.with(JsonGenerator.Feature.QUOTE_FIELD_NAMES)
                .writeValueAsString(map));
    }

    public void testObjectWriterWithNode() throws Exception
    {
        ObjectNode stuff = MAPPER.createObjectNode();
        stuff.put("a", 5);
        ObjectWriter writer = MAPPER.writerFor(JsonNode.class);
        String json = writer.writeValueAsString(stuff);
        assertEquals("{\"a\":5}", json);
    }

    public void testPolymorpicWithTyping() throws Exception
    {
        ObjectWriter writer = MAPPER.writerFor(PolyBase.class);
        String json = writer.writeValueAsString(new ImplA(3));
        assertEquals(aposToQuotes("{'type':'A','value':3}"), json);
    }
}
