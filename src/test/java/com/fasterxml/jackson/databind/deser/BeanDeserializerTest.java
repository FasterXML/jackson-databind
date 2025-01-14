package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

@SuppressWarnings("serial")
public class BeanDeserializerTest
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
        protected BeanDeserializerModifier modifier;

        public ModuleImpl(BeanDeserializerModifier modifier)
        {
            super("test", Version.unknownVersion());
            this.modifier = modifier;
        }

        @Override
        public void setupModule(SetupContext context)
        {
            super.setupModule(context);
            if (modifier != null) {
                context.addBeanDeserializerModifier(modifier);
            }
        }
    }

    static class RemovingModifier extends BeanDeserializerModifier
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

    static class ReplacingModifier extends BeanDeserializerModifier
    {
        private final JsonDeserializer<?> _deserializer;

        public ReplacingModifier(JsonDeserializer<?> s) { _deserializer = s; }

        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            return _deserializer;
        }
    }

    static class BogusBeanDeserializer extends JsonDeserializer<Object>
    {
        private final String a, b;

        public BogusBeanDeserializer(String a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            p.skipChildren();
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
        implements ContextualDeserializer
    {
        protected static int propCount;

        public Issue476Deserializer(BeanDeserializer src) {
            super(src);
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property) {
            propCount++;
            return this;
        }
    }
    public class Issue476DeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
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
            context.addBeanDeserializerModifier(new Issue476DeserializerModifier());
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

    public static class Issue1912CustomBeanDeserializer
        extends JsonDeserializer<Issue1912Bean>
        implements ContextualDeserializer
    {
        private BeanDeserializer defaultDeserializer;

        public Issue1912CustomBeanDeserializer(BeanDeserializer defaultDeserializer) {
            this.defaultDeserializer = defaultDeserializer;
        }

        @Override
        public Issue1912Bean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.nextFieldName(); // read subBean
            if (p.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalArgumentException("Unexpected token "+p.currentToken());
            }

            Issue1912SubBean subBean = (Issue1912SubBean) defaultDeserializer.findProperty("subBean").deserialize(p, ctxt);
            // Must also read trailing END_OBJECT
            if (p.nextToken() != JsonToken.END_OBJECT) {
                throw new IllegalArgumentException("Unexpected token "+p.currentToken());
            }

            return new Issue1912Bean(subBean);
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
                throws JsonMappingException {
            if (defaultDeserializer instanceof ContextualDeserializer) {
                ContextualDeserializer cd = (ContextualDeserializer) defaultDeserializer;
                return new Issue1912CustomBeanDeserializer((BeanDeserializer) cd.createContextual(ctxt, property));
            }
            return this;
        }
    }

    public static class Issue1912CustomPropertyDeserializer extends JsonDeserializer<Issue1912SubBean> {

        @Override
        public Issue1912SubBean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.nextFieldName(); // read "a"
            Issue1912SubBean object = new Issue1912SubBean(p.nextTextValue() + "_custom");
            p.nextToken();
            return object;
        }
    }
    public static class Issue1912UseAddOrReplacePropertyDeserializerModifier extends BeanDeserializerModifier {

        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
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
            context.addBeanDeserializerModifier(new Issue1912UseAddOrReplacePropertyDeserializerModifier());
        }
    }

    // [Issue#121], arrays, collections, maps

    enum EnumABC { A, B, C; }

    static class ArrayDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyArrayDeserializer(DeserializationConfig config, ArrayType valueType,
                BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            return (JsonDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser p,
                        DeserializationContext ctxt) throws IOException
                {
                    p.skipChildren();
                    return new String[] { "foo" };
                }
            };
        }
    }

    static class CollectionDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config, CollectionType valueType,
                BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            return (JsonDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser p,
                        DeserializationContext ctxt) throws IOException
                {
                    p.skipChildren();
                    ArrayList<String> list = new ArrayList<String>();
                    list.add("foo");
                    return list;
                }
            };
        }
    }

    static class MapDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType valueType,
                BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            return (JsonDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser p,
                        DeserializationContext ctxt) throws IOException
                {
                    p.skipChildren();
                    HashMap<String,String> map = new HashMap<String,String>();
                    map.put("a", "foo");
                    return map;
                }
            };
        }
    }

    static class EnumDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType valueType,
                BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            return (JsonDeserializer<?>) new StdDeserializer<Object>(Object.class) {
                @Override public Object deserialize(JsonParser jp,
                        DeserializationContext ctxt) {
                    return "foo";
                }
            };
        }
    }

    static class KeyDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public KeyDeserializer modifyKeyDeserializer(DeserializationConfig config, JavaType valueType,
                KeyDeserializer kd) {
            return new KeyDeserializer() {
                @Override
                public Object deserializeKey(String key,
                        DeserializationContext ctxt) throws IOException
                 {
                    return "foo";
                }
            };
        }
    }

    static class UCStringDeserializer extends StdScalarDeserializer<String>
    {
        private final JsonDeserializer<?> _deser;

        public UCStringDeserializer(JsonDeserializer<?> orig) {
            super(String.class);
            _deser = orig;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new RemovingModifier("a")));
        Bean bean = mapper.readValue("{\"b\":\"2\"}", Bean.class);
        assertEquals("2", bean.b);
        // and 'a' has its default value:
        assertEquals("a", bean.a);
    }

    @Test
    public void testDeserializerReplacement() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new ReplacingModifier(new BogusBeanDeserializer("foo", "bar"))));
        Bean bean = mapper.readValue("{\"a\":\"xyz\"}", Bean.class);
        // custom deserializer always produces instance like this:
        assertEquals("foo", bean.a);
        assertEquals("bar", bean.b);
    }

    @Test
    public void testIssue476() throws Exception
    {
        final String JSON = "{\"value1\" : {\"name\" : \"fruit\", \"value\" : \"apple\"}, \"value2\" : {\"name\" : \"color\", \"value\" : \"red\"}}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Issue476Module());
        mapper.readValue(JSON, Issue476Bean.class);

        // there are 2 properties
        assertEquals(2, Issue476Deserializer.propCount);
    }

    // [databind#120]
    @Test
    public void testModifyArrayDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setDeserializerModifier(new ArrayDeserializerModifier()));
        Object[] result = mapper.readValue("[1,2]", Object[].class);
        assertEquals(1, result.length);
        assertEquals("foo", result[0]);
    }

    @Test
    public void testModifyCollectionDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setDeserializerModifier(new CollectionDeserializerModifier())
        );
        List<?> result = mapper.readValue("[1,2]", List.class);
        assertEquals(1, result.size());
        assertEquals("foo", result.get(0));
    }

    @Test
    public void testModifyMapDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setDeserializerModifier(new MapDeserializerModifier())
        );
        Map<?,?> result = mapper.readValue("{\"a\":1,\"b\":2}", Map.class);
        assertEquals(1, result.size());
        assertEquals("foo", result.get("a"));
    }

    @Test
    public void testModifyEnumDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setDeserializerModifier(new EnumDeserializerModifier())
        );
        Object result = mapper.readValue(q("B"), EnumABC.class);
        assertEquals("foo", result);
    }

    @Test
    public void testModifyKeyDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setDeserializerModifier(new KeyDeserializerModifier())
        );
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setDeserializerModifier(new BeanDeserializerModifier() {
                        @Override
                        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                BeanDescription beanDesc, JsonDeserializer<?> deser) {
                            if (beanDesc.getBeanClass() == String.class) {
                                return new UCStringDeserializer(deser);
                            }
                            return deser;
                        }
            }));
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
