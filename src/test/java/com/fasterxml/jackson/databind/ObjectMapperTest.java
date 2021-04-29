package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

import com.fasterxml.jackson.databind.cfg.DeserializationContexts;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.SimpleType;

public class ObjectMapperTest extends BaseMapTest
{
    static class Bean {
        int value = 3;
        
        public void setX(int v) { value = v; }

        protected Bean() { }
        public Bean(int v) { value = v; }
    }

    static class EmptyBean { }

    static class BeanWithoutDefaultConstructor {
        private int value;

        public BeanWithoutDefaultConstructor(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @SuppressWarnings("serial")
    static class MyAnnotationIntrospector extends JacksonAnnotationIntrospector { }

    // for [databind#689]
    @SuppressWarnings("serial")
    static class FooPrettyPrinter extends MinimalPrettyPrinter {
        public FooPrettyPrinter() {
            super(" /*foo*/ ");
        }

        @Override
        public void writeArrayValueSeparator(JsonGenerator g)
        {
            g.writeRaw(" , ");
        }
    }

    private final JsonMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Test methods, config
    /**********************************************************
     */

    public void testFeatureDefaults()
    {
        assertTrue(MAPPER.isEnabled(TokenStreamFactory.Feature.CANONICALIZE_PROPERTY_NAMES));
        assertTrue(MAPPER.isEnabled(JsonWriteFeature.QUOTE_PROPERTY_NAMES));
        assertTrue(MAPPER.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        assertTrue(MAPPER.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        assertFalse(MAPPER.isEnabled(JsonWriteFeature.ESCAPE_NON_ASCII));
        assertTrue(MAPPER.isEnabled(JsonWriteFeature.WRITE_NAN_AS_STRINGS));
        JsonMapper mapper = JsonMapper.builder()
                .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
                .disable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .build();
        assertFalse(mapper.isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        assertFalse(mapper.isEnabled(JsonWriteFeature.WRITE_NAN_AS_STRINGS));
    }

    /*
    /**********************************************************
    /* Test methods, other
    /**********************************************************
     */

    public void testProps()
    {
        // should have default factory
        assertNotNull(MAPPER.getNodeFactory());
        JsonNodeFactory nf = new JsonNodeFactory(true);
        JsonMapper m = JsonMapper.builder()
                .nodeFactory(nf)
                .build();
        assertNull(m.getInjectableValues());
        assertSame(nf, m.getNodeFactory());
    }

    // Test to ensure that we can check property ordering defaults...
    public void testConfigForPropertySorting() throws Exception
    {
        ObjectMapper m = newJsonMapper();
        
        // sort-alphabetically is disabled by default:
        assertFalse(m.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(m.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        SerializationConfig sc = m.serializationConfig();
        assertFalse(sc.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertFalse(sc.shouldSortPropertiesAlphabetically());
        assertTrue(sc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        DeserializationConfig dc = m.deserializationConfig();
        assertFalse(dc.shouldSortPropertiesAlphabetically());
        assertTrue(dc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));

        // but when enabled, should be visible:
        m = jsonMapperBuilder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
                .build();
        assertTrue(m.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertFalse(m.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        sc = m.serializationConfig();
        assertTrue(sc.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(sc.shouldSortPropertiesAlphabetically());
        assertFalse(sc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        dc = m.deserializationConfig();
        // and not just via SerializationConfig, but also via DeserializationConfig
        assertTrue(dc.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(dc.shouldSortPropertiesAlphabetically());
        assertFalse(dc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
    }

    public void testDeserializationContextCache() throws Exception   
    {
        ObjectMapper m = newJsonMapper();
        final String JSON = "{ \"x\" : 3 }";

        DeserializationContexts.DefaultImpl dc = (DeserializationContexts.DefaultImpl) m._deserializationContexts;
        DeserializerCache cache = dc.cacheForTests();

        assertEquals(0, cache.cachedDeserializersCount());
        // and then should get one constructed for:
        Bean bean = m.readValue(JSON, Bean.class);
        assertNotNull(bean);
        // Since 2.6, serializer for int also cached:
        assertEquals(2, cache.cachedDeserializersCount());
        cache.flushCachedDeserializers();
        assertEquals(0, cache.cachedDeserializersCount());

        // 07-Nov-2014, tatu: As per [databind#604] verify that Maps also get cached
        m = new ObjectMapper();
        dc = (DeserializationContexts.DefaultImpl) m._deserializationContexts;
        cache = dc.cacheForTests();

        List<?> stuff = m.readValue("[ ]", List.class);
        assertNotNull(stuff);
        // may look odd, but due to "Untyped" deserializer thing, we actually have
        // 4 deserializers (int, List<?>, Map<?,?>, Object)
        assertEquals(4, cache.cachedDeserializersCount());
    }

    // For [databind#689]
    public void testCustomDefaultPrettyPrinter() throws Exception
    {
        final int[] input = new int[] { 1, 2 };

        JsonMapper vanilla = new JsonMapper();

        // without anything else, compact:
        assertEquals("[1,2]", vanilla.writeValueAsString(input));
        assertEquals("[1,2]", vanilla.writer().writeValueAsString(input));

        // or with default, get... defaults:
        JsonMapper m = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        assertEquals("[ 1, 2 ]", m.writeValueAsString(input));
        assertEquals("[ 1, 2 ]", vanilla.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        assertEquals("[ 1, 2 ]", vanilla.writer().withDefaultPrettyPrinter().writeValueAsString(input));

        // but then with our custom thingy...
        m = JsonMapper.builder()
                .defaultPrettyPrinter(new FooPrettyPrinter())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        assertEquals("[1 , 2]", m.writeValueAsString(input));
        assertEquals("[1 , 2]", m.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        assertEquals("[1 , 2]", m.writer().withDefaultPrettyPrinter().writeValueAsString(input));

        // and yet, can disable too
        assertEquals("[1,2]", m.writer().without(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(input));
    }

    public void testDataOutputViaMapper() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("a", 1);
        final String exp = "{\"a\":1}";
        try (DataOutputStream data = new DataOutputStream(bytes)) {
            MAPPER.writeValue((DataOutput) data, input);
        }
        assertEquals(exp, bytes.toString("UTF-8"));

        // and also via ObjectWriter...
        bytes.reset();
        try (DataOutputStream data = new DataOutputStream(bytes)) {
            MAPPER.writer().writeValue((DataOutput) data, input);
        }
        assertEquals(exp, bytes.toString("UTF-8"));
    }

    @SuppressWarnings("unchecked")
    public void testDataInputViaMapper() throws Exception
    {
        byte[] src = "{\"a\":1}".getBytes("UTF-8");
        DataInput input = new DataInputStream(new ByteArrayInputStream(src));
        Map<String,Object> map = (Map<String,Object>) MAPPER.readValue(input, Map.class);
        assertEquals(Integer.valueOf(1), map.get("a"));

        input = new DataInputStream(new ByteArrayInputStream(src));
        // and via ObjectReader
        map = MAPPER.readerFor(Map.class)
                .readValue(input);
        assertEquals(Integer.valueOf(1), map.get("a"));

        input = new DataInputStream(new ByteArrayInputStream(src));
        JsonNode n = MAPPER.readerFor(Map.class)
                .readTree(input);
        assertNotNull(n);
    }

    @SuppressWarnings("serial")
    public void testRegisterDependentModules() {

        final SimpleModule secondModule = new SimpleModule() {
            @Override
            public Object getRegistrationId() {
                return "dep1";
            }
        };

        final SimpleModule thirdModule = new SimpleModule() {
            @Override
            public Object getRegistrationId() {
                return "dep2";
            }
        };

        final SimpleModule mainModule = new SimpleModule() {
            @Override
            public Iterable<? extends JacksonModule> getDependencies() {
                return Arrays.asList(secondModule, thirdModule);
            }

            @Override
            public Object getRegistrationId() {
                return "main";
            }
        };

        ObjectMapper objectMapper = jsonMapperBuilder()
                .addModule(mainModule)
                .build();

        Collection<JacksonModule> mods = objectMapper.getRegisteredModules();
        List<Object> ids = mods.stream().map(mod -> mod.getRegistrationId())
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("dep1", "dep2", "main"), ids);
    }

    // since 2.12
    public void testHasExplicitTimeZone() throws Exception
    {
        final TimeZone DEFAULT_TZ = TimeZone.getTimeZone("UTC");

        // By default, not explicitly set
        assertFalse(MAPPER.serializationConfig().hasExplicitTimeZone());
        assertFalse(MAPPER.deserializationConfig().hasExplicitTimeZone());
        assertEquals(DEFAULT_TZ, MAPPER.serializationConfig().getTimeZone());
        assertEquals(DEFAULT_TZ, MAPPER.deserializationConfig().getTimeZone());
        assertFalse(MAPPER.reader().getConfig().hasExplicitTimeZone());
        assertFalse(MAPPER.writer().getConfig().hasExplicitTimeZone());

        final TimeZone TZ = TimeZone.getTimeZone("GMT+4");

        // should be able to set it via mapper
        ObjectMapper mapper = JsonMapper.builder()
                .defaultTimeZone(TZ)
                .build();
        assertSame(TZ, mapper.serializationConfig().getTimeZone());
        assertSame(TZ, mapper.deserializationConfig().getTimeZone());
        assertTrue(mapper.serializationConfig().hasExplicitTimeZone());
        assertTrue(mapper.deserializationConfig().hasExplicitTimeZone());
        assertTrue(mapper.reader().getConfig().hasExplicitTimeZone());
        assertTrue(mapper.writer().getConfig().hasExplicitTimeZone());

        // ... as well as via ObjectReader/-Writer
        {
            final ObjectReader r = MAPPER.reader().with(TZ);
            assertTrue(r.getConfig().hasExplicitTimeZone());
            assertSame(TZ, r.getConfig().getTimeZone());
            final ObjectWriter w = MAPPER.writer().with(TZ);
            assertTrue(w.getConfig().hasExplicitTimeZone());
            assertSame(TZ, w.getConfig().getTimeZone());

            // but can also remove explicit definition
            final ObjectReader r2 = r.with((TimeZone) null);
            assertFalse(r2.getConfig().hasExplicitTimeZone());
            assertEquals(DEFAULT_TZ, r2.getConfig().getTimeZone());
            final ObjectWriter w2 = w.with((TimeZone) null);
            assertFalse(w2.getConfig().hasExplicitTimeZone());
            assertEquals(DEFAULT_TZ, w2.getConfig().getTimeZone());
        }
    }

    // Tons of test for [databind#2013] (and other similar)

    public void test_createParser_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser =  MAPPER.createParser(inputStream);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = MAPPER.createParser(path.toFile());

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = MAPPER.createParser(path);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Url() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = MAPPER.createParser(path.toUri().toURL());

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Reader() throws Exception
    {
        Reader reader = new StringReader("\"value\"");
        JsonParser jsonParser = MAPPER.createParser(reader);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_ByteArray() throws Exception
    {
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        JsonParser jsonParser = MAPPER.createParser(bytes);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_String() throws Exception
    {
        String string = "\"value\"";
        JsonParser jsonParser = MAPPER.createParser(string);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_CharArray() throws Exception
    {
        char[] chars = "\"value\"".toCharArray();
        JsonParser jsonParser = MAPPER.createParser(chars);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_DataInput() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        DataInput dataInput = new DataInputStream(inputStream);
        JsonParser jsonParser = MAPPER.createParser(dataInput);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_failsIfArgumentIsNull() throws Exception
    {
        ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((InputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((DataInput) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((URL) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((Path) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((File) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((Reader) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((String) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((byte[]) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((byte[]) null, -1, -1));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((char[]) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createParser((char[]) null, -1, -1));
    }

    public void test_createGenerator_OutputStream() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = MAPPER.createGenerator(outputStream);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the stream has not been closed by close
        outputStream.write(1);
    }

    public void test_createGenerator_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        JsonGenerator jsonGenerator = MAPPER.createGenerator(path.toFile(), JsonEncoding.UTF8);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_createGenerator_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        JsonGenerator jsonGenerator = MAPPER.createGenerator(path, JsonEncoding.UTF8);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_createGenerator_Writer() throws Exception
    {
        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = MAPPER.createGenerator(writer);

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
        JsonGenerator jsonGenerator = MAPPER.createGenerator(dataOutput);

        jsonGenerator.writeString("value");
        jsonGenerator.close();

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the data output has not been closed by close
        dataOutput.write(1);
    }

    public void test_createGenerator_failsIfArgumentIsNull() throws Exception
    {
        ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.createGenerator((OutputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createGenerator((OutputStream) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createGenerator((DataOutput) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createGenerator((Path) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createGenerator((File) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.createGenerator((Writer) null));
    }

    public void test_readTree_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonNode jsonNode =  MAPPER.readTree(inputStream);

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonNode jsonNode = MAPPER.readTree(path.toFile());

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonNode jsonNode = MAPPER.readTree(path);

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_Url() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonNode jsonNode = MAPPER.readTree(path.toUri().toURL());

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_Reader() throws Exception
    {
        Reader reader = new StringReader("\"value\"");
        JsonNode jsonNode = MAPPER.readTree(reader);

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_ByteArray() throws Exception
    {
        // with offset and length
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        JsonNode jsonNode1 = MAPPER.readTree(bytes);

        assertEquals(jsonNode1.textValue(), "value");

        // without offset and length
        JsonNode jsonNode2 = MAPPER.readTree(bytes, 0, bytes.length);

        assertEquals(jsonNode2.textValue(), "value");
    }

    public void test_readTree_String() throws Exception
    {
        String string = "\"value\"";
        JsonNode jsonNode = MAPPER.readTree(string);

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_failsIfArgumentIsNull() throws Exception
    {
        ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((InputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((URL) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((Path) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((File) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((Reader) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((String) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((byte[]) null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readTree((byte[]) null, -1, -1));
    }

    public void test_readValue_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        String result1 = MAPPER.readValue(inputStream, String.class);
        assertEquals(result1, "value");

        inputStream.reset();
        String result2 = MAPPER.readValue(inputStream, SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");

        inputStream.reset();
        String result3 = MAPPER.readValue(inputStream, new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        String result1 = MAPPER.readValue(path.toFile(), String.class);
        assertEquals(result1, "value");
        String result2 = MAPPER.readValue(path.toFile(), SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");
        String result3 = MAPPER.readValue(path.toFile(), new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        String result1 = MAPPER.readValue(path, String.class);
        assertEquals(result1, "value");
        String result2 = MAPPER.readValue(path, SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");
        String result3 = MAPPER.readValue(path, new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_Url() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        String result1 = MAPPER.readValue(path.toUri().toURL(), String.class);
        assertEquals(result1, "value");
        String result2 = MAPPER.readValue(path.toUri().toURL(), SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");
        String result3 = MAPPER.readValue(path.toUri().toURL(), new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_Reader() throws Exception
    {
        Reader reader1 = new StringReader("\"value\"");
        String result1 = MAPPER.readValue(reader1, String.class);
        assertEquals(result1, "value");

        Reader reader2 = new StringReader("\"value\"");
        String result2 = MAPPER.readValue(reader2, SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");

        Reader reader3 = new StringReader("\"value\"");
        String result3 = MAPPER.readValue(reader3, new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_ByteArray() throws Exception
    {
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        String result1 = MAPPER.readValue(bytes, String.class);
        assertEquals(result1, "value");
        String result2 = MAPPER.readValue(bytes, SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");
        String result3 = MAPPER.readValue(bytes, new TypeReference<String>() {});
        assertEquals(result3, "value");
        String result4 = MAPPER.readValue(bytes, 0, bytes.length, String.class);
        assertEquals(result4, "value");
        String result5 = MAPPER.readValue(bytes, 0, bytes.length, SimpleType.constructUnsafe(String.class));
        assertEquals(result5, "value");
        String result6 = MAPPER.readValue(bytes, 0, bytes.length, new TypeReference<String>() {});
        assertEquals(result6, "value");
    }

    public void test_readValue_String() throws Exception
    {
        String string = "\"value\"";
        String result1 = MAPPER.readValue(string, String.class);
        assertEquals(result1, "value");
        String result2 = MAPPER.readValue(string, SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");
        String result3 = MAPPER.readValue(string, new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_DataInput() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        DataInput dataInput1 = new DataInputStream(inputStream);
        String result1 = MAPPER.readValue(dataInput1, String.class);
        assertEquals(result1, "value");

        inputStream.reset();
        DataInput dataInput2 = new DataInputStream(inputStream);
        String result2 = MAPPER.readValue(dataInput2, SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");

        inputStream.reset();
        DataInput dataInput3 = new DataInputStream(inputStream);
        String result3 = MAPPER.readValue(dataInput3, new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_JsonParser() throws Exception
    {
        String string = "\"value\"";

        JsonParser jsonParser1 = MAPPER.createParser(string);
        String result1 = MAPPER.readValue(jsonParser1, String.class);
        assertEquals(result1, "value");

        JsonParser jsonParser2 = MAPPER.createParser(string);
        String result2 = MAPPER.readValue(jsonParser2, SimpleType.constructUnsafe(String.class));
        assertEquals(result2, "value");

        JsonParser jsonParser3 = MAPPER.createParser(string);
        String result3 = MAPPER.readValue(jsonParser3, new TypeReference<String>() {});
        assertEquals(result3, "value");
    }

    public void test_readValue_withoutDefaultContructor() throws Exception {
        BeanWithoutDefaultConstructor bean = new BeanWithoutDefaultConstructor(1);
        byte[] bytes = MAPPER.writeValueAsBytes(bean);

        JsonMapper jsonMapper = jsonMapperBuilder().enable(MapperFeature.CREATE_DEFAULT_CONSTRUCTOR_IF_NOT_EXISTS).build();
        BeanWithoutDefaultConstructor result = jsonMapper.readValue(bytes, BeanWithoutDefaultConstructor.class);
        assertNotNull(result);
        try {
            MAPPER.readValue(bytes, BeanWithoutDefaultConstructor.class);
        } catch (Exception e) {
            verifyException(e, "Cannot construct instance");
        }
    }

    @SuppressWarnings("rawtypes")
    public void test_readValue_failsIfArgumentIsNull() throws Exception
    {
        final ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((InputStream) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((InputStream) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((InputStream) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((DataInput) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((DataInput) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((DataInput) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((URL) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((URL) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((URL) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((Path) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((Path) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((Path) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((File) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((File) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((File) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((Reader) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((Reader) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((Reader) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((String) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((String) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((String) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((JsonParser) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((JsonParser) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((JsonParser) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((byte[]) null, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((byte[]) null, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((byte[]) null, new TypeReference<Map>() {}));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((byte[]) null, -1, -1, Map.class));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((byte[]) null, -1, -1, SimpleType.constructUnsafe(Map.class)));
        test_method_failsIfArgumentIsNull(() -> objectMapper.readValue((byte[]) null, -1, -1, new TypeReference<Map>() {}));
    }

    public void test_writeValue_OutputStream() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MAPPER.writeValue(outputStream, "value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the stream has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValue_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        MAPPER.writeValue(path.toFile(), "value");

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_writeValue_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        MAPPER.writeValue(path, "value");

        assertEquals(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), "\"value\"");
    }

    public void test_writeValue_Writer() throws Exception
    {
        Writer writer = new StringWriter();
        MAPPER.writeValue(writer, "value");

        assertEquals(writer.toString(), "\"value\"");

        // the writer has not been closed by close
        writer.append('1');
    }

    public void test_writeValue_DataOutput() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);
        MAPPER.writeValue(dataOutput, "value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the data output has not been closed by close
        dataOutput.write(1);
    }

    public void test_writeValue_JsonGenerator() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = MAPPER.createGenerator(outputStream);
        MAPPER.writeValue(jsonGenerator, "value");

        assertEquals(new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "\"value\"");

        // the output stream has not been closed by close
        outputStream.write(1);
    }

    public void test_writeValue_failsIfArgumentIsNull() throws Exception
    {
        ObjectMapper objectMapper = MAPPER;
        test_method_failsIfArgumentIsNull(() -> objectMapper.writeValue((OutputStream) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writeValue((DataOutput) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writeValue((Path) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writeValue((File) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writeValue((Writer) null, null));
        test_method_failsIfArgumentIsNull(() -> objectMapper.writeValue((JsonGenerator) null, null));
    }

    /**
     * Verifies that {@link ObjectMapper} can read from and write to {@link Path}s
     * whose {@link Path#getFileSystem()} is not the {@linkplain FileSystems#getDefault() default file system}.
     */
    public void test_readValue_writeValue_Path_nonDefaultFileSystem() throws IOException {
        Path zipFile = Files.createTempFile("", ".zip");
        // write an empty zip archive to the temp file
        try (OutputStream out = Files.newOutputStream(zipFile);
             ZipOutputStream zipped = new ZipOutputStream(out)) {
        }

        // Open the empty zip archive as a file system.
        try (FileSystem zipFs = FileSystems.newFileSystem(zipFile, (ClassLoader) null)) {
            ObjectMapper mapper = MAPPER;

            Path jsonFile = zipFs.getPath("/test.json");
            mapper.writeValue(jsonFile, "value");

            String serialized = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
            assertEquals(serialized, "\"value\"");

            String result = mapper.readValue(jsonFile, String.class);
            assertEquals(result, "value");
        }
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
