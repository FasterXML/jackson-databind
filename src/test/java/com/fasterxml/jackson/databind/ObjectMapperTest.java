package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.cfg.DeserializationContexts;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.*;

public class ObjectMapperTest extends BaseMapTest
{
    static class Bean {
        int value = 3;
        
        public void setX(int v) { value = v; }

        protected Bean() { }
        public Bean(int v) { value = v; }
    }

    static class EmptyBean { }

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

    private final JsonMapper MAPPER = new JsonMapper();

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
    /* Test methods, mapper.copy()
    /**********************************************************
     */

    // [databind#1580]
    public void testCopyOfConfigOverrides() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig config = m.serializationConfig();
        assertEquals(JsonSetter.Value.empty(), config.getDefaultNullHandling());
        assertNull(config.getDefaultMergeable());

        // change
        VisibilityChecker customVis = VisibilityChecker.defaultInstance()
                .withFieldVisibility(Visibility.ANY);
        m = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .changeDefaultVisibility(vc -> customVis)
                .changeDefaultNullHandling(n -> n.withValueNulls(Nulls.SKIP))
                .defaultMergeable(Boolean.TRUE)
                .build();
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
        ObjectMapper m = new ObjectMapper();
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
}
