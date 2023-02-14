package com.fasterxml.jackson.databind.module;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("serial")
public class SimpleModuleTest extends BaseMapTest
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

    /*
    /**********************************************************
    /* Unit tests; first, verifying need for custom handlers
    /**********************************************************
     */

    /**
     * Basic test to ensure we do not have functioning default
     * serializers for custom types used in tests.
     */
    public void testWithoutModule()
    {
        ObjectMapper mapper = new ObjectMapper();
        // first: serialization failure:
        try {
            mapper.writeValueAsString(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (IOException e) {
            verifyException(e, "No serializer found");
        }

        // then deserialization
        try {
            mapper.readValue("{\"str\":\"ab\",\"num\":2}", CustomBean.class);
            fail("Should have caused an exception");
        } catch (IOException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, "no creators");
        }
    }

    /*
    /**********************************************************
    /* Unit tests; simple serializers
    /**********************************************************
     */

    public void testSimpleBeanSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new CustomBeanSerializer());
        mapper.registerModule(mod);
        assertEquals(q("abcde|5"), mapper.writeValueAsString(new CustomBean("abcde", 5)));
    }

    public void testSimpleEnumSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new SimpleEnumSerializer());
        // for fun, call "multi-module" registration
        mapper.registerModules(mod);
        assertEquals(q("b"), mapper.writeValueAsString(SimpleEnum.B));
    }

    public void testSimpleInterfaceSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new BaseSerializer());
        // and another variant here too
        List<SimpleModule> mods = Arrays.asList(mod);
        mapper.registerModules(mods);
        assertEquals(q("Base:1"), mapper.writeValueAsString(new Impl1()));
        assertEquals(q("Base:2"), mapper.writeValueAsString(new Impl2()));
    }

    /*
    /**********************************************************
    /* Unit tests; simple deserializers
    /**********************************************************
     */

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

    public void testSimpleEnumDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(SimpleEnum.class, new SimpleEnumDeserializer());
        mapper.registerModule(mod);
        SimpleEnum result = mapper.readValue(q("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

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

    public void testMixIns() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.setMixInAnnotation(MixableBean.class, MixInForOrder.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        Map<String,Object> props = this.writeAndMap(mapper, new MixableBean());
        assertEquals(3, props.size());
        assertEquals(Integer.valueOf(3), props.get("c"));
        assertEquals(Integer.valueOf(1), props.get("a"));
        assertEquals(Integer.valueOf(2), props.get("b"));
    }

    public void testAccessToMapper() throws Exception
    {
        ContextVerifierModule module = new ContextVerifierModule();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    // [databind#626]
    public void testMixIns626() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // no real annotations, but nominally add ones from 'String' to 'Object', just for testing
        mapper.registerModule(new TestModule626(Object.class, String.class));
        Class<?> found = mapper.findMixInClassFor(Object.class);
        assertEquals(String.class, found);
    }

    public void testAutoDiscovery() throws Exception
    {
        List<?> mods = ObjectMapper.findModules();
        assertEquals(0, mods.size());
    }
}
