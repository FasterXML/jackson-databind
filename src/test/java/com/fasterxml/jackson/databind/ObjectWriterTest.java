package com.fasterxml.jackson.databind;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
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
                .without(JsonWriteFeature.QUOTE_FIELD_NAMES);
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", 1);
        assertEquals("{a:1}", writer.writeValueAsString(map));
        // but can also reconfigure
        assertEquals("{\"a\":1}", writer.with(JsonWriteFeature.QUOTE_FIELD_NAMES)
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
        assertEquals(a2q("{'type':'A','value':3}"), json);
        json = writer.writeValueAsString(new ImplB(-5));
        assertEquals(a2q("{'type':'B','b':-5}"), json);
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

        // and via explicitly passed generator
        JsonGenerator g = MAPPER.createGenerator(new StringWriter());
        input = new CloseableValue();
        assertFalse(input.closed);
        w.writeValue(g, input);
        assertTrue(input.closed);
        g.close();
        input.close();
    }

    public void testViewSettings() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        ObjectWriter newW = w.withView(String.class);
        assertNotSame(w, newW);
        assertSame(newW, newW.withView(String.class));

        newW = w.with(Locale.CANADA);
        assertNotSame(w, newW);
        assertSame(newW, newW.with(Locale.CANADA));
    }

    public void testMiscSettings() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        assertSame(MAPPER.getFactory(), w.getFactory());
        assertFalse(w.hasPrefetchedSerializer());
        assertNotNull(w.getTypeFactory());

        JsonFactory f = new JsonFactory();
        w = w.with(f);
        assertSame(f, w.getFactory());
        ObjectWriter newW = w.with(Base64Variants.MODIFIED_FOR_URL);
        assertNotSame(w, newW);
        assertSame(newW, newW.with(Base64Variants.MODIFIED_FOR_URL));

        w = w.withAttributes(Collections.emptyMap());
        w = w.withAttribute("a", "b");
        assertEquals("b", w.getAttributes().getAttribute("a"));
        w = w.withoutAttribute("a");
        assertNull(w.getAttributes().getAttribute("a"));

        FormatSchema schema = new BogusSchema();
        try {
            newW = w.with(schema);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot use FormatSchema");
        }
    }

    public void testRootValueSettings() throws Exception
    {
        ObjectWriter w = MAPPER.writer();

        // First, root name:
        ObjectWriter newW = w.withRootName("foo");
        assertNotSame(w, newW);
        assertSame(newW, newW.withRootName(PropertyName.construct("foo")));
        w = newW;
        newW = w.withRootName((String) null);
        assertNotSame(w, newW);
        assertSame(newW, newW.withRootName((PropertyName) null));

        // Then root value separator

        w = w.withRootValueSeparator(new SerializedString(","));
        assertSame(w, w.withRootValueSeparator(new SerializedString(",")));
        assertSame(w, w.withRootValueSeparator(","));

         newW = w.withRootValueSeparator("/");
        assertNotSame(w, newW);
        assertSame(newW, newW.withRootValueSeparator("/"));

        newW = w.withRootValueSeparator((String) null);
        assertNotSame(w, newW);

        newW = w.withRootValueSeparator((SerializableString) null);
        assertNotSame(w, newW);
    }

    public void testFeatureSettings() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        assertFalse(w.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        assertFalse(w.isEnabled(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION));
        assertFalse(w.isEnabled(StreamWriteFeature.STRICT_DUPLICATE_DETECTION));

        ObjectWriter newW = w.with(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS,
                SerializationFeature.INDENT_OUTPUT);
        assertNotSame(w, newW);
        assertTrue(newW.isEnabled(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS));
        assertTrue(newW.isEnabled(SerializationFeature.INDENT_OUTPUT));
        assertSame(newW, newW.with(SerializationFeature.INDENT_OUTPUT));
        assertSame(newW, newW.withFeatures(SerializationFeature.INDENT_OUTPUT));

        newW = w.withFeatures(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS,
                SerializationFeature.INDENT_OUTPUT);
        assertNotSame(w, newW);

        newW = w.without(SerializationFeature.FAIL_ON_EMPTY_BEANS,
                SerializationFeature.EAGER_SERIALIZER_FETCH);
        assertNotSame(w, newW);
        assertFalse(newW.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        assertFalse(newW.isEnabled(SerializationFeature.EAGER_SERIALIZER_FETCH));
        assertSame(newW, newW.without(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        assertSame(newW, newW.withoutFeatures(SerializationFeature.FAIL_ON_EMPTY_BEANS));

        assertNotSame(w, w.withoutFeatures(SerializationFeature.FAIL_ON_EMPTY_BEANS,
                SerializationFeature.EAGER_SERIALIZER_FETCH));
    }

    public void testGeneratorFeatures() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        assertNotSame(w, w.with(JsonWriteFeature.ESCAPE_NON_ASCII));
        assertNotSame(w, w.withFeatures(JsonWriteFeature.ESCAPE_NON_ASCII));

        assertTrue(w.isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
        assertNotSame(w, w.without(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
        assertNotSame(w, w.withoutFeatures(JsonGenerator.Feature.AUTO_CLOSE_TARGET));

        assertFalse(w.isEnabled(StreamWriteFeature.STRICT_DUPLICATE_DETECTION));
        assertNotSame(w, w.with(StreamWriteFeature.STRICT_DUPLICATE_DETECTION));
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
            verifyException(e, "argument \"type\" is null");
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
            verifyException(e, "Cannot use FormatSchema");
        }
    }
}
