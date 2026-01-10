package tools.jackson.databind.ser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.POJOPropertyBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying that it is possible to configure
 * construction of {@link BeanSerializer} instances.
 */
@SuppressWarnings("serial")
public class ValueSerializerModifierTest extends DatabindTestUtil
{
    static class SerializerModifierModule extends SimpleModule
    {
        protected ValueSerializerModifier modifier;

        public SerializerModifierModule(ValueSerializerModifier modifier)
        {
            super("test", Version.unknownVersion());
            this.modifier = modifier;
        }

        @Override
        public void setupModule(SetupContext context)
        {
            super.setupModule(context);
            if (modifier != null) {
                context.addSerializerModifier(modifier);
            }
        }
    }

    @JsonPropertyOrder({"b", "a"})
    static class Bean {
        public String b = "b";
        public String a = "a";
    }

    static class RemovingModifier extends ValueSerializerModifier
    {
        private final String _removedProperty;

        public RemovingModifier(String remove) { _removedProperty = remove; }

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc,
                List<BeanPropertyWriter> beanProperties)
        {
            Iterator<BeanPropertyWriter> it = beanProperties.iterator();
            while (it.hasNext()) {
                BeanPropertyWriter bpw = it.next();
                if (bpw.getName().equals(_removedProperty)) {
                    it.remove();
                }
            }
            return beanProperties;
        }
    }

    static class ReorderingModifier extends ValueSerializerModifier
    {
        @Override
        public List<BeanPropertyWriter> orderProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            TreeMap<String,BeanPropertyWriter> props = new TreeMap<String,BeanPropertyWriter>();
            for (BeanPropertyWriter bpw : beanProperties) {
                props.put(bpw.getName(), bpw);
            }
            return new ArrayList<BeanPropertyWriter>(props.values());
        }
    }

    static class ReplacingModifier extends ValueSerializerModifier
    {
        private final ValueSerializer<?> _serializer;

        public ReplacingModifier(ValueSerializer<?> s) { _serializer = s; }

        @Override
        public ValueSerializer<?> modifySerializer(SerializationConfig config,
                BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return _serializer;
        }
    }

    static class BuilderModifier extends ValueSerializerModifier
    {
        private final ValueSerializer<?> _serializer;

        public BuilderModifier(ValueSerializer<?> ser) {
            _serializer = ser;
        }

        @Override
        public BeanSerializerBuilder updateBuilder(SerializationConfig config,
                BeanDescription.Supplier beanDesc, BeanSerializerBuilder builder) {
            return new BogusSerializerBuilder(builder, _serializer);
        }
    }

    static class BogusSerializerBuilder extends BeanSerializerBuilder
    {
        private final ValueSerializer<?> _serializer;

        public BogusSerializerBuilder(BeanSerializerBuilder src,
                ValueSerializer<?> ser) {
            super(src);
            _serializer = ser;
        }

        @Override
        public ValueSerializer<?> build() {
            return _serializer;
        }
    }

    static class BogusBeanSerializer extends StdSerializer<Object>
    {
        private final int _value;

        public BogusBeanSerializer(int v) {
            super(Object.class);
            _value = v;
        }

        @Override
        public void serialize(Object value, JsonGenerator g,
                SerializationContext provider) {
            g.writeNumber(_value);
        }
    }

    static class EmptyBean {
        @JsonIgnore
        public String name = "foo";
    }

    static class EmptyBeanModifier extends ValueSerializerModifier
    {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            JavaType strType = config.constructType(String.class);
            // we need a valid BeanPropertyDefinition; this will do (just need name to match)
            POJOPropertyBuilder prop = new POJOPropertyBuilder(config, null, true, new PropertyName("bogus"));
            try {
                AnnotatedField f = new AnnotatedField(null, EmptyBean.class.getDeclaredField("name"), null);
                beanProperties.add(new BeanPropertyWriter(prop, f, null,
                        strType,
                        null, null, strType,
                        false, null,
                        null, null));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e.getMessage());
            }
            return beanProperties;
        }
    }

    // [Issue#539]: use post-modifier
    static class EmptyBeanModifier539 extends ValueSerializerModifier
    {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            return beanProperties;
        }

        @Override
        public ValueSerializer<?> modifySerializer(SerializationConfig config,
                BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new BogusBeanSerializer(42);
        }
    }
    // [databind#120], arrays, collections, maps

    static class ArraySerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyArraySerializer(SerializationConfig config,
                ArrayType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class CollectionSerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyCollectionSerializer(SerializationConfig config,
                CollectionType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class MapSerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyMapSerializer(SerializationConfig config,
                MapType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class EnumSerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config,
                JavaType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class KeySerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyKeySerializer(SerializationConfig config,
                JavaType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeName("foo");
                }
            };
        }
    }

    // [databind#1612]
    @JsonPropertyOrder({ "a", "b", "c" })
    static class Bean1612 {
        public Integer a;
        public Integer b;
        public Double c;

        public Bean1612(Integer a, Integer b, Double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    static class Modifier1612 extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public BeanSerializerBuilder updateBuilder(SerializationConfig config,
                BeanDescription.Supplier beanDescRef,
                BeanSerializerBuilder builder) {
            List<BeanPropertyWriter> filtered = new ArrayList<BeanPropertyWriter>(2);
            List<BeanPropertyWriter> properties = builder.getProperties();
            //Make the filtered properties list bigger
            builder.setFilteredProperties(new BeanPropertyWriter[] {properties.get(0), properties.get(1), properties.get(2)});

            //The props will be shorter
            filtered.add(properties.get(1));
            filtered.add(properties.get(2));
            builder.setProperties(filtered);
            return builder;
        }
    }

    
    // [databind#5414]

    // HiddenFieldModule should prevent the output of the password field.
    record User5414(String name, @Hidden String password) {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface Hidden {}

    static class HiddenFieldModule5414 extends SimpleModule {
        @Override
        public void setupModule(SetupContext context) {
          super.setupModule(context);
          context.addSerializerModifier(new HiddenFieldRemover5414());
        }
    }

    static class HiddenFieldRemover5414 extends ValueSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
            return beanProperties.stream()
                    .filter(writer -> !isHidden(writer.getMember()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private boolean isHidden(AnnotatedMember member) {
            if (member.annotations() == null) {
                return false;
            }
            return member
                    .annotations()
                    .anyMatch(annotation -> annotation.annotationType().equals(Hidden.class));
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testPropertyRemoval() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new RemovingModifier("a")))
                .build();
        Bean bean = new Bean();
        assertEquals("{\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    @Test
    public void testPropertyReorder() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new ReorderingModifier()))
                .build();
        Bean bean = new Bean();
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    @Test
    public void testBuilderReplacement() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new BuilderModifier(new BogusBeanSerializer(17))))
                .build();
        Bean bean = new Bean();
        assertEquals("17", mapper.writeValueAsString(bean));
    }

    @Test
    public void testSerializerReplacement() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new ReplacingModifier(new BogusBeanSerializer(123))))
                .build();
        Bean bean = new Bean();
        assertEquals("123", mapper.writeValueAsString(bean));
    }

    @Test
    public void testEmptyBean() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test", Version.unknownVersion()) {
            @Override
            public void setupModule(SetupContext context)
            {
                super.setupModule(context);
                context.addSerializerModifier(new EmptyBeanModifier());
            }
                })
                .build();
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"bogus\":\"foo\"}", json);
    }

    @Test
    public void testEmptyBean539() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test", Version.unknownVersion()) {
            @Override
            public void setupModule(SetupContext context)
            {
                super.setupModule(context);
                context.addSerializerModifier(new EmptyBeanModifier539());
            }
                })
                .build();
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("42", json);
    }

    // [databind#121]

    @Test
    public void testModifyArraySerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new ArraySerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new Integer[] { 1, 2 }));
    }

    @Test
    public void testModifyCollectionSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new CollectionSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new ArrayList<Integer>()));
    }

    @Test
    public void testModifyMapSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new MapSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new HashMap<String,String>()));
    }

    @Test
    public void testModifyEnumSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new EnumSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(ABC.C));
    }

    @Test
    public void testModifyKeySerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new KeySerializerModifier()))
                .build();
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("x", 3);
        assertEquals("{\"foo\":3}", mapper.writeValueAsString(map));
    }

    // [databind#1612]
    @Test
    public void modifierIssue1612Test() throws Exception
    {
        SimpleModule mod = new SimpleModule();
        mod.setSerializerModifier(new Modifier1612());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        try {
            mapper.writeValueAsString(new Bean1612(0, 1, 2d));
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Failed to construct BeanSerializer");
            verifyException(e, Bean1612.class.getName());
        }
    }

    // [databind#5414]
    @Test
    public void annotationsAccessIssue5414()
    {
        var mapper = JsonMapper.builder()
                .addModule(new HiddenFieldModule5414())
                .build();
        User5414 user = new User5414("John", "123456");
        String userJson = mapper.writeValueAsString(user);
        assertEquals("{\"name\":\"John\"}", userJson);
    }
}
