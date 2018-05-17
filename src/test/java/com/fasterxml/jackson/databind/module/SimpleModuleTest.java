package com.fasterxml.jackson.databind.module;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;

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
        public void serialize(CustomBean value, JsonGenerator g, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            // We will write it as a String, with '|' as delimiter
            g.writeString(value.str + "|" + value.num);
        }
    }
    
    static class CustomBeanDeserializer extends JsonDeserializer<CustomBean>
    {
        @Override
        public CustomBean deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            String text = p.getText();
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
        public void serialize(SimpleEnum value, JsonGenerator g, SerializerProvider provider)
            throws IOException
        {
            g.writeString(value.name().toLowerCase());
        }
    }

    static class SimpleEnumDeserializer extends JsonDeserializer<SimpleEnum>
    {
        @Override
        public SimpleEnum deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            return SimpleEnum.valueOf(p.getText().toUpperCase());
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
        public void serialize(Base value, JsonGenerator g, SerializerProvider provider) throws IOException {
            g.writeString("Base:"+value.getText());
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

    static class TestModule626 extends SimpleModule {
        final Class<?> mixin, target;
        public TestModule626(Class<?> t, Class<?> m) {
            super("Test");
            target = t;
            mixin = m;
        }

        @Override
        public void setupModule(SetupContext context) {
            context.setMixIn(target, mixin);
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
            // 20-Sep-2017, tatu: Jackson 2.x had different exception; 3.x finds implicits too
            verifyException(e, "Unrecognized field \"str\"");

            /*
            verifyException(e, "Cannot construct");
            verifyException(e, "no creators");
            */
        }
    }

    /*
    /**********************************************************
    /* Unit tests; simple serializers
    /**********************************************************
     */

    public void testSimpleBeanSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new CustomBeanSerializer());
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod)
                .build();
        assertEquals(quote("abcde|5"), mapper.writeValueAsString(new CustomBean("abcde", 5)));
    }

    public void testSimpleEnumSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new SimpleEnumSerializer());
        // for fun, call "multi-module" registration
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod)
                .build();
        assertEquals(quote("b"), mapper.writeValueAsString(SimpleEnum.B));
    }

    public void testSimpleInterfaceSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new BaseSerializer());
        // and another variant here too
        List<SimpleModule> mods = Arrays.asList(mod);
        ObjectMapper mapper = ObjectMapper.builder()
                .addModules(mods)
                .build();
        assertEquals(quote("Base:1"), mapper.writeValueAsString(new Impl1()));
        assertEquals(quote("Base:2"), mapper.writeValueAsString(new Impl2()));
    }
    
    /*
    /**********************************************************
    /* Unit tests; simple deserializers
    /**********************************************************
     */
    
    public void testSimpleBeanDeserializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(CustomBean.class, new CustomBeanDeserializer());
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod)
                .build();
        CustomBean bean = mapper.readValue(quote("xyz|3"), CustomBean.class);
        assertEquals("xyz", bean.str);
        assertEquals(3, bean.num);
    }

    public void testSimpleEnumDeserializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(SimpleEnum.class, new SimpleEnumDeserializer());
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod)
                .build();
        SimpleEnum result = mapper.readValue(quote("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

    public void testMultipleModules() throws Exception
    {
        MySimpleModule mod1 = new MySimpleModule("test1", Version.unknownVersion());
        SimpleModule mod2 = new SimpleModule("test2", Version.unknownVersion());
        mod1.addSerializer(SimpleEnum.class, new SimpleEnumSerializer());
        mod1.addDeserializer(CustomBean.class, new CustomBeanDeserializer());

        Map<Class<?>,JsonDeserializer<?>> desers = new HashMap<>();
        desers.put(SimpleEnum.class, new SimpleEnumDeserializer());
        mod2.setDeserializers(new SimpleDeserializers(desers));
        mod2.addSerializer(CustomBean.class, new CustomBeanSerializer());

        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(mod1)
                .addModule(mod2)
                .build();
        assertEquals(quote("b"), mapper.writeValueAsString(SimpleEnum.B));
        SimpleEnum result = mapper.readValue(quote("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);

        // also let's try it with different order of registration, just in case
        mapper = ObjectMapper.builder()
                .addModule(mod2)
                .addModule(mod1)
                .build();
        assertEquals(quote("b"), mapper.writeValueAsString(SimpleEnum.B));
        result = mapper.readValue(quote("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

    public void testGetRegisteredModules()
    {
        MySimpleModule mod1 = new MySimpleModule("test1", Version.unknownVersion());
        AnotherSimpleModule mod2 = new AnotherSimpleModule("test2", Version.unknownVersion());

        ObjectMapper mapper = objectMapperBuilder()
                .addModule(mod1)
                .addModule(mod2)
                .build();

        List<com.fasterxml.jackson.databind.Module> mods = new ArrayList<>(mapper.getRegisteredModules());
        assertEquals(2, mods.size());
        // Should retain ordering even if not mandated
        assertEquals("test1", mods.get(0).getModuleName());
        assertEquals("test2", mods.get(1).getModuleName());
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
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(module)
                .build();
        Map<String,Object> props = this.writeAndMap(mapper, new MixableBean());
        assertEquals(3, props.size());
        assertEquals(Integer.valueOf(3), props.get("c"));
        assertEquals(Integer.valueOf(1), props.get("a"));
        assertEquals(Integer.valueOf(2), props.get("b"));
    }

    public void testAccessToMapper() throws Exception
    {
        com.fasterxml.jackson.databind.Module module = new com.fasterxml.jackson.databind.Module()
        {
            @Override
            public String getModuleName() { return "x"; }

            @Override
            public Version version() { return Version.unknownVersion(); }

            @Override
            public void setupModule(SetupContext context)
            {
                Object c = context.getOwner();
                if (!(c instanceof MapperBuilder<?,?>)) {
                    throw new RuntimeException("Owner should be a `MapperBuilder` but is not; is: "+c);
                }
            }
        };
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(module)
                .build();
        assertNotNull(mapper);
    }

    public void testAutoDiscovery() throws Exception
    {
        List<?> mods = MapperBuilder.findModules();
        assertEquals(0, mods.size());
    }
}
