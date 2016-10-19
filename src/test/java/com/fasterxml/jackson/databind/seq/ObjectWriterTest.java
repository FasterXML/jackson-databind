package com.fasterxml.jackson.databind.seq;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for checking features added to {@link ObjectWriter}, such
 * as adding of explicit pretty printer.
 */
public class ObjectWriterTest
    extends BaseMapTest
{
    static class CloseableValue implements Closeable
    {
        public int x;

        public boolean closed;
        
        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    final ObjectMapper MAPPER = new ObjectMapper();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    static class PolyBase {
    }

    @JsonTypeName("A")
    static class ImplA extends PolyBase {
        public int value;
        
        public ImplA(int v) { value = v; }
    }

    @JsonTypeName("B")
    static class ImplB extends PolyBase {
        public int b;
        
        public ImplB(int v) { b = v; }
    }

    /*
    /**********************************************************
    /* Test methods, normal operation
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

    public void testPolymorphicWithTyping() throws Exception
    {
        ObjectWriter writer = MAPPER.writerFor(PolyBase.class);
        String json;

        json = writer.writeValueAsString(new ImplA(3));
        assertEquals(aposToQuotes("{'type':'A','value':3}"), json);
        json = writer.writeValueAsString(new ImplB(-5));
        assertEquals(aposToQuotes("{'type':'B','b':-5}"), json);
    }

    public void testCanSerialize() throws Exception
    {
        assertTrue(MAPPER.writer().canSerialize(String.class));
        assertTrue(MAPPER.writer().canSerialize(String.class, null));
    }

    public void testNoPrefetch() throws Exception
    {
        ObjectWriter w = MAPPER.writer()
                .without(SerializationFeature.EAGER_SERIALIZER_FETCH);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        w.writeValue(out, Integer.valueOf(3));
        out.close();
        assertEquals("3", out.toString("UTF-8"));
    }

    public void testWithCloseCloseable() throws Exception
    {
        ObjectWriter w = MAPPER.writer()
                .with(SerializationFeature.CLOSE_CLOSEABLE);
        assertTrue(w.isEnabled(SerializationFeature.CLOSE_CLOSEABLE));
        CloseableValue input = new CloseableValue();
        assertFalse(input.closed);
        byte[] json = w.writeValueAsBytes(input);
        assertNotNull(json);
        assertTrue(input.closed);
        input.close();
    }

    public void testSettings() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        assertFalse(w.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        assertFalse(w.isEnabled(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION));
        assertSame(MAPPER.getFactory(), w.getFactory());
        assertFalse(w.hasPrefetchedSerializer());
        assertNotNull(w.getTypeFactory());

        JsonFactory f = new JsonFactory();
        w = w.with(f);
        assertSame(f, w.getFactory());

        w = w.withView(String.class);
        w = w.withAttributes(Collections.emptyMap());
        w = w.withAttribute("a", "b");
        assertEquals("b", w.getAttributes().getAttribute("a"));
        w = w.withoutAttribute("a");
        assertNull(w.getAttributes().getAttribute("a"));
        w = w.withRootValueSeparator(new SerializedString(","));
    }

    /*
    /**********************************************************
    /* Test methods, failures
    /**********************************************************
     */

    public void testArgumentChecking() throws Exception
    {
        final ObjectWriter w = MAPPER.writer();
        try {
            w.acceptJsonFormatVisitor((JavaType) null, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "type must be provided");
        }
    }

    public void testSchema() throws Exception
    {
        try {
            MAPPER.writerFor(String.class)
                .with(new BogusSchema())
                .writeValueAsBytes("foo");
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Can not use FormatSchema");
        }
    }
}
