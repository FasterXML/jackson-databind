package com.fasterxml.jackson.databind;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class ObjectReaderTest extends BaseMapTest
{
    static class POJO {
        public Map<String, Object> name;
    }

    static class A2297 {
        String knownField;

        @JsonCreator
        private A2297(@JsonProperty("knownField") String knownField) {
            this.knownField = knownField;
        }
    }

    private final JsonMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, simple read/write with defaults
    /**********************************************************************
     */

    public void testSimpleViaParser() throws Exception
    {
        final String JSON = "[1]";
        JsonParser p = MAPPER.createParser(JSON);
        Object ob = MAPPER.readerFor(Object.class)
                .readValue(p);
        p.close();
        assertTrue(ob instanceof List<?>);
    }

    public void testSimpleAltSources() throws Exception
    {
        final String JSON = "[1]";
        final byte[] BYTES = JSON.getBytes("UTF-8");
        Object ob = MAPPER.readerFor(Object.class)
                .readValue(BYTES);
        assertTrue(ob instanceof List<?>);

        ob = MAPPER.readerFor(Object.class)
                .readValue(BYTES, 0, BYTES.length);
        assertTrue(ob instanceof List<?>);
        assertEquals(1, ((List<?>) ob).size());

        // but also failure mode(s)
        try {
            MAPPER.readerFor(Object.class)
                .readValue(new byte[0]);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "No content to map due to end-of-input");
        }
    }

    // [databind#2693]: convenience read methods:
    public void testReaderForArrayOf() throws Exception
    {
        Object value = MAPPER.readerForArrayOf(ABC.class)
                .readValue("[ \"A\", \"C\" ]");
        assertEquals(ABC[].class, value.getClass());
        ABC[] abcs = (ABC[]) value;
        assertEquals(2, abcs.length);
        assertEquals(ABC.A, abcs[0]);
        assertEquals(ABC.C, abcs[1]);
    }

    // [databind#2693]: convenience read methods:
    public void testReaderForListOf() throws Exception
    {
        Object value = MAPPER.readerForListOf(ABC.class)
                .readValue("[ \"B\", \"C\" ]");
        assertEquals(ArrayList.class, value.getClass());
        assertEquals(Arrays.asList(ABC.B, ABC.C), value);
    }

    // [databind#2693]: convenience read methods:
    public void testReaderForMapOf() throws Exception
    {
        Object value = MAPPER.readerForMapOf(ABC.class)
                .readValue("{\"key\" : \"B\" }");
        assertEquals(LinkedHashMap.class, value.getClass());
        assertEquals(Collections.singletonMap("key", ABC.B), value);
    }

    public void testNodeHandling() throws Exception
    {
        JsonNodeFactory nodes = new JsonNodeFactory(true);
        ObjectReader r = MAPPER.reader().with(nodes);
        // but also no further changes if attempting again
        assertSame(r, r.with(nodes));
        assertTrue(r.createArrayNode().isArray());
        assertTrue(r.createObjectNode().isObject());
    }

    /*
    /**********************************************************************
    /* Test methods, some alternative JSON settings
    /**********************************************************************
     */

    public void testJsonReadFeaturesComments() throws Exception
    {
        final String JSON = "[ /* foo */ 7 ]";
        // default won't accept comments, let's change that:
        ObjectReader reader = MAPPER.readerFor(int[].class)
                .with(JsonReadFeature.ALLOW_JAVA_COMMENTS);

        int[] value = reader.readValue(JSON);
        assertNotNull(value);
        assertEquals(1, value.length);
        assertEquals(7, value[0]);

        // but also can go back
        try {
            reader.without(JsonReadFeature.ALLOW_JAVA_COMMENTS).readValue(JSON);
            fail("Should not have passed");
        } catch (DatabindException e) {
            // DatabindException since it gets wrapped
            verifyException(e, "foo");
        }
    }

    public void testJsonReadFeaturesCtrlChars() throws Exception
    {
        String FIELD = "a\tb";
        String VALUE = "\t";
        String JSON = "{ "+quote(FIELD)+" : "+quote(VALUE)+"}";
        Map<?, ?> result;

        // First: by default, unescaped control characters should not work
        try {
            result = MAPPER.readValue(JSON, Map.class);
            fail("Should not pass with defaylt settings");
        } catch (StreamReadException e) {
            verifyException(e, "Illegal unquoted character");
        }

        // But both ObjectReader:
        result = MAPPER.readerFor(Map.class)
                .with(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .readValue(JSON);
        assertEquals(1, result.size());

        // and new mapper should work
        ObjectMapper mapper2 = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .build();
        result = mapper2.readerFor(Map.class)
                .readValue(JSON);
        assertEquals(1, result.size());
    }

    /*
    /**********************************************************************
    /* Test methods, config setting verification
    /**********************************************************************
     */

    public void testFeatureSettings() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        assertFalse(r.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        assertFalse(r.isEnabled(StreamReadFeature.IGNORE_UNDEFINED));
        
        r = r.withoutFeatures(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        assertFalse(r.isEnabled(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES));
        assertFalse(r.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));
        r = r.withFeatures(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        assertTrue(r.isEnabled(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES));
        assertTrue(r.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));

        // alternative method too... can't recall why two
        assertSame(r, r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));

        // and another one
        assertSame(r, r.with(r.getConfig()));

        // and with StreamReadFeatures
        r = MAPPER.reader();
        assertFalse(r.isEnabled(StreamReadFeature.IGNORE_UNDEFINED));
        ObjectReader r2 = r.with(StreamReadFeature.IGNORE_UNDEFINED);
        assertTrue(r2.isEnabled(StreamReadFeature.IGNORE_UNDEFINED));
        ObjectReader r3 = r2.without(StreamReadFeature.IGNORE_UNDEFINED);
        assertFalse(r3.isEnabled(StreamReadFeature.IGNORE_UNDEFINED));
    }

    public void testMiscSettings() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        assertSame(MAPPER.tokenStreamFactory(), r.parserFactory());

        assertNotNull(r.typeFactory());
        assertNull(r.getInjectableValues());

        r = r.withAttributes(Collections.emptyMap());
        ContextAttributes attrs = r.getAttributes();
        assertNotNull(attrs);
        assertNull(attrs.getAttribute("abc"));
        assertSame(r, r.withoutAttribute("foo"));

        ObjectReader newR = r.forType(MAPPER.constructType(String.class));
        assertNotSame(r, newR);
        assertSame(newR, newR.forType(String.class));

        DeserializationProblemHandler probH = new DeserializationProblemHandler() {
        };
        newR = r.withHandler(probH);
        assertNotSame(r, newR);
        assertSame(newR, newR.withHandler(probH));
        r = newR;
    }

    public void testNoPrefetch() throws Exception
    {
        ObjectReader r = MAPPER.reader()
                .without(DeserializationFeature.EAGER_DESERIALIZER_FETCH);
        Number n = r.forType(Integer.class).readValue("123 ");
        assertEquals(Integer.valueOf(123), n);
    }

    public void testGetValueType() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        assertNull(r.getValueType());

        r = r.forType(String.class);
        assertEquals(MAPPER.constructType(String.class), r.getValueType());
    }

    public void testParserConfigViaReader() throws Exception
    {
        try (JsonParser p = MAPPER.reader()
                .with(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .createParser("[ ]")) {
            assertTrue(p.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        }

        /*
        try (JsonParser p = MAPPER.reader()
                .with(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .createParser("[ ]")) {
            assertTrue(p.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        }
        */
    }

    public void testGeneratorConfigViaReader() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = MAPPER.writer()
                .with(StreamWriteFeature.IGNORE_UNKNOWN)
                .createGenerator(sw)) {
            assertTrue(g.isEnabled(StreamWriteFeature.IGNORE_UNKNOWN));
        }
    }

    /*
    /**********************************************************************
    /* Test methods, JsonPointer
    /**********************************************************************
     */

    public void testNoPointerLoading() throws Exception {
        final String source = "{\"foo\":{\"bar\":{\"caller\":{\"name\":{\"value\":1234}}}}}";

        JsonNode tree = MAPPER.readTree(source);
        JsonNode node = tree.at("/foo/bar/caller");
        POJO pojo = MAPPER.treeToValue(node, POJO.class);
        assertTrue(pojo.name.containsKey("value"));
        assertEquals(1234, pojo.name.get("value"));
    }

    public void testPointerLoading() throws Exception {
        final String source = "{\"foo\":{\"bar\":{\"caller\":{\"name\":{\"value\":1234}}}}}";

        ObjectReader reader = MAPPER.readerFor(POJO.class).at("/foo/bar/caller");

        POJO pojo = reader.readValue(source);
        assertTrue(pojo.name.containsKey("value"));
        assertEquals(1234, pojo.name.get("value"));
    }

    public void testPointerLoadingAsJsonNode() throws Exception {
        final String source = "{\"foo\":{\"bar\":{\"caller\":{\"name\":{\"value\":1234}}}}}";

        ObjectReader reader = MAPPER.readerFor(POJO.class).at(JsonPointer.compile("/foo/bar/caller"));

        JsonNode node = reader.readTree(source);
        assertTrue(node.has("name"));
        JsonNode entry = node.get("name");
        assertNotNull(entry);
        assertTrue(entry.isObject());
        assertEquals(1234, entry.get("value").asInt());
    }

    public void testPointerLoadingMappingIteratorOne() throws Exception {
        final String source = "{\"foo\":{\"bar\":{\"caller\":{\"name\":{\"value\":1234}}}}}";

        ObjectReader reader = MAPPER.readerFor(POJO.class).at("/foo/bar/caller");

        MappingIterator<POJO> itr = reader.readValues(source);

        POJO pojo = itr.next();

        assertTrue(pojo.name.containsKey("value"));
        assertEquals(1234, pojo.name.get("value"));
        assertFalse(itr.hasNext());
        itr.close();
    }
    
    public void testPointerLoadingMappingIteratorMany() throws Exception {
        final String source = "{\"foo\":{\"bar\":{\"caller\":[{\"name\":{\"value\":1234}}, {\"name\":{\"value\":5678}}]}}}";

        ObjectReader reader = MAPPER.readerFor(POJO.class).at("/foo/bar/caller");

        MappingIterator<POJO> itr = reader.readValues(source);

        POJO pojo = itr.next();

        assertTrue(pojo.name.containsKey("value"));
        assertEquals(1234, pojo.name.get("value"));
        assertTrue(itr.hasNext());
        
        pojo = itr.next();

        assertNotNull(pojo.name);
        assertTrue(pojo.name.containsKey("value"));
        assertEquals(5678, pojo.name.get("value"));
        assertFalse(itr.hasNext());
        itr.close();
    }

    // [databind#1637]
    public void testPointerWithArrays() throws Exception
    {
        final String json = aposToQuotes("{\n'wrapper1': {\n" +
                "  'set1': ['one', 'two', 'three'],\n" +
                "  'set2': ['four', 'five', 'six']\n" +
                "},\n" +
                "'wrapper2': {\n" +
                "  'set1': ['one', 'two', 'three'],\n" +
                "  'set2': ['four', 'five', 'six']\n" +
                "}\n}");

        final Pojo1637 testObject = MAPPER.readerFor(Pojo1637.class)
                .at("/wrapper1")
                .readValue(json);
        assertNotNull(testObject);

        assertNotNull(testObject.set1);
        assertTrue(!testObject.set1.isEmpty());

        assertNotNull(testObject.set2);
        assertTrue(!testObject.set2.isEmpty());
    }

    public static class Pojo1637 {
        public Set<String> set1;
        public Set<String> set2;
    }    

    // [databind#2636]
    public void testCanPassResultToOverloadedMethod() throws Exception {
        final String source = "{\"foo\":{\"bar\":{\"caller\":{\"name\":{\"value\":1234}}}}}";

        ObjectReader reader = MAPPER.readerFor(POJO.class).at("/foo/bar/caller");

        process((POJO) reader.readValue(source));
    }

    void process(POJO pojo) {
        // do nothing - just used to show that the compiler can choose the correct method overloading to invoke
    }

    void process(String pojo) {
        // do nothing - just used to show that the compiler can choose the correct method overloading to invoke
        throw new Error();
    }
    
    /*
    /**********************************************************************
    /* Test methods, ObjectCodec
    /**********************************************************************
     */

    public void testTreeToValue()
    {
        ArrayNode n = MAPPER.createArrayNode();
        n.add("xyz");
        ObjectReader r = MAPPER.readerFor(String.class);
        List<?> list = r.treeToValue(n, List.class);
        assertEquals(1, list.size());

        // since 2.13:
        String[] arr = r.treeToValue(n, MAPPER.constructType(String[].class));
        assertEquals(1, arr.length);
        assertEquals("xyz", arr[0]);
    }

    /*
    /**********************************************************************
    /* Test methods, failures, other
    /**********************************************************************
     */

    public void testMissingType() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        try {
            r.readValue("1");
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "No value type configured");
        }
    }

    public void testSchema() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(String.class);
        
        // Ok to try to set `null` schema, always:
        r = r.with((FormatSchema) null);

        try {
            // but not schema that doesn't match format (no schema exists for json)
            r = r.with(new BogusSchema())
                .readValue(quote("foo"));
            
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot use FormatSchema");
        }
    }

    // For [databind#2297]
    public void testUnknownFields2297() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().addHandler(new DeserializationProblemHandler(){
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, ValueDeserializer<?> deserializer, Object beanOrClass, String propertyName) {
                p.readValueAsTree();
                return true;
            }
        }).build();
        A2297 aObject = mapper.readValue("{\"unknownField\" : 1, \"knownField\": \"test\"}", A2297.class);

        assertEquals("test", aObject.knownField);
    }

    public void test_createParser_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser =  MAPPER.reader().createParser(inputStream);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = MAPPER.reader().createParser(path.toFile());

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = MAPPER.reader().createParser(path);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Url() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonParser jsonParser = MAPPER.reader().createParser(path.toUri().toURL());

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_Reader() throws Exception
    {
        Reader reader = new StringReader("\"value\"");
        JsonParser jsonParser = MAPPER.reader().createParser(reader);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_ByteArray() throws Exception
    {
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        JsonParser jsonParser = MAPPER.reader().createParser(bytes);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_String() throws Exception
    {
        String string = "\"value\"";
        JsonParser jsonParser = MAPPER.reader().createParser(string);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_CharArray() throws Exception
    {
        char[] chars = "\"value\"".toCharArray();
        JsonParser jsonParser = MAPPER.reader().createParser(chars);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_DataInput() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        DataInput dataInput = new DataInputStream(inputStream);
        JsonParser jsonParser = MAPPER.reader().createParser(dataInput);

        assertEquals(jsonParser.nextTextValue(), "value");
    }

    public void test_createParser_failsIfArgumentIsNull() throws Exception
    {
        ObjectReader objectReader = MAPPER.reader();
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((InputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((DataInput) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((URL) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((Path) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((File) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((Reader) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((String) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((byte[]) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((byte[]) null, -1, -1));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((char[]) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.createParser((char[]) null, -1, -1));
    }

    public void test_readTree_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        JsonNode jsonNode =  MAPPER.reader().readTree(inputStream);

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_Reader() throws Exception
    {
        Reader reader = new StringReader("\"value\"");
        JsonNode jsonNode = MAPPER.reader().readTree(reader);

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_ByteArray() throws Exception
    {
        // with offset and length
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        JsonNode jsonNode1 = MAPPER.reader().readTree(bytes);

        assertEquals(jsonNode1.textValue(), "value");

        // without offset and length
        JsonNode jsonNode2 = MAPPER.reader().readTree(bytes, 0, bytes.length);

        assertEquals(jsonNode2.textValue(), "value");
    }

    public void test_readTree_String() throws Exception
    {
        String string = "\"value\"";
        JsonNode jsonNode = MAPPER.reader().readTree(string);

        assertEquals(jsonNode.textValue(), "value");
    }

    public void test_readTree_failsIfArgumentIsNull() throws Exception
    {
        ObjectReader objectReader = MAPPER.reader();
        test_method_failsIfArgumentIsNull(() -> objectReader.readTree((InputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readTree((Reader) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readTree((String) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readTree((byte[]) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readTree((byte[]) null, -1, -1));
    }

    public void test_readValue_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        String result = MAPPER.readerFor(String.class).readValue(inputStream);
        assertEquals(result, "value");
    }

    public void test_readValue_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        String result = MAPPER.readerFor(String.class).readValue(path.toFile());
        assertEquals(result, "value");
    }

    public void test_readValue_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        String result = MAPPER.readerFor(String.class).readValue(path);
        assertEquals(result, "value");
    }

    public void test_readValue_Url() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        String result = MAPPER.readerFor(String.class).readValue(path.toUri().toURL());
        assertEquals(result, "value");
    }

    public void test_readValue_Reader() throws Exception
    {
        Reader reader = new StringReader("\"value\"");
        String result = MAPPER.readerFor(String.class).readValue(reader);
        assertEquals(result, "value");
    }

    public void test_readValue_ByteArray() throws Exception
    {
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        String result = MAPPER.readerFor(String.class).readValue(bytes);
        assertEquals(result, "value");
    }

    public void test_readValue_String() throws Exception
    {
        String string = "\"value\"";
        String result = MAPPER.readerFor(String.class).readValue(string);
        assertEquals(result, "value");
    }

    public void test_readValue_DataInput() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        DataInput dataInput = new DataInputStream(inputStream);
        String result = MAPPER.readerFor(String.class).readValue(dataInput);
        assertEquals(result, "value");
    }

    public void test_readValue_JsonParser() throws Exception
    {
        String string = "\"value\"";
        JsonParser jsonParser = MAPPER.reader().createParser(string);
        String result = MAPPER.readerFor(String.class).readValue(jsonParser);
        assertEquals(result, "value");
    }

    public void test_readValue_failsIfArgumentIsNull() throws Exception
    {
        ObjectReader objectReader = MAPPER.reader();
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((InputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((DataInput) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((URL) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((Path) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((File) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((Reader) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((String) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((JsonParser) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((byte[]) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValue((byte[]) null, -1, -1));
    }

    public void test_readValues_InputStream() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(inputStream);
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_File() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(path.toFile());
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_Path() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(path);
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_Url() throws Exception
    {
        Path path = Files.createTempFile("", "");
        Files.write(path, "\"value\"".getBytes(StandardCharsets.UTF_8));
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(path.toUri().toURL());
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_Reader() throws Exception
    {
        Reader reader = new StringReader("\"value\"");
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(reader);
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_ByteArray() throws Exception
    {
        byte[] bytes = "\"value\"".getBytes(StandardCharsets.UTF_8);
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(bytes);
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_String() throws Exception
    {
        String string = "\"value\"";
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(string);
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_DataInput() throws Exception
    {
        InputStream inputStream = new ByteArrayInputStream("\"value\"".getBytes(StandardCharsets.UTF_8));
        DataInput dataInput = new DataInputStream(inputStream);
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(dataInput);
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_JsonParser() throws Exception
    {
        String string = "\"value\"";
        JsonParser jsonParser = MAPPER.reader().createParser(string);
        MappingIterator<String> result = MAPPER.readerFor(String.class).readValues(jsonParser);
        assertEquals(result.next(), "value");
        assertEquals(result.hasNext(), false);
    }

    public void test_readValues_failsIfArgumentIsNull() throws Exception
    {
        ObjectReader objectReader = MAPPER.reader();
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((InputStream) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((DataInput) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((URL) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((Path) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((File) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((Reader) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((String) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((JsonParser) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((byte[]) null));
        test_method_failsIfArgumentIsNull(() -> objectReader.readValues((byte[]) null, -1, -1));
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
