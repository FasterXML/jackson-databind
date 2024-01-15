package tools.jackson.databind.deser.bean;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

@SuppressWarnings("serial")
public class TestBeanDeserializer
{
    static abstract class Abstract {
        public int x;
    }

    static class Bean {
        public String b = "b";
        public String a = "a";

        public Bean() { }
        public Bean(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    static class ModuleImpl extends SimpleModule
    {
        protected ValueDeserializerModifier modifier;

        public ModuleImpl(ValueDeserializerModifier modifier)
        {
            super("test", Version.unknownVersion());
            this.modifier = modifier;
        }

        @Override
        public void setupModule(SetupContext context)
        {
            super.setupModule(context);
            if (modifier != null) {
                context.addDeserializerModifier(modifier);
            }
        }
    }

    static class RemovingModifier extends ValueDeserializerModifier
    {
        private final String _removedProperty;

        public RemovingModifier(String remove) { _removedProperty = remove; }

        @Override
        public BeanDeserializerBuilder updateBuilder(DeserializationConfig config,
                BeanDescription beanDesc, BeanDeserializerBuilder builder) {
            builder.addIgnorable(_removedProperty);
            return builder;
        }
    }

    static class ReplacingModifier extends ValueDeserializerModifier
    {
        private final ValueDeserializer<?> _deserializer;

        public ReplacingModifier(ValueDeserializer<?> s) { _deserializer = s; }

        @Override
        public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
                BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
            return _deserializer;
        }
    }

    static class BogusBeanDeserializer extends ValueDeserializer<Object>
    {
        private final String a, b;

        public BogusBeanDeserializer(String a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        {
            return new Bean(a, b);
        }
    }
    static class Issue476Bean {
        public Issue476Type value1, value2;
    }
    static class Issue476Type {
        public String name, value;
    }
    static class Issue476Deserializer extends BeanDeserializer
    {
        protected static int propCount;

        public Issue476Deserializer(BeanDeserializer src) {
            super(src);
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property)
        {
            super.createContextual(ctxt, property);
            propCount++;
            return this;
        }
    }
    public class Issue476DeserializerModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
                BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
            if (Issue476Type.class == beanDesc.getBeanClass()) {
                return new Issue476Deserializer((BeanDeserializer)deserializer);
            }
            return super.modifyDeserializer(config, beanDesc, deserializer);
        }
    }
    public class Issue476Module extends SimpleModule
    {
        public Issue476Module() {
            super("Issue476Module", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context) {
            context.addDeserializerModifier(new Issue476DeserializerModifier());
        }
    }

    public static class Issue1912Bean {
        public Issue1912SubBean subBean;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) // This is need to populate _propertyBasedCreator on BeanDeserializerBase
        public Issue1912Bean(@JsonProperty("subBean") Issue1912SubBean subBean) {
            this.subBean = subBean;
        }
    }
    public static class Issue1912SubBean {
        public String a;

        public Issue1912SubBean() { }

        public Issue1912SubBean(String a) {
            this.a = a;
        }
    }

    public static class Issue1912CustomBeanDeserializer extends ValueDeserializer<Issue1912Bean> {
        private BeanDeserializer defaultDeserializer;

        public Issue1912CustomBeanDeserializer(BeanDeserializer defaultDeserializer) {
            this.defaultDeserializer = defaultDeserializer;
        }

        @Override
        public Issue1912Bean deserialize(JsonParser p, DeserializationContext ctxt)
        {
            // this is need on some cases, this populate _propertyBasedCreator
            defaultDeserializer.resolve(ctxt);

            p.nextName(); // read subBean
            p.nextToken(); // read start object

            Issue1912SubBean subBean = (Issue1912SubBean) defaultDeserializer.findProperty("subBean").deserialize(p, ctxt);

            return new Issue1912Bean(subBean);
        }
    }

    public static class Issue1912CustomPropertyDeserializer extends ValueDeserializer<Issue1912SubBean>
    {
        @Override
        public Issue1912SubBean deserialize(JsonParser p, DeserializationContext ctxt)
        {
            p.nextName(); // read "a"
            Issue1912SubBean object = new Issue1912SubBean(p.nextTextValue() + "_custom");
            p.nextToken();
            return object;
        }
    }

    public static class Issue1912UseAddOrReplacePropertyDeserializerModifier extends ValueDeserializerModifier
    {
        @Override
        public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
            if (beanDesc.getBeanClass() == Issue1912Bean.class) {
                return new Issue1912CustomBeanDeserializer((BeanDeserializer) deserializer);
            }
            return super.modifyDeserializer(config, beanDesc, deserializer);
        }

        @Override
        public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
            if (beanDesc.getBeanClass() == Issue1912Bean.class) {
                Iterator<SettableBeanProperty> props = builder.getProperties();
                while (props.hasNext()) {
                    SettableBeanProperty prop = props.next();
                    SettableBeanProperty propWithCustomDeserializer = prop.withValueDeserializer(new Issue1912CustomPropertyDeserializer());
                    builder.addOrReplaceProperty(propWithCustomDeserializer, true);
                }
            }

            return builder;
        }
    }
    public class Issue1912Module extends SimpleModule {

        public Issue1912Module() {
            super("Issue1912Module", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context) {
            context.addDeserializerModifier(new Issue1912UseAddOrReplacePropertyDeserializerModifier());
        }
    }

    // [Issue#121], arrays, collections, maps

    enum EnumABC { A, B, C; }

    static class ArrayDeserializerModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyArrayDeserializer(DeserializationConfig config, ArrayType valueType,
                BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
            return (ValueDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser jp,
                        DeserializationContext ctxt) {
                    return new String[] { "foo" };
                }
            };
        }
    }

    static class CollectionDeserializerModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config, CollectionType valueType,
                BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
            return (ValueDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser jp,
                        DeserializationContext ctxt) {
                    ArrayList<String> list = new ArrayList<String>();
                    list.add("foo");
                    return list;
                }
            };
        }
    }

    static class MapDeserializerModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType valueType,
                BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
            return (ValueDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser jp,
                        DeserializationContext ctxt) {
                    HashMap<String,String> map = new HashMap<String,String>();
                    map.put("a", "foo");
                    return map;
                }
            };
        }
    }

    static class EnumDeserializerModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType valueType,
                BeanDescription beanDesc, ValueDeserializer<?> deserializer) {
            return (ValueDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser jp,
                        DeserializationContext ctxt) {
                    return "foo";
                }
            };
        }
    }

    static class KeyDeserializerModifier extends ValueDeserializerModifier {
        @Override
        public KeyDeserializer modifyKeyDeserializer(DeserializationConfig config, JavaType valueType,
                KeyDeserializer kd) {
            return new KeyDeserializer() {
                @Override
                public Object deserializeKey(String key,
                        DeserializationContext ctxt) {
                    return "foo";
                }
            };
        }
    }

    static class UCStringDeserializer extends StdScalarDeserializer<String>
    {
        private final ValueDeserializer<?> _deser;

        public UCStringDeserializer(ValueDeserializer<?> orig) {
            super(String.class);
            _deser = orig;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            Object ob = _deser.deserialize(p, ctxt);
            return String.valueOf(ob).toUpperCase();
        }
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Test to verify details of how trying to deserialize into
     * abstract type should fail (if there is no way to determine
     * actual type information for the concrete type to use)
     */
    @Test
    public void testAbstractFailure() throws Exception
    {
        try {
            MAPPER.readValue("{ \"x\" : 3 }", Abstract.class);
            fail("Should fail on trying to deserialize abstract type");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "cannot construct");
        }
    }
    @Test
    public void testPropertyRemoval() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new ModuleImpl(new RemovingModifier("a")))
                .build();
        Bean bean = mapper.readValue("{\"b\":\"2\"}", Bean.class);
        assertEquals("2", bean.b);
        // and 'a' has its default value:
        assertEquals("a", bean.a);
    }

    @Test
    public void testDeserializerReplacement() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new ModuleImpl(new ReplacingModifier(new BogusBeanDeserializer("foo", "bar"))))
                .build();
        Bean bean = mapper.readValue("{\"a\":\"xyz\"}", Bean.class);
        // custom deserializer always produces instance like this:
        assertEquals("foo", bean.a);
        assertEquals("bar", bean.b);
    }

    @Test
    public void testIssue476() throws Exception
    {
        final String JSON = "{\"value1\" : {\"name\" : \"fruit\", \"value\" : \"apple\"}, \"value2\" : {\"name\" : \"color\", \"value\" : \"red\"}}";

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new Issue476Module())
                .build();
        mapper.readValue(JSON, Issue476Bean.class);

        // there are 2 properties
        assertEquals(2, Issue476Deserializer.propCount);
    }

    // [databind#120]
    @Test
    public void testModifyArrayDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setDeserializerModifier(new ArrayDeserializerModifier()))
                .build();
        Object[] result = mapper.readValue("[1,2]", Object[].class);
        assertEquals(1, result.length);
        assertEquals("foo", result[0]);
    }

    @Test
    public void testModifyCollectionDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setDeserializerModifier(new CollectionDeserializerModifier()))
            .build();
        List<?> result = mapper.readValue("[1,2]", List.class);
        assertEquals(1, result.size());
        assertEquals("foo", result.get(0));
    }

    @Test
    public void testModifyMapDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setDeserializerModifier(new MapDeserializerModifier()))
                .build();
        Map<?,?> result = mapper.readValue("{\"a\":1,\"b\":2}", Map.class);
        assertEquals(1, result.size());
        assertEquals("foo", result.get("a"));
    }

    @Test
    public void testModifyEnumDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setDeserializerModifier(new EnumDeserializerModifier()))
                .build();
        Object result = mapper.readValue(q("B"), EnumABC.class);
        assertEquals("foo", result);
    }

    @Test
    public void testModifyKeyDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setDeserializerModifier(new KeyDeserializerModifier()))
                .build();
        Map<?,?> result = mapper.readValue("{\"a\":1}", Map.class);
        assertEquals(1, result.size());
        assertEquals("foo", result.entrySet().iterator().next().getKey());
    }

    /**
     * Test to verify that even standard deserializers will result in `modifyDeserializer`
     * getting appropriately called.
     */
    @Test
    public void testModifyStdScalarDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setDeserializerModifier(new ValueDeserializerModifier() {
                        @Override
                        public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                BeanDescription beanDesc, ValueDeserializer<?> deser) {
                            if (beanDesc.getBeanClass() == String.class) {
                                return new UCStringDeserializer(deser);
                            }
                            return deser;
                        }
                        }))
                .build();
        Object result = mapper.readValue(q("abcDEF"), String.class);
        assertEquals("ABCDEF", result);
    }

    @Test
    public void testAddOrReplacePropertyIsUsedOnDeserialization() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new Issue1912Module())
                .build();

        Issue1912Bean result = mapper.readValue("{\"subBean\": {\"a\":\"foo\"}}", Issue1912Bean.class);
        assertEquals("foo_custom", result.subBean.a);
    }
}
