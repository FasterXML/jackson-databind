package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

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
        public void writeArrayValueSeparator(JsonGenerator g) throws IOException
        {
            g.writeRaw(" , ");
        }
    }

    // for [databind#206]
    @SuppressWarnings("serial")
    static class NoCopyMapper extends ObjectMapper { }

    // [databind#2785]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "packetType")
    public interface Base2785  {
    }
    @JsonTypeName("myType")
    static class Impl2785 implements Base2785 {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Test methods, config
    /**********************************************************
     */

    public void testFactoryFeatures()
    {
        assertTrue(MAPPER.isEnabled(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES));
    }

    public void testGeneratorFeatures()
    {
        // and also for mapper
        JsonMapper mapper = new JsonMapper();
        assertTrue(mapper.isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
        assertTrue(mapper.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        assertFalse(mapper.isEnabled(JsonWriteFeature.ESCAPE_NON_ASCII));
        assertTrue(mapper.isEnabled(JsonWriteFeature.WRITE_NAN_AS_STRINGS));
        mapper = JsonMapper.builder()
                .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
                .disable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .build();
        assertFalse(mapper.isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        assertFalse(mapper.isEnabled(JsonWriteFeature.WRITE_NAN_AS_STRINGS));
    }

    public void testParserFeatures()
    {
        // and also for mapper
        ObjectMapper mapper = new ObjectMapper();

        assertTrue(mapper.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        assertTrue(mapper.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        assertFalse(mapper.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));

        mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE,
                JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        assertFalse(mapper.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        assertFalse(mapper.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
    }

    /*
    /**********************************************************
    /* Test methods, mapper.copy()
    /**********************************************************
     */

    // [databind#28]: ObjectMapper.copy()
    public void testCopy() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        assertFalse(m.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        InjectableValues inj = new InjectableValues.Std();
        m.setInjectableValues(inj);
        assertFalse(m.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
        m.enable(JsonParser.Feature.IGNORE_UNDEFINED);
        assertTrue(m.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));

        // // First: verify that handling of features is decoupled:

        ObjectMapper m2 = m.copy();
        assertFalse(m2.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        m2.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        assertTrue(m2.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertSame(inj, m2.getInjectableValues());

        // but should NOT change the original
        assertFalse(m.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        // nor vice versa:
        assertFalse(m.isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));
        assertFalse(m2.isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));
        m.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        assertTrue(m.isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));
        assertFalse(m2.isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));

        // // Also, underlying JsonFactory instances should be distinct
        assertNotSame(m.getFactory(), m2.getFactory());

        // [databind#2755]: also need to copy this:
        assertNotSame(m.getSubtypeResolver(), m2.getSubtypeResolver());

        // [databind#122]: Need to ensure mix-ins are not shared
        assertEquals(0, m.getSerializationConfig().mixInCount());
        assertEquals(0, m2.getSerializationConfig().mixInCount());
        assertEquals(0, m.getDeserializationConfig().mixInCount());
        assertEquals(0, m2.getDeserializationConfig().mixInCount());

        m.addMixIn(String.class, Integer.class);
        assertEquals(1, m.getSerializationConfig().mixInCount());
        assertEquals(0, m2.getSerializationConfig().mixInCount());
        assertEquals(1, m.getDeserializationConfig().mixInCount());
        assertEquals(0, m2.getDeserializationConfig().mixInCount());

        // [databind#913]: Ensure JsonFactory Features copied
        assertTrue(m2.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
    }

    // [databind#1580]
    public void testCopyOfConfigOverrides() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig config = m.getSerializationConfig();
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion());
        assertEquals(JsonSetter.Value.empty(), config.getDefaultSetterInfo());
        assertNull(config.getDefaultMergeable());
        VisibilityChecker<?> defaultVis = config.getDefaultVisibilityChecker();
        assertEquals(VisibilityChecker.Std.class, defaultVis.getClass());

        // change
        JsonInclude.Value customIncl = JsonInclude.Value.empty().withValueInclusion(JsonInclude.Include.NON_DEFAULT);
        m.setDefaultPropertyInclusion(customIncl);
        JsonSetter.Value customSetter = JsonSetter.Value.forValueNulls(Nulls.SKIP);
        m.setDefaultSetterInfo(customSetter);
        m.setDefaultMergeable(Boolean.TRUE);
        VisibilityChecker<?> customVis = VisibilityChecker.Std.defaultInstance()
                .withFieldVisibility(Visibility.ANY);
        m.setVisibility(customVis);
        assertSame(customVis, m.getVisibilityChecker());

        // and verify that copy retains these settings
        ObjectMapper m2 = m.copy();
        SerializationConfig config2 = m2.getSerializationConfig();
        assertSame(customIncl, config2.getDefaultPropertyInclusion());
        assertSame(customSetter, config2.getDefaultSetterInfo());
        assertEquals(Boolean.TRUE, config2.getDefaultMergeable());
        assertSame(customVis, config2.getDefaultVisibilityChecker());
    }

    // [databind#2785]
    public void testCopyOfSubtypeResolver2785() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().copy();
        objectMapper.registerSubtypes(Impl2785.class);
        Object result = objectMapper.readValue("{ \"packetType\": \"myType\" }", Base2785.class);
        assertNotNull(result);
    }

    public void testCopyWith() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        //configuring some settings to non-defaults
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true);
        JsonFactory newFactory = JsonFactory.builder()
            .configure(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING, false)
            .build();
        ObjectMapper copiedMapper = mapper.copyWith(newFactory);
        String json = "{ \"color\" : \"Black\", \"free\" : \"true\", \"pages\" : \"204.04\" }";
        JsonNode readResult = copiedMapper.readTree(json);
        // validate functionality
        assertEquals("Black", readResult.get("color").asText());
        assertEquals(true, readResult.get("free").asBoolean());
        assertEquals(204, readResult.get("pages").asInt());
        String readResultAsString = "{\n  \"color\" : \"Black\",\n  \"free\" : \"true\",\n  \"pages\" : \"204.04\"\n}";
        assertEquals(readResultAsString, mapper.writeValueAsString(readResult));

        // validate properties
        Boolean mapperConfig1 = mapper._deserializationConfig.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        Boolean copiedMapperConfig1 = copiedMapper._deserializationConfig.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        Boolean mapperConfig2 = mapper._deserializationConfig.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        Boolean copiedMapperConfig2 = copiedMapper._deserializationConfig.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        Boolean mapperConfig3 = mapper._serializationConfig.isEnabled(SerializationFeature.INDENT_OUTPUT);
        Boolean copiedMapperConfig3 = copiedMapper._serializationConfig.isEnabled(SerializationFeature.INDENT_OUTPUT);
        Boolean mapperConfig4 = mapper._serializationConfig.isEnabled(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        Boolean copiedMapperConfig4 = copiedMapper._serializationConfig.isEnabled(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        assertNotSame(mapper.getFactory(), copiedMapper.getFactory());
        assertSame(mapperConfig1, copiedMapperConfig1);
        assertSame(mapperConfig2, copiedMapperConfig2);
        assertSame(mapperConfig3, copiedMapperConfig3);
        assertSame(mapperConfig4, copiedMapperConfig4);
        assertNotSame(mapper.getFactory().isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING),
            copiedMapper.getFactory().isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING)
        );
    }

    public void testFailedCopy() throws Exception
    {
        NoCopyMapper src = new NoCopyMapper();
        try {
            src.copy();
            fail("Should not pass");
        } catch (IllegalStateException e) {
            verifyException(e, "does not override copy()");
        }
    }

    public void testAnnotationIntrospectorCopying()
    {
        ObjectMapper m = new ObjectMapper();
        m.setAnnotationIntrospector(new MyAnnotationIntrospector());
        assertEquals(MyAnnotationIntrospector.class,
                m.getDeserializationConfig().getAnnotationIntrospector().getClass());
        ObjectMapper m2 = m.copy();

        assertEquals(MyAnnotationIntrospector.class,
                m2.getDeserializationConfig().getAnnotationIntrospector().getClass());
        assertEquals(MyAnnotationIntrospector.class,
                m2.getSerializationConfig().getAnnotationIntrospector().getClass());
    }

    /*
    /**********************************************************
    /* Test methods, other
    /**********************************************************
     */

    public void testProps()
    {
        ObjectMapper m = new ObjectMapper();
        // should have default factory
        assertNotNull(m.getNodeFactory());
        JsonNodeFactory nf = new JsonNodeFactory(true);
        m.setNodeFactory(nf);
        assertNull(m.getInjectableValues());
        assertSame(nf, m.getNodeFactory());
    }

    // Test to ensure that we can check property ordering defaults...
    public void testConfigForPropertySorting() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        // sort-alphabetically is disabled by default:
        assertFalse(m.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        SerializationConfig sc = m.getSerializationConfig();
        assertFalse(sc.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertFalse(sc.shouldSortPropertiesAlphabetically());
        DeserializationConfig dc = m.getDeserializationConfig();
        assertFalse(dc.shouldSortPropertiesAlphabetically());

        // but when enabled, should be visible:
        m = jsonMapperBuilder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
        sc = m.getSerializationConfig();
        assertTrue(sc.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(sc.shouldSortPropertiesAlphabetically());
        dc = m.getDeserializationConfig();
        // and not just via SerializationConfig, but also via DeserializationConfig
        assertTrue(dc.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(dc.shouldSortPropertiesAlphabetically());
    }

    // Test to ensure that we can check forced property ordering defaults...
    public void testConfigForForcedPropertySorting() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        // sort-alphabetically is disabled by default:
        assertTrue(m.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        SerializationConfig sc = m.getSerializationConfig();
        assertTrue(sc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        DeserializationConfig dc = m.getDeserializationConfig();
        assertTrue(dc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));

        // but when enabled, should be visible:
        m = jsonMapperBuilder()
                .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
                .build();
        sc = m.getSerializationConfig();
        assertFalse(sc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        dc = m.getDeserializationConfig();
        // and not just via SerializationConfig, but also via DeserializationConfig
        assertFalse(dc.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
    }

    public void testJsonFactoryLinkage()
    {
        // first, implicit factory, giving implicit linkage
        assertSame(MAPPER, MAPPER.getFactory().getCodec());

        // and then explicit factory, which should also be implicitly linked
        JsonFactory f = new JsonFactory();
        ObjectMapper m = new ObjectMapper(f);
        assertSame(f, m.getFactory());
        assertSame(m, f.getCodec());
    }

    public void testProviderConfig() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        final String JSON = "{ \"x\" : 3 }";

        assertEquals(0, m._deserializationContext._cache.cachedDeserializersCount());
        // and then should get one constructed for:
        Bean bean = m.readValue(JSON, Bean.class);
        assertNotNull(bean);
        // Since 2.6, serializer for int also cached:
        assertEquals(2, m._deserializationContext._cache.cachedDeserializersCount());
        m._deserializationContext._cache.flushCachedDeserializers();
        assertEquals(0, m._deserializationContext._cache.cachedDeserializersCount());

        // 07-Nov-2014, tatu: As per [databind#604] verify that Maps also get cached
        m = new ObjectMapper();
        List<?> stuff = m.readValue("[ ]", List.class);
        assertNotNull(stuff);
        // may look odd, but due to "Untyped" deserializer thing, we actually have
        // 4 deserializers (int, List<?>, Map<?,?>, Object)
        assertEquals(4, m._deserializationContext._cache.cachedDeserializersCount());
    }

    // For [databind#689]
    public void testCustomDefaultPrettyPrinter() throws Exception
    {
        final ObjectMapper m = new ObjectMapper();
        final int[] input = new int[] { 1, 2 };

        // without anything else, compact:
        assertEquals("[1,2]", m.writeValueAsString(input));

        // or with default, get... defaults:
        m.enable(SerializationFeature.INDENT_OUTPUT);
        assertEquals("[ 1, 2 ]", m.writeValueAsString(input));
        assertEquals("[ 1, 2 ]", m.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        assertEquals("[ 1, 2 ]", m.writer().withDefaultPrettyPrinter().writeValueAsString(input));

        // but then with our custom thingy...
        m.setDefaultPrettyPrinter(new FooPrettyPrinter());
        assertEquals("[1 , 2]", m.writeValueAsString(input));
        assertEquals("[1 , 2]", m.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        assertEquals("[1 , 2]", m.writer().withDefaultPrettyPrinter().writeValueAsString(input));

        // and yet, can disable too
        assertEquals("[1,2]", m.writer().without(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(input));
    }

    // For [databind#703], [databind#978]
    public void testNonSerializabilityOfObject()
    {
        ObjectMapper m = new ObjectMapper();
        assertFalse(m.canSerialize(Object.class));
        // but this used to pass, incorrectly, second time around
        assertFalse(m.canSerialize(Object.class));

        // [databind#978]: Different answer if empty Beans ARE allowed
        m = new ObjectMapper();
        m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        assertTrue(m.canSerialize(Object.class));
        assertTrue(MAPPER.writer().without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .canSerialize(Object.class));
        assertFalse(MAPPER.writer().with(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .canSerialize(Object.class));
    }

    // for [databind#756]
    public void testEmptyBeanSerializability()
    {
        // with default settings, error
        assertFalse(MAPPER.writer().with(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .canSerialize(EmptyBean.class));
        // but with changes
        assertTrue(MAPPER.writer().without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .canSerialize(EmptyBean.class));
    }

    // for [databind#2749]: just to check there's no NPE; method really not useful
    public void testCanDeserialize()
    {
        assertTrue(MAPPER.canDeserialize(MAPPER.constructType(EmptyBean.class)));
        assertTrue(MAPPER.canDeserialize(MAPPER.constructType(Object.class)));
    }

    // for [databind#898]
    public void testSerializerProviderAccess() throws Exception
    {
        // ensure we have "fresh" instance, just in case
        ObjectMapper mapper = new ObjectMapper();
        JsonSerializer<?> ser = mapper.getSerializerProviderInstance()
                .findValueSerializer(Bean.class);
        assertNotNull(ser);
        assertEquals(Bean.class, ser.handledType());
    }

    // for [databind#1074]
    public void testCopyOfParserFeatures() throws Exception
    {
        // ensure we have "fresh" instance to start with
        ObjectMapper mapper = new ObjectMapper();
        assertFalse(mapper.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
        mapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
        assertTrue(mapper.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));

        ObjectMapper copy = mapper.copy();
        assertTrue(copy.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));

        // also verify there's no back-linkage
        copy.configure(JsonParser.Feature.IGNORE_UNDEFINED, false);
        assertFalse(copy.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
        assertTrue(mapper.isEnabled(JsonParser.Feature.IGNORE_UNDEFINED));
    }

    // since 2.8
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

    // since 2.8
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
        ObjectMapper objectMapper = newJsonMapper();

        final SimpleModule secondModule = new SimpleModule() {
            @Override
            public Object getTypeId() {
                return "second";
            }
        };

        final SimpleModule thirdModule = new SimpleModule() {
            @Override
            public Object getTypeId() {
                return "third";
            }
        };

        final SimpleModule firstModule = new SimpleModule() {
            @Override
            public Iterable<? extends Module> getDependencies() {
                return Arrays.asList(secondModule, thirdModule);
            }

            @Override
            public Object getTypeId() {
                return "main";
            }
        };

        objectMapper.registerModule(firstModule);

        assertEquals(
            new HashSet<>(Arrays.asList("second", "third", "main")),
            objectMapper.getRegisteredModuleIds()
        );
    }

    // since 2.12
    public void testHasExplicitTimeZone() throws Exception
    {
        final TimeZone DEFAULT_TZ = TimeZone.getTimeZone("UTC");

        // By default, not explicitly set
        assertFalse(MAPPER.getSerializationConfig().hasExplicitTimeZone());
        assertFalse(MAPPER.getDeserializationConfig().hasExplicitTimeZone());
        assertEquals(DEFAULT_TZ, MAPPER.getSerializationConfig().getTimeZone());
        assertEquals(DEFAULT_TZ, MAPPER.getDeserializationConfig().getTimeZone());
        assertFalse(MAPPER.reader().getConfig().hasExplicitTimeZone());
        assertFalse(MAPPER.writer().getConfig().hasExplicitTimeZone());

        final TimeZone TZ = TimeZone.getTimeZone("GMT+4");

        // should be able to set it via mapper
        ObjectMapper mapper = JsonMapper.builder()
                .defaultTimeZone(TZ)
                .build();
        assertSame(TZ, mapper.getSerializationConfig().getTimeZone());
        assertSame(TZ, mapper.getDeserializationConfig().getTimeZone());
        assertTrue(mapper.getSerializationConfig().hasExplicitTimeZone());
        assertTrue(mapper.getDeserializationConfig().hasExplicitTimeZone());
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
