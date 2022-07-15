package tools.jackson.databind;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import tools.jackson.core.*;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonWriteFeature;

import tools.jackson.databind.node.ObjectNode;

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

    private final ObjectMapper MAPPER = newJsonMapper();

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
                .without(JsonWriteFeature.QUOTE_PROPERTY_NAMES);
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", 1);
        assertEquals("{a:1}", writer.writeValueAsString(map));
        // but can also reconfigure
        assertEquals("{\"a\":1}", writer.with(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
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

        // Avoid using the default Locale so that a new ObjectWriter will be
        // created when calling `with(Locale)`.
        Locale newLocale = notTheDefaultLocale();

        newW = w.with(newLocale);
        assertNotSame(w, newW);
        assertSame(newW, newW.with(newLocale));
    }

    private Locale notTheDefaultLocale() {
        return Arrays.stream(Locale.getAvailableLocales())
                .filter(locale -> !locale.equals(Locale.getDefault()))
                .findAny()
                .get();
    }

    public void testMiscSettings() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        assertSame(MAPPER.tokenStreamFactory(), w.generatorFactory());
        assertFalse(w.hasPrefetchedSerializer());
        assertNotNull(w.typeFactory());

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

    public void testStreamWriteFeatures() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        assertNotSame(w, w.with(JsonWriteFeature.ESCAPE_NON_ASCII));
        assertNotSame(w, w.withFeatures(JsonWriteFeature.ESCAPE_NON_ASCII));

        assertTrue(w.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        assertNotSame(w, w.without(StreamWriteFeature.AUTO_CLOSE_TARGET));
        assertNotSame(w, w.withoutFeatures(StreamWriteFeature.AUTO_CLOSE_TARGET));
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

    public void test_createGenerator_OutputStream() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = MAPPER.writer().createGenerator(outputStream);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the stream has not been closed by close
        outputStream.write(1);
    }

    public void test_createGenerator_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        JsonGenerator jsonGenerator = MAPPER.writer().createGenerator(path.toFile(), JsonEncoding.UTF8);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_createGenerator_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        JsonGenerator jsonGenerator = MAPPER.writer().createGenerator(path, JsonEncoding.UTF8);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_createGenerator_Writer() throws Exception
    {
        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = MAPPER.writer().createGenerator(writer);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(writer.toString(), "\"value\"");

        // the writer has not been closed by close
        writer.append('1');
    }

    public void test_createGenerator_DataOutput() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);
        JsonGenerator jsonGenerator = MAPPER.writer().createGenerator(dataOutput);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the data output has not been closed by close
        dataOutput.write(1);
    }

    public void test_createGenerator_failsIfArgumentIsNull() throws Exception
    {
        ObjectWriter objectWriter = MAPPER.writer();
        test_method_failsIfArgumentIsNull(() -> objectWriter.createGenerator((OutputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectWriter.createGenerator((OutputStream) null, null));
        test_method_failsIfArgumentIsNull(() -> objectWriter.createGenerator((DataOutput) null));
        test_method_failsIfArgumentIsNull(() -> objectWriter.createGenerator((Path) null, null));
        test_method_failsIfArgumentIsNull(() -> objectWriter.createGenerator((File) null, null));
        test_method_failsIfArgumentIsNull(() -> objectWriter.createGenerator((Writer) null));
    }

    public void test_writeValue_OutputStream() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MAPPER.writer().writeValue(outputStream, "value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the stream has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValue_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        MAPPER.writer().writeValue(path.toFile(), "value");

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_writeValue_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        MAPPER.writer().writeValue(path, "value");

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_writeValue_Writer() throws Exception
    {
        Writer writer = new StringWriter();
        MAPPER.writer().writeValue(writer, "value");

        assertEquals(writer.toString(), "\"value\"");

        // the writer has not been closed by close
        writer.append('1');
    }

    public void test_writeValue_DataOutput() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);
        MAPPER.writer().writeValue(dataOutput, "value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the data output has not been closed by close
        dataOutput.write(1);
    }

    public void test_writeValue_JsonGenerator() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = MAPPER.createGenerator(outputStream);
        MAPPER.writer().writeValue(jsonGenerator, "value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the output stream has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValue_failsIfArgumentIsNull() throws Exception
    {
        ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValue((OutputStream) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValue((DataOutput) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValue((Path) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValue((File) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValue((Writer) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValue((JsonGenerator) null, null));
    }

    public void test_writeValues_OutputStream() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SequenceWriter sequenceWriter = MAPPER.writer().writeValues(outputStream);
        sequenceWriter.write("value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the stream has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValues_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        SequenceWriter sequenceWriter = MAPPER.writer().writeValues(path.toFile());
        sequenceWriter.write("value");

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_writeValues_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        SequenceWriter sequenceWriter = MAPPER.writer().writeValues(path);
        sequenceWriter.write("value");

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_writeValues_Writer() throws Exception
    {
        Writer writer = new StringWriter();
        SequenceWriter sequenceWriter = MAPPER.writer().writeValues(writer);
        sequenceWriter.write("value");

        assertEquals(writer.toString(), "\"value\"");

        // the writer has not been closed by close
        writer.append('1');
    }

    public void test_writeValues_DataOutput() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);
        SequenceWriter sequenceWriter = MAPPER.writer().writeValues(dataOutput);
        sequenceWriter.write("value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the data output has not been closed by close
        dataOutput.write(1);
    }

    public void test_writeValues_JsonGenerator() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = MAPPER.createGenerator(outputStream);
        SequenceWriter sequenceWriter = MAPPER.writer().writeValues(jsonGenerator);
        sequenceWriter.write("value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the data output has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValues_failsIfArgumentIsNull() throws Exception
    {
        ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValues((OutputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValues((DataOutput) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValues((Path) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValues((File) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValues((Writer) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValues((JsonGenerator) null));
    }

    public void test_writeValuesAsArray_OutputStream() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SequenceWriter sequenceWriter = MAPPER.writer().writeValuesAsArray(outputStream);
        sequenceWriter.write("value");
        sequenceWriter.flush();
        sequenceWriter.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "[\"value\"]");

        // the stream has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValuesAsArray_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        SequenceWriter sequenceWriter = MAPPER.writer().writeValuesAsArray(path.toFile());
        sequenceWriter.write("value");
        sequenceWriter.flush();
        sequenceWriter.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "[\"value\"]");
    }

    public void test_writeValuesAsArray_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        SequenceWriter sequenceWriter = MAPPER.writer().writeValuesAsArray(path);
        sequenceWriter.write("value");
        sequenceWriter.flush();
        sequenceWriter.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "[\"value\"]");
    }

    public void test_writeValuesAsArray_Writer() throws Exception
    {
        Writer writer = new StringWriter();
        SequenceWriter sequenceWriter = MAPPER.writer().writeValuesAsArray(writer);
        sequenceWriter.write("value");
        sequenceWriter.flush();
        sequenceWriter.close();

        assertEquals(writer.toString(), "[\"value\"]");

        // the writer has not been closed by close
        writer.append('1');
    }

    public void test_writeValuesAsArray_DataOutput() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);
        SequenceWriter sequenceWriter = MAPPER.writer().writeValuesAsArray(dataOutput);
        sequenceWriter.write("value");
        sequenceWriter.flush();
        sequenceWriter.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "[\"value\"]");

        // the data output has not been closed by close
        dataOutput.write(1);
    }

    public void test_writeValuesAsArray_JsonGenerator() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = MAPPER.createGenerator(outputStream);
        SequenceWriter sequenceWriter = MAPPER.writer().writeValuesAsArray(jsonGenerator);
        sequenceWriter.write("value");
        sequenceWriter.flush();
        sequenceWriter.close();
        jsonGenerator.flush();
        jsonGenerator.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "[\"value\"]");

        // the data output has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValuesAsArray_failsIfArgumentIsNull() throws Exception
    {
        ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValuesAsArray((OutputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValuesAsArray((DataOutput) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValuesAsArray((Path) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValuesAsArray((File) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValuesAsArray((Writer) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writer().writeValuesAsArray((JsonGenerator) null));
    }

    private static void test_method_failsIfArgumentIsNull(Runnable runnable) throws Exception
    {
        try {
            runnable.run();
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            verifyException(expected, "Argument \"");
            verifyException(expected, "\" is null");
        }
    }
}
