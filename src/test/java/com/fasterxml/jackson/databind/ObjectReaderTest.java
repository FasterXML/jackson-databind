package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectReaderTest extends BaseMapTest
{
    final JsonMapper MAPPER = JsonMapper.builder().build();

    static class POJO {
        public Map<String, Object> name;
    }

    /*
    /**********************************************************
    /* Test methods, simple read/write with defaults
    /**********************************************************
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
    /**********************************************************
    /* Test methods, some alternative JSON settings
    /**********************************************************
     */

    public void testParserFeaturesComments() throws Exception
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

    public void testParserFeaturesCtrlChars() throws Exception
    {
        String FIELD = "a\tb";
        String VALUE = "\t";
        String JSON = "{ "+q(FIELD)+" : "+q(VALUE)+"}";
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
    /**********************************************************
    /* Test methods, config setting verification
    /**********************************************************
     */

    public void testFeatureSettings() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        assertFalse(r.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        assertFalse(r.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
        
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

    public void testFeatureSettingsDeprecated() throws Exception
    {
        final ObjectReader r = MAPPER.reader();
        assertFalse(r.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));

        assertTrue(r.with(JsonParser.Feature.IGNORE_UNDEFINED)
                .isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
        assertFalse(r.without(JsonParser.Feature.IGNORE_UNDEFINED)
                .isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));

        // and then variants
        assertFalse(r.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
        ObjectReader r2 = r.withFeatures(JsonParser.Feature.IGNORE_UNDEFINED,
                JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        assertTrue(r2.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
        assertTrue(r2.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));

        ObjectReader r3 = r2.withoutFeatures(JsonParser.Feature.IGNORE_UNDEFINED,
                JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        assertFalse(r3.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
        assertFalse(r3.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
    }

    public void testMiscSettings() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        assertSame(MAPPER.getFactory(), r.getFactory());

        JsonFactory f = new JsonFactory();
        r = r.with(f);
        assertSame(f, r.getFactory());
        assertSame(r, r.with(f));

        assertNotNull(r.getTypeFactory());
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

    @SuppressWarnings("deprecation")
    public void testDeprecatedSettings() throws Exception
    {
        ObjectReader r = MAPPER.reader();

        // and deprecated variants
        ObjectReader newR = r.forType(MAPPER.constructType(String.class));
        assertSame(newR, newR.withType(String.class));
        assertSame(newR, newR.withType(MAPPER.constructType(String.class)));

        newR = newR.withRootName(PropertyName.construct("foo"));
        assertNotSame(r, newR);
        assertSame(newR, newR.withRootName(PropertyName.construct("foo")));
    }

    public void testNoPrefetch() throws Exception
    {
        ObjectReader r = MAPPER.reader()
                .without(DeserializationFeature.EAGER_DESERIALIZER_FETCH);
        Number n = r.forType(Integer.class).readValue("123 ");
        assertEquals(Integer.valueOf(123), n);
    }

    // @since 2.10
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

        try (JsonParser p = MAPPER.reader()
                .with(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .createParser("[ ]")) {
            assertTrue(p.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature()));
        }
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
    /**********************************************************
    /* Test methods, JsonPointer
    /**********************************************************
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
        assertEquals("{\"value\":1234}", node.get("name").toString());
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
        final String json = a2q("{\n'wrapper1': {\n" +
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

        process(reader.readValue(source, POJO.class));
    }

    void process(POJO pojo) {
        // do nothing - just used to show that the compiler can choose the correct method overloading to invoke
    }

    void process(String pojo) {
        // do nothing - just used to show that the compiler can choose the correct method overloading to invoke
        throw new Error();
    }
    
    /*
    /**********************************************************
    /* Test methods, ObjectCodec
    /**********************************************************
     */

    public void testTreeToValue() throws Exception
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

    public void testCodecUnsupportedWrites() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(String.class);
        JsonGenerator g = MAPPER.createGenerator(new StringWriter());
        ObjectNode n = MAPPER.createObjectNode();
        try {
            r.writeTree(g, n);
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            ;
        }
        try {
            r.writeValue(g, "Foo");
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            ;
        }
        g.close();
    }

    /*
    /**********************************************************
    /* Test methods, failures, other
    /**********************************************************
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
                .readValue(q("foo"));
            
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot use FormatSchema");
        }
    }

    // For [databind#2297]
    public void testUnknownFields() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().addHandler(new DeserializationProblemHandler(){
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
                p.readValueAsTree();
                return true;
            }
        }).build();
        A aObject = mapper.readValue("{\"unknownField\" : 1, \"knownField\": \"test\"}", A.class);

        assertEquals("test", aObject.knownField);
    }

    private static class A {
        String knownField;

        @JsonCreator
        private A(@JsonProperty("knownField") String knownField) {
            this.knownField = knownField;
        }
    }

    @SuppressWarnings("unchecked")
    public void testReaderForFixedElementTypes() throws IOException {
        List<JavaType> elementTypeList = new ArrayList<>();
        elementTypeList.add(MAPPER.getTypeFactory().constructType(Integer.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructType(BigDecimal.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructType(Long.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructType(String.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructParametricType(Generic.class, BigDecimal.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructCollectionType(List.class, Shape.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructType(TestBean.class));
        String content = "[]";
        JsonParser p = MAPPER.createParser(content);
        ObjectReader listObjectReader = MAPPER.readerFor(List.class);
        List<Object> objectList = listObjectReader.readValue(p, elementTypeList);
        p.close();
        assertEquals(0, objectList.size());
        content = "[null,null,null,null,null]";
        p = MAPPER.createParser(content);
        listObjectReader = MAPPER.readerFor(ArrayList.class);
        objectList = listObjectReader.readValue(p, elementTypeList);
        p.close();
        assertEquals(5, objectList.size());
        content = "[1,2,3,null,{\"s\":1.23},"
                + "[{\"@class\":\"" + getClass().getCanonicalName() + "$Circle\","
                + "\"radius\":4},"
                + "{\"@class\":\"" + getClass().getCanonicalName() + "$Rectangle\","
                + "\"width\":5,\"height\":6}],"
                + "{\"map1\":{\"a\":1},\"map2\":{\"a\":1}}]";
        p = MAPPER.createParser(content);
        objectList = listObjectReader.readValue(p, elementTypeList);
        p.close();
        assertEquals(new Integer(1), objectList.get(0));
        assertEquals(new BigDecimal("2"), objectList.get(1));
        assertEquals(new Long(3), objectList.get(2));
        assertEquals(null, objectList.get(3));
        assertEquals(new BigDecimal("1.23"), ((Generic<BigDecimal>) objectList.get(4)).getT());
        assertEquals(4, ((Circle) ((List<?>) objectList.get(5)).get(0)).getRadius());
        assertEquals(5, ((Rectangle) ((List<?>) objectList.get(5)).get(1)).getWidth());
        assertEquals(6, ((Rectangle) ((List<?>) objectList.get(5)).get(1)).getHeight());
        assertEquals(100, ((TestBean) objectList.get(6)).getMap1().get("a").intValue());
        assertEquals(1, ((TestBean) objectList.get(6)).getMap2().get("a").intValue());
        elementTypeList.clear();
        elementTypeList.add(MAPPER.getTypeFactory().constructType(Integer.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructType(BigDecimal.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructType(Long.class));
        elementTypeList.add(MAPPER.getTypeFactory().constructType(String.class));
        content = "[1,2,3,null]";
        p = MAPPER.createParser(content);
        objectList = listObjectReader.readValue(p, elementTypeList);
        p.close();
        assertEquals(new Integer(1), objectList.get(0));
        assertEquals(new BigDecimal("2"), objectList.get(1));
        assertEquals(new Long(3), objectList.get(2));
    }

    @JsonDeserialize(using = Generic.CustomerDeserializer.class)
    private static class Generic<T> {
        private T t;

        public T getT() {
            return t;
        }

        public void setT(T t) {
            this.t = t;
        }

        private static class CustomerDeserializer extends JsonDeserializer<Generic<?>>
                implements ContextualDeserializer {

            private Class<?> tClazz;

            @SuppressWarnings("unused")
            public CustomerDeserializer() {
            }

            public CustomerDeserializer(Class<?> tClazz) {
                this.tClazz = tClazz;
            }

            @Override
            public Generic<?> deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException, JacksonException {
                JsonNode node = ctxt.readTree(p).findValue("s");
                Generic<Object> g = new Generic<>();
                Object t = ((ObjectMapper) p.getCodec()).convertValue(node, this.tClazz);
                g.setT(t);
                return g;
            }

            @Override
            public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
                    throws JsonMappingException {
                JavaType currentType = null;
                if (property == null) {
                    // current type is root type.
                    currentType = ctxt.getContextualType();
                } else {
                    // current type is wrapped in other type.
                    currentType = property.getType();
                }
                Class<?> tClazz = currentType.getBindings().getBoundType(0).getRawClass();
                return new CustomerDeserializer(tClazz);
            }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private static class Shape {

    }

    private static class Circle extends Shape {
        private int radius;

        public int getRadius() {
            return radius;
        }

        @SuppressWarnings("unused")
        public void setRadius(int radius) {
            this.radius = radius;
        }
    }

    private static class Rectangle extends Shape {
        private int width;
        private int height;

        public int getWidth() {
            return width;
        }

        @SuppressWarnings("unused")
        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        @SuppressWarnings("unused")
        public void setHeight(int height) {
            this.height = height;
        }
    }

    @SuppressWarnings("unused")
    private static class TestBean {

        @JsonProperty("map1")
        @JsonDeserialize(contentUsing = TestBeanCustomDeserializer.class)
        Map<String, Integer> map1;

        @JsonProperty("map2")
        Map<String, Integer> map2;

        public Map<String, Integer> getMap1() {
            return map1;
        }

        public void setMap1(Map<String, Integer> map1) {
            this.map1 = map1;
        }

        public Map<String, Integer> getMap2() {
            return map2;
        }

        public void setMap2(Map<String, Integer> map2) {
            this.map2 = map2;
        }
    }

    private static class TestBeanCustomDeserializer extends StdDeserializer<Integer> {
        private static final long serialVersionUID = 1L;

        public TestBeanCustomDeserializer() {
            super(Integer.class);
        }

        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            Integer value = p.readValueAs(Integer.class);
            return value * 100;
        }
    }
}
