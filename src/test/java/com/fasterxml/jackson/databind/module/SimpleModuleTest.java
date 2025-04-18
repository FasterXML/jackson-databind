package com.fasterxml.jackson.databind.module;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("serial")
public class SimpleModuleTest extends DatabindTestUtil
{
    /**
     * Trivial bean that requires custom serializer and deserializer
     */
    final static class CustomBean
    {
        protected String str;
        protected int num;

        public CustomBean(String s, int i) {
            str = s;
            num = i;
        }
    }

    static enum SimpleEnum { A, B; }

    // Extend SerializerBase to get access to declared handledType
    static class CustomBeanSerializer extends StdSerializer<CustomBean>
    {
        public CustomBeanSerializer() { super(CustomBean.class); }

        @Override
        public void serialize(CustomBean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException
        {
            // We will write it as a String, with '|' as delimiter
            jgen.writeString(value.str + "|" + value.num);
        }

        @Deprecated
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return null;
        }
    }

    static class CustomBeanDeserializer extends JsonDeserializer<CustomBean>
    {
        @Override
        public CustomBean deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            String text = jp.getText();
            int ix = text.indexOf('|');
            if (ix < 0) {
                throw new IOException("Failed to parse String value of \""+text+"\"");
            }
            String str = text.substring(0, ix);
            int num = Integer.parseInt(text.substring(ix+1));
            return new CustomBean(str, num);
        }
    }

    static class SimpleEnumSerializer extends StdSerializer<SimpleEnum>
    {
        public SimpleEnumSerializer() { super(SimpleEnum.class); }

        @Override
        public void serialize(SimpleEnum value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException
        {
            jgen.writeString(value.name().toLowerCase());
        }

        @Deprecated
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return null;
        }
    }

    static class SimpleEnumDeserializer extends JsonDeserializer<SimpleEnum>
    {
        @Override
        public SimpleEnum deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            return SimpleEnum.valueOf(jp.getText().toUpperCase());
        }
    }

    interface Base {
        public String getText();
    }

    static class Impl1 implements Base {
        @Override
        public String getText() { return "1"; }
    }

    static class Impl2 extends Impl1 {
        @Override
        public String getText() { return "2"; }
    }

    static class BaseSerializer extends StdScalarSerializer<Base>
    {
        public BaseSerializer() { super(Base.class); }

        @Override
        public void serialize(Base value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString("Base:"+value.getText());
        }
    }

    static class MixableBean {
        public int a = 1;
        public int b = 2;
        public int c = 3;
    }

    @JsonPropertyOrder({"c", "a", "b"})
    static class MixInForOrder { }

    protected static class MySimpleSerializers extends SimpleSerializers { }
    protected static class MySimpleDeserializers extends SimpleDeserializers { }

    /**
     * Test module which uses custom 'serializers' and 'deserializers' container; used
     * to trigger type problems.
     */
    protected static class MySimpleModule extends SimpleModule
    {
        public MySimpleModule(String name, Version version) {
            super(name, version);
            _deserializers = new MySimpleDeserializers();
            _serializers = new MySimpleSerializers();
        }
    }

    /**
     * Test module that is different from MySimpleModule. Used to test registration
     * of multiple modules.
     */
    protected static class AnotherSimpleModule extends SimpleModule
    {
        public AnotherSimpleModule(String name, Version version) {
            super(name, version);
        }
    }

    protected static class ContextVerifierModule extends com.fasterxml.jackson.databind.Module
    {
        @Override
        public String getModuleName() { return "x"; }

        @Override
        public Version version() { return Version.unknownVersion(); }

        @Override
        public void setupModule(SetupContext context)
        {
            ObjectCodec c = context.getOwner();
            assertNotNull(c);
            assertTrue(c instanceof ObjectMapper);
            ObjectMapper m = context.getOwner();
            assertNotNull(m);
        }
    }

    static class TestModule626 extends SimpleModule {
        final Class<?> mixin, target;
        public TestModule626(Class<?> t, Class<?> m) {
            super("Test");
            target = t;
            mixin = m;
        }

        @Override
        public void setupModule(SetupContext context) {
            context.setMixInAnnotations(target, mixin);
        }
    }

    // [databind#3787]
    static class Test3787Bean {
        public String value;
    }

    static class Deserializer3787A extends JsonDeserializer<Test3787Bean> {
        @Override
        public Test3787Bean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.skipChildren(); // important to consume value
            Test3787Bean simpleTestBean = new Test3787Bean();
            simpleTestBean.value = "I am A";
            return simpleTestBean;
        }
    }

    static class Deserializer3787B extends JsonDeserializer<Test3787Bean> {
        @Override
        public Test3787Bean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.skipChildren(); // important to consume value
            Test3787Bean simpleTestBean = new Test3787Bean();
            simpleTestBean.value = "I am B";
            return simpleTestBean;
        }
    }

    static class Serializer3787A extends JsonSerializer<Test3787Bean> {
        @Override
        public void serialize(Test3787Bean value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeRaw("a-result");
        }
    }

    static class Serializer3787B extends JsonSerializer<Test3787Bean> {
        @Override
        public void serialize(Test3787Bean value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeRaw("b-result");
        }
    }

    // For [databind#5063]
    static class Module5063A extends SimpleModule {
        public Module5063A() {
            super(Version.unknownVersion());
        }
    }
    
    static class Module5063B extends SimpleModule {
        public Module5063B() {
            super(Version.unknownVersion());
        }
    }

    /*
    /**********************************************************
    /* Unit tests; first, verifying need for custom handlers
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDeserializationWithoutModule() throws Exception
    {
        final String DOC = "{\"str\":\"ab\",\"num\":2}";

        try {
            MAPPER.readValue(DOC, CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, "no creators");
        }

        // And then other variations
        try {
            MAPPER.readValue(new StringReader(DOC), CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, "no creators");
        }

        try {
            MAPPER.readValue(utf8Bytes(DOC), CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, "no creators");
        }

        try {
            MAPPER.readValue(new ByteArrayInputStream(utf8Bytes(DOC)), CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, "no creators");
        }
    }

    /**
     * Basic test to ensure we do not have functioning default
     * serializers for custom types used in tests.
     */
    @Test
    public void testSerializationWithoutModule() throws Exception
    {
        // first: serialization failure:
        try {
            MAPPER.writeValueAsString(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "No serializer found");
        }

        // and with another write call for test coverage
        try {
            MAPPER.writeValueAsBytes(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "No serializer found");
        }
    }

    /*
    /**********************************************************
    /* Unit tests; simple serializers
    /**********************************************************
     */

    @Test
    public void testSimpleBeanSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new CustomBeanSerializer());
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(mod)
                .build();
        assertEquals(q("abcde|5"), mapper.writeValueAsString(new CustomBean("abcde", 5)));
    }

    @Test
    public void testSimpleEnumSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new SimpleEnumSerializer());
        // for fun, call "multi-module" registration
        ObjectMapper mapper = JsonMapper.builder()
                .addModules(mod)
                .build();
        assertEquals(q("b"), mapper.writeValueAsString(SimpleEnum.B));
    }

    @Test
    public void testSimpleInterfaceSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new BaseSerializer());
        // and another variant here too
        List<SimpleModule> mods = Arrays.asList(mod);
        ObjectMapper mapper = JsonMapper.builder()
                .addModules(mods)
                .build();
        assertEquals(q("Base:1"), mapper.writeValueAsString(new Impl1()));
        assertEquals(q("Base:2"), mapper.writeValueAsString(new Impl2()));
    }

    /*
    /**********************************************************
    /* Unit tests; simple deserializers
    /**********************************************************
     */

    @Test
    public void testSimpleBeanDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(CustomBean.class, new CustomBeanDeserializer());
        mapper.registerModule(mod);
        CustomBean bean = mapper.readValue(q("xyz|3"), CustomBean.class);
        assertEquals("xyz", bean.str);
        assertEquals(3, bean.num);
    }

    @Test
    public void testSimpleEnumDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(SimpleEnum.class, new SimpleEnumDeserializer());
        mapper.registerModule(mod);
        SimpleEnum result = mapper.readValue(q("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

    @Test
    public void testMultipleModules() throws Exception
    {
        MySimpleModule mod1 = new MySimpleModule("test1", Version.unknownVersion());
        assertEquals("test1", mod1.getModuleName());
        // 07-Jun-2021, tatu: as per [databind#3110]:
        assertEquals("test1", mod1.getTypeId());
        SimpleModule mod2 = new SimpleModule("test2", Version.unknownVersion());
        assertEquals("test2", mod2.getModuleName());
        assertEquals("test2", mod2.getTypeId());
        mod1.addSerializer(SimpleEnum.class, new SimpleEnumSerializer());
        mod1.addDeserializer(CustomBean.class, new CustomBeanDeserializer());

        Map<Class<?>,JsonDeserializer<?>> desers = new HashMap<>();
        desers.put(SimpleEnum.class, new SimpleEnumDeserializer());
        mod2.setDeserializers(new SimpleDeserializers(desers));
        mod2.addSerializer(CustomBean.class, new CustomBeanSerializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(mod1);
        mapper.registerModule(mod2);
        assertEquals(q("b"), mapper.writeValueAsString(SimpleEnum.B));
        SimpleEnum result = mapper.readValue(q("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);

        // also let's try it with different order of registration, just in case
        mapper = new ObjectMapper();
        mapper.registerModule(mod2);
        mapper.registerModule(mod1);
        assertEquals(q("b"), mapper.writeValueAsString(SimpleEnum.B));
        result = mapper.readValue(q("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

    @Test
    public void testGetRegisteredModules()
    {
        MySimpleModule mod1 = new MySimpleModule("test1", Version.unknownVersion());
        AnotherSimpleModule mod2 = new AnotherSimpleModule("test2", Version.unknownVersion());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(mod1)
                .addModule(mod2)
                .build();

        Set<Object> registeredModuleIds = mapper.getRegisteredModuleIds();
        assertEquals(2, registeredModuleIds.size());
        assertTrue(registeredModuleIds.contains(mod1.getTypeId()));
        assertTrue(registeredModuleIds.contains(mod2.getTypeId()));

        // 01-Jul-2019, [databind#2374]: verify empty list is fine
        mapper = new ObjectMapper();
        assertEquals(0, mapper.getRegisteredModuleIds().size());

        // 07-Jun-2021, tatu [databind#3110] Casual SimpleModules ARE returned
        //    too!
        mapper = JsonMapper.builder()
                .addModule(new SimpleModule())
                .build();
        assertEquals(1, mapper.getRegisteredModuleIds().size());
        Object id = mapper.getRegisteredModuleIds().iterator().next();
        assertTrue(id instanceof String);
        if (!id.toString().startsWith("SimpleModule-")) {
            fail("SimpleModule registration id should start with 'SimpleModule-', does not: ["
                    +id+"]");
        }

        // And named ones retain their name
        mapper = JsonMapper.builder()
                .addModule(new SimpleModule("VerySpecialModule"))
                .build();
        assertEquals(Collections.singleton("VerySpecialModule"),
                mapper.getRegisteredModuleIds());
    }

    // More [databind#3110] testing
    @Test
    public void testMultipleSimpleModules()
    {
        final SimpleModule mod1 = new SimpleModule();
        final SimpleModule mod2 = new SimpleModule();
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(mod1)
                .addModule(mod2)
                .build();
        assertEquals(2, mapper.getRegisteredModuleIds().size());

        // Still avoid actual duplicates
        mapper = JsonMapper.builder()
                .addModule(mod1)
                .addModule(mod1)
                .build();
        assertEquals(1, mapper.getRegisteredModuleIds().size());

        // Same for (anonymous) sub-classes
        final SimpleModule subMod1 = new SimpleModule() { };
        final SimpleModule subMod2 = new SimpleModule() { };
        mapper = JsonMapper.builder()
                .addModule(subMod1)
                .addModule(subMod2)
                .build();
        assertEquals(2, mapper.getRegisteredModuleIds().size());

        mapper = JsonMapper.builder()
                .addModule(subMod1)
                .addModule(subMod1)
                .build();
        assertEquals(1, mapper.getRegisteredModuleIds().size());
    }

    /*
    /**********************************************************
    /* Unit tests; other
    /**********************************************************
     */

    @Test
    public void testMixIns() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.setMixInAnnotation(MixableBean.class, MixInForOrder.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        Map<String,Object> props = writeAndMap(mapper, new MixableBean());
        assertEquals(3, props.size());
        assertEquals(Integer.valueOf(3), props.get("c"));
        assertEquals(Integer.valueOf(1), props.get("a"));
        assertEquals(Integer.valueOf(2), props.get("b"));
    }

    @Test
    public void testAccessToMapper() throws Exception
    {
        ContextVerifierModule module = new ContextVerifierModule();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    // [databind#626]
    @Test
    public void testMixIns626() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // no real annotations, but nominally add ones from 'String' to 'Object', just for testing
        mapper.registerModule(new TestModule626(Object.class, String.class));
        Class<?> found = mapper.findMixInClassFor(Object.class);
        assertEquals(String.class, found);
    }

    @Test
    public void testAutoDiscovery() throws Exception
    {
        List<?> mods = ObjectMapper.findModules();
        assertEquals(0, mods.size());
    }

    @Test
    public void testAddSerializerTwiceThenOnlyLatestIsKept() throws Exception {
        SimpleModule module = new SimpleModule()
            .addSerializer(Test3787Bean.class, new Serializer3787A())
            .addSerializer(Test3787Bean.class, new Serializer3787B());
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(module)
                .build();
        assertEquals("b-result", objectMapper.writeValueAsString(new Test3787Bean()));
    }

    @Test
    public void testAddModuleWithSerializerTwiceThenOnlyLatestIsKept() throws Exception {
        SimpleModule firstModule = new SimpleModule()
            .addSerializer(Test3787Bean.class, new Serializer3787A());
        SimpleModule secondModule = new SimpleModule()
            .addSerializer(Test3787Bean.class, new Serializer3787B());
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(firstModule)
                .addModule(secondModule)
                .build();
        Test3787Bean obj = new Test3787Bean();

        String result = objectMapper.writeValueAsString(obj);

        assertEquals("b-result", result);
    }

    @Test
    public void testAddModuleWithSerializerTwiceThenOnlyLatestIsKept_reverseOrder() throws Exception {
        SimpleModule firstModule = new SimpleModule()
            .addSerializer(Test3787Bean.class, new Serializer3787A());
        SimpleModule secondModule = new SimpleModule()
            .addSerializer(Test3787Bean.class, new Serializer3787B());
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(secondModule)
                .addModule(firstModule)
                .build();

        assertEquals("a-result", objectMapper.writeValueAsString(new Test3787Bean()));
    }

    @Test
    public void testAddDeserializerTwiceThenOnlyLatestIsKept() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Test3787Bean.class, new Deserializer3787A())
            .addDeserializer(Test3787Bean.class, new Deserializer3787B());
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(module)
                .build();

        Test3787Bean result = objectMapper.readValue(
            "{\"value\" : \"I am C\"}", Test3787Bean.class);

        assertEquals("I am B", result.value);
    }

    @Test
    public void testAddModuleWithDeserializerTwiceThenOnlyLatestIsKept() throws Exception {
        SimpleModule firstModule = new SimpleModule()
            .addDeserializer(Test3787Bean.class, new Deserializer3787A());
        SimpleModule secondModule = new SimpleModule()
            .addDeserializer(Test3787Bean.class, new Deserializer3787B());
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(firstModule)
                .addModule(secondModule)
                .build();

        Test3787Bean result = objectMapper.readValue(
            "{\"value\" : \"I am C\"}", Test3787Bean.class);

        assertEquals("I am B", result.value);
    }

    @Test
    public void testAddModuleWithDeserializerTwiceThenOnlyLatestIsKept_reverseOrder() throws Exception {
        SimpleModule firstModule = new SimpleModule()
            .addDeserializer(Test3787Bean.class, new Deserializer3787A());
        SimpleModule secondModule = new SimpleModule()
            .addDeserializer(Test3787Bean.class, new Deserializer3787B());
        ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(secondModule)
            .addModule(firstModule)
            .build();

        Test3787Bean result = objectMapper.readValue(
            "{\"value\" : \"I am C\"}", Test3787Bean.class);
        
        assertEquals("I am A", result.value);
    }

    // For [databind#5063]
    @Test
    public void testDuplicateModules5063() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new Module5063A())
                .addModule(new Module5063B())
                .build();
        Set<Object> modules = mapper.getRegisteredModuleIds();
        assertEquals(2, modules.size());
    }
}
