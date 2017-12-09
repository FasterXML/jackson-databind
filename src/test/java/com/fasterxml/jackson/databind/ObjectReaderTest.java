package com.fasterxml.jackson.databind;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class ObjectReaderTest extends BaseMapTest
{
    final ObjectMapper MAPPER = new ObjectMapper();

    static class POJO {
        public Map<String, Object> name;
    }

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
    }

    public void testParserFeatures() throws Exception
    {
        final String JSON = "[ /* foo */ 7 ]";
        // default won't accept comments, let's change that:
        ObjectReader reader = MAPPER.readerFor(int[].class)
                .with(JsonParser.Feature.ALLOW_COMMENTS);

        int[] value = reader.readValue(JSON);
        assertNotNull(value);
        assertEquals(1, value.length);
        assertEquals(7, value[0]);

        // but also can go back
        try {
            reader.without(JsonParser.Feature.ALLOW_COMMENTS).readValue(JSON);
            fail("Should not have passed");
        } catch (JsonProcessingException e) {
            verifyException(e, "foo");
        }
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

    public void testFeatureSettings() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        assertFalse(r.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        assertFalse(r.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        
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
        } catch (JsonMappingException e) {
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
}
