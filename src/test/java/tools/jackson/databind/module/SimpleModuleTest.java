package tools.jackson.databind.module;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

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
        {
            // We will write it as a String, with '|' as delimiter
            g.writeString(value.str + "|" + value.num);
        }
    }

    static class CustomBeanDeserializer extends ValueDeserializer<CustomBean>
    {
        @Override
        public CustomBean deserialize(JsonParser p, DeserializationContext ctxt)
        {
            String text = p.getText();
            int ix = text.indexOf('|');
            if (ix < 0) {
                throw new StreamReadException(p, "Failed to parse String value of \""+text+"\"");
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
        {
            g.writeString(value.name().toLowerCase());
        }
    }

    static class SimpleEnumDeserializer extends ValueDeserializer<SimpleEnum>
    {
        @Override
        public SimpleEnum deserialize(JsonParser p, DeserializationContext ctxt)
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
        public void serialize(Base value, JsonGenerator g, SerializerProvider provider) {
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
    /**********************************************************************
    /* Unit tests; first, verifying need for custom handlers
    /**********************************************************************
     */

    /**
     * Basic test to ensure we do not have functioning default
     * serializers for custom types used in tests.
     */
    public void testWithoutModule() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // first: serialization failure:
        try {
            mapper.writeValueAsString(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (JacksonException e) {
            verifyException(e, "No serializer found");
        }

        // then deserialization
        try {
            mapper.readValue("{\"str\":\"ab\",\"num\":2}", CustomBean.class);
            fail("Should have caused an exception");
        } catch (UnrecognizedPropertyException e) {
            // 20-Sep-2017, tatu: Jackson 2.x had different exception; 3.x finds implicits too
            verifyException(e, "Unrecognized property \"str\"");

            /*
            verifyException(e, "Cannot construct");
            verifyException(e, "no creators");
            */
        }
    }

    /*
    /**********************************************************************
    /* Unit tests; simple serializers
    /**********************************************************************
     */

    public void testSimpleBeanSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new CustomBeanSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        assertEquals(q("abcde|5"), mapper.writeValueAsString(new CustomBean("abcde", 5)));
    }

    public void testSimpleEnumSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new SimpleEnumSerializer());
        // for fun, call "multi-module" registration
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        assertEquals(q("b"), mapper.writeValueAsString(SimpleEnum.B));
    }

    public void testSimpleInterfaceSerializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addSerializer(new BaseSerializer());
        // and another variant here too
        List<SimpleModule> mods = Arrays.asList(mod);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModules(mods)
                .build();
        assertEquals(q("Base:1"), mapper.writeValueAsString(new Impl1()));
        assertEquals(q("Base:2"), mapper.writeValueAsString(new Impl2()));
    }

    /*
    /**********************************************************************
    /* Unit tests; simple deserializers
    /**********************************************************************
     */
    
    public void testSimpleBeanDeserializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(CustomBean.class, new CustomBeanDeserializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        CustomBean bean = mapper.readValue(q("xyz|3"), CustomBean.class);
        assertEquals("xyz", bean.str);
        assertEquals(3, bean.num);
    }

    public void testSimpleEnumDeserializer() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(SimpleEnum.class, new SimpleEnumDeserializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        SimpleEnum result = mapper.readValue(q("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

    public void testMultipleModules() throws Exception
    {
        MySimpleModule mod1 = new MySimpleModule("test1", Version.unknownVersion());
        SimpleModule mod2 = new SimpleModule("test2", Version.unknownVersion());
        mod1.addSerializer(SimpleEnum.class, new SimpleEnumSerializer());
        mod1.addDeserializer(CustomBean.class, new CustomBeanDeserializer());

        Map<Class<?>,ValueDeserializer<?>> desers = new HashMap<>();
        desers.put(SimpleEnum.class, new SimpleEnumDeserializer());
        mod2.setDeserializers(new SimpleDeserializers(desers));
        mod2.addSerializer(CustomBean.class, new CustomBeanSerializer());

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod1)
                .addModule(mod2)
                .build();
        assertEquals(q("b"), mapper.writeValueAsString(SimpleEnum.B));
        SimpleEnum result = mapper.readValue(q("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);

        // also let's try it with different order of registration, just in case
        mapper = jsonMapperBuilder()
                .addModule(mod2)
                .addModule(mod1)
                .build();
        assertEquals(q("b"), mapper.writeValueAsString(SimpleEnum.B));
        result = mapper.readValue(q("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

    public void testGetRegisteredModules()
    {
        MySimpleModule mod1 = new MySimpleModule("test1", Version.unknownVersion());
        AnotherSimpleModule mod2 = new AnotherSimpleModule("test2", Version.unknownVersion());

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod1)
                .addModule(mod2)
                .build();

        List<JacksonModule> mods = new ArrayList<>(mapper.getRegisteredModules());
        assertEquals(2, mods.size());
        // Should retain ordering even if not mandated
        assertEquals("test1", mods.get(0).getModuleName());
        assertEquals("test2", mods.get(1).getModuleName());

        // 01-Jul-2019, [databind#2374]: verify empty list is fine
        mapper = newJsonMapper();
        assertEquals(0, mapper.getRegisteredModules().size());

        // 07-Jun-2021, tatu [databind#3110] Casual SimpleModules ARE returned
        //    too!
        mapper = JsonMapper.builder()
                .addModule(new SimpleModule())
                .build();
        assertEquals(1, mapper.getRegisteredModules().size());
        Object id = mapper.getRegisteredModules().iterator().next().getRegistrationId();
        // Id type won't be String but...
        if (!id.toString().startsWith("SimpleModule-")) {
            fail("SimpleModule registration id should start with 'SimpleModule-', does not: ["
                    +id+"]");
        }

        // And named ones retain their name
        final JacksonModule vsm = new SimpleModule("VerySpecialModule");
        mapper = JsonMapper.builder()
                .addModule(vsm)
                .build();
        Collection<JacksonModule> reg = mapper.getRegisteredModules();
        assertEquals(1, reg.size());
        assertSame(vsm, reg.iterator().next());
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
        assertEquals(2, mapper.getRegisteredModules().size());

        // Still avoid actual duplicates
        mapper = JsonMapper.builder()
                .addModule(mod1)
                .addModule(mod1)
                .build();
        assertEquals(1, mapper.getRegisteredModules().size());

        // Same for (anonymous) sub-classes
        final SimpleModule subMod1 = new SimpleModule() { };
        final SimpleModule subMod2 = new SimpleModule() { };
        mapper = JsonMapper.builder()
                .addModule(subMod1)
                .addModule(subMod2)
                .build();
        assertEquals(2, mapper.getRegisteredModules().size());

        mapper = JsonMapper.builder()
                .addModule(subMod1)
                .addModule(subMod1)
                .build();
        assertEquals(1, mapper.getRegisteredModules().size());
    }

    /*
    /**********************************************************************
    /* Unit tests; other
    /**********************************************************************
     */

    public void testMixIns() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.setMixInAnnotation(MixableBean.class, MixInForOrder.class);
        ObjectMapper mapper = jsonMapperBuilder()
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
        final JacksonModule module = new JacksonModule()
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
        ObjectMapper mapper = jsonMapperBuilder()
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
