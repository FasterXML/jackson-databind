package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

/**
 * Unit tests for verifying that it is possible to configure
 * construction of {@link BeanSerializer} instances.
 */
@SuppressWarnings("serial")
public class ValueSerializerModifierTest extends BaseMapTest
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
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
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
        public List<BeanPropertyWriter> orderProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties)
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
        public ValueSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                ValueSerializer<?> serializer) {
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
                BeanDescription beanDesc, BeanSerializerBuilder builder) {
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
                SerializerProvider provider) {
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
                BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties)
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
                        null));
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
                BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            return beanProperties;
        }
        
        @Override
        public ValueSerializer<?> modifySerializer(SerializationConfig config,
                BeanDescription beanDesc, ValueSerializer<?> serializer) {
            return new BogusBeanSerializer(42);
        }
    }
    // [databind#120], arrays, collections, maps
    
    static class ArraySerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyArraySerializer(SerializationConfig config,
                ArrayType valueType, BeanDescription beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializerProvider provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class CollectionSerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyCollectionSerializer(SerializationConfig config,
                CollectionType valueType, BeanDescription beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializerProvider provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class MapSerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyMapSerializer(SerializationConfig config,
                MapType valueType, BeanDescription beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializerProvider provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class EnumSerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config,
                JavaType valueType, BeanDescription beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializerProvider provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class KeySerializerModifier extends ValueSerializerModifier {
        @Override
        public ValueSerializer<?> modifyKeySerializer(SerializationConfig config,
                JavaType valueType, BeanDescription beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializerProvider provider) {
                    g.writeName("foo");
                }
            };
        }
    }

    /*
    /********************************************************
    /* Unit tests: success
    /********************************************************
     */

    public void testPropertyRemoval() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new RemovingModifier("a")))
                .build();
        Bean bean = new Bean();
        assertEquals("{\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    public void testPropertyReorder() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new ReorderingModifier()))
                .build();
        Bean bean = new Bean();
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    public void testBuilderReplacement() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new BuilderModifier(new BogusBeanSerializer(17))))
                .build();
        Bean bean = new Bean();
        assertEquals("17", mapper.writeValueAsString(bean));
    }    
    public void testSerializerReplacement() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new ReplacingModifier(new BogusBeanSerializer(123))))
                .build();
        Bean bean = new Bean();
        assertEquals("123", mapper.writeValueAsString(bean));
    }

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

    public void testModifyArraySerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new ArraySerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new Integer[] { 1, 2 }));
    }

    public void testModifyCollectionSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new CollectionSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new ArrayList<Integer>()));
    }

    public void testModifyMapSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new MapSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new HashMap<String,String>()));
    }

    public void testModifyEnumSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new EnumSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(ABC.C));
    }

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
}
