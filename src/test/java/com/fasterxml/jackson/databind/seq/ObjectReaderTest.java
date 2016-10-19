package com.fasterxml.jackson.databind.seq;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
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
        JsonParser p = MAPPER.getFactory().createParser(JSON);
        Object ob = MAPPER.readerFor(Object.class)
                .readValue(p);
        p.close();
        assertTrue(ob instanceof List<?>);
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

    public void testNodeHandling() throws Exception
    {
        JsonNodeFactory nodes = new JsonNodeFactory(true);
        ObjectReader r = MAPPER.reader().with(nodes);
        assertTrue(r.createArrayNode().isArray());
        assertTrue(r.createObjectNode().isObject());
    }

    public void testSettings() throws Exception
    {
        ObjectReader r = MAPPER.reader();
        assertSame(MAPPER.getFactory(), r.getFactory());

        JsonFactory f = new JsonFactory();
        r = r.with(f);
        assertSame(f, r.getFactory());

        assertNotNull(r.getTypeFactory());
        assertNull(r.getInjectableValues());

        r = r.withAttributes(Collections.emptyMap());
        ContextAttributes attrs = r.getAttributes();
        assertNotNull(attrs);
        assertNull(attrs.getAttribute("abc"));

        r = r.forType(MAPPER.constructType(String.class));
        r = r.withRootName(PropertyName.construct("foo"));

        r = r.withoutFeatures(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        assertFalse(r.isEnabled(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES));
        assertFalse(r.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));
        r = r.withFeatures(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        assertTrue(r.isEnabled(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES));
        assertTrue(r.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));
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
    /* Test methods, failures
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
            verifyException(e, "Can not use FormatSchema");
        }
    }
}
