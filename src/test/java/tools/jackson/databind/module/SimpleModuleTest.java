package tools.jackson.databind.module;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

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
        protected Integer num;

        public CustomBean(String s, Integer i) {
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
        public void serialize(CustomBean value, JsonGenerator g, SerializationContext provider)
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
            String text = p.getString();
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
        public void serialize(SimpleEnum value, JsonGenerator g, SerializationContext provider)
        {
            g.writeString(value.name().toLowerCase());
        }
    }

    static class SimpleEnumDeserializer extends ValueDeserializer<SimpleEnum>
    {
        @Override
        public SimpleEnum deserialize(JsonParser p, DeserializationContext ctxt)
        {
            return SimpleEnum.valueOf(p.getString().toUpperCase());
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
        public void serialize(Base value, JsonGenerator g, SerializationContext provider) {
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

    // [databind#3787]
    static class Test3787Bean {
        public String value;
    }

    static class Deserializer3787A extends ValueDeserializer<Test3787Bean> {
        @Override
        public Test3787Bean deserialize(JsonParser p, DeserializationContext ctxt) {
            p.skipChildren(); // important to consume value
            Test3787Bean simpleTestBean = new Test3787Bean();
            simpleTestBean.value = "I am A";
            return simpleTestBean;
        }
    }

    static class Deserializer3787B extends ValueDeserializer<Test3787Bean> {
        @Override
        public Test3787Bean deserialize(JsonParser p, DeserializationContext ctxt)  {
            p.skipChildren(); // important to consume value
            Test3787Bean simpleTestBean = new Test3787Bean();
            simpleTestBean.value = "I am B";
            return simpleTestBean;
        }
    }

    static class Serializer3787A extends ValueSerializer<Test3787Bean> {
        @Override
        public void serialize(Test3787Bean value, JsonGenerator gen, SerializationContext serializers) {
            gen.writeRaw("a-result");
        }
    }

    static class Serializer3787B extends ValueSerializer<Test3787Bean> {
        @Override
        public void serialize(Test3787Bean value, JsonGenerator gen, SerializationContext serializers) {
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
    /**********************************************************************
    /* Unit tests; first, verifying need for custom handlers
    /**********************************************************************
     */

    @Test
    public void testDeserializationWithoutModule() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                // since 3.0 not enabled by default
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        final String DOC = "{\"str\":\"ab\",\"num\":2}";

        try {
            mapper.readValue(DOC, CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Unrecognized property");
        }

        // And then other variations
        try {
            mapper.readValue(new StringReader(DOC), CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Unrecognized property");
        }

        try {
            mapper.readValue(utf8Bytes(DOC), CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Unrecognized property");
        }

        try {
            mapper.readValue(new ByteArrayInputStream(utf8Bytes(DOC)), CustomBean.class);
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    /**
     * Basic test to ensure we do not have functioning default
     * serializers for custom types used in tests.
     */
    @Test
    public void testSerializationWithoutModule() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                // since 3.0 not enabled by default
                .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();

        // first: serialization failure:
        try {
            mapper.writeValueAsString(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "No serializer found");
        }

        // and with another write call for test coverage
        try {
            mapper.writeValueAsBytes(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (DatabindException e) {
            verifyException(e, "No serializer found");
        }
    }

    /*
    /**********************************************************************
    /* Unit tests; simple serializers
    /**********************************************************************
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
    /**********************************************************************
    /* Unit tests; simple deserializers
    /**********************************************************************
     */

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testGetRegisteredModules()
    {
        MySimpleModule mod1 = new MySimpleModule("test1", Version.unknownVersion());
        AnotherSimpleModule mod2 = new AnotherSimpleModule("test2", Version.unknownVersion());

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod1)
                .addModule(mod2)
                .build();

        List<JacksonModule> mods = _registeredModules(mapper);
        assertEquals(2, mods.size());
        // Should retain ordering even if not mandated
        assertEquals("test1", mods.get(0).getModuleName());
        assertEquals("test2", mods.get(1).getModuleName());

        // 01-Jul-2019, [databind#2374]: verify empty list is fine
        mapper = newJsonMapper();
        assertEquals(0, _registeredModules(mapper).size());

        // 07-Jun-2021, tatu [databind#3110] Casual SimpleModules ARE returned
        //    too!
        mapper = JsonMapper.builder()
                .addModule(new SimpleModule())
                .build();
        assertEquals(1, _registeredModules(mapper).size());
        Object id = mapper.registeredModules().iterator().next().getRegistrationId();
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
        Collection<JacksonModule> reg = _registeredModules(mapper);
        assertEquals(1, reg.size());
        assertSame(vsm, reg.iterator().next());
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
        assertEquals(2, _registeredModules(mapper).size());

        // Still avoid actual duplicates
        mapper = JsonMapper.builder()
                .addModule(mod1)
                .addModule(mod1)
                .build();
        assertEquals(1, _registeredModules(mapper).size());

        // Same for (anonymous) sub-classes
        final SimpleModule subMod1 = new SimpleModule() { };
        final SimpleModule subMod2 = new SimpleModule() { };
        mapper = JsonMapper.builder()
                .addModule(subMod1)
                .addModule(subMod2)
                .build();
        assertEquals(2, _registeredModules(mapper).size());

        mapper = JsonMapper.builder()
                .addModule(subMod1)
                .addModule(subMod1)
                .build();
        assertEquals(1, _registeredModules(mapper).size());
    }

    /*
    /**********************************************************************
    /* Unit tests; other
    /**********************************************************************
     */

    @Test
    public void testMixIns() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.setMixInAnnotation(MixableBean.class, MixInForOrder.class);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        Map<String,Object> props = writeAndMap(mapper, new MixableBean());
        assertEquals(3, props.size());
        assertEquals(Integer.valueOf(3), props.get("c"));
        assertEquals(Integer.valueOf(1), props.get("a"));
        assertEquals(Integer.valueOf(2), props.get("b"));
    }

    @Test
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

    @Test
    public void testAutoDiscovery() throws Exception
    {
        List<?> mods = MapperBuilder.findModules();
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
    public void testAddModuleWithDeserializerTwiceThenOnlyLatestIsKept_reverseOrder() throws Exception
    {
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
        assertEquals(2, _registeredModules(mapper).size());
    }

    private List<JacksonModule> _registeredModules(ObjectMapper mapper) {
        return new ArrayList<>(mapper.registeredModules());
    }
}
