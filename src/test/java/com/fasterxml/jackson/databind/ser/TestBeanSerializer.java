package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

/**
 * Unit tests for verifying that it is possible to configure
 * construction of {@link BeanSerializer} instances.
 */
@SuppressWarnings("serial")
public class TestBeanSerializer extends BaseMapTest
{
    /*
    /********************************************************
    /* Helper types
    /********************************************************
     */

    static class ModuleImpl extends SimpleModule
    {
        protected BeanSerializerModifier modifier;
        
        public ModuleImpl(BeanSerializerModifier modifier)
        {
            super("test", Version.unknownVersion());
            this.modifier = modifier;
        }
        
        @Override
        public void setupModule(SetupContext context)
        {
            super.setupModule(context);
            if (modifier != null) {
                context.addBeanSerializerModifier(modifier);
            }
        }
    }

    @JsonPropertyOrder({"b", "a"})
    static class Bean {
        public String b = "b";
        public String a = "a";
    }

    static class RemovingModifier extends BeanSerializerModifier
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
    
    static class ReorderingModifier extends BeanSerializerModifier
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

    static class ReplacingModifier extends BeanSerializerModifier
    {
        private final JsonSerializer<?> _serializer;
        
        public ReplacingModifier(JsonSerializer<?> s) { _serializer = s; }
        
        @Override
        public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                JsonSerializer<?> serializer) {
            return _serializer;
        }
    }

    static class BuilderModifier extends BeanSerializerModifier
    {
        private final JsonSerializer<?> _serializer;
        
        public BuilderModifier(JsonSerializer<?> ser) {
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
        private final JsonSerializer<?> _serializer;
        
        public BogusSerializerBuilder(BeanSerializerBuilder src,
                JsonSerializer<?> ser) {
            super(src);
            _serializer = ser;
        }

        @Override
        public JsonSerializer<?> build() {
            return _serializer;
        }
    }
    
    static class BogusBeanSerializer extends JsonSerializer<Object>
    {
        private final int _value;
        
        public BogusBeanSerializer(int v) { _value = v; }
        
        @Override
        public void serialize(Object value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException {
            jgen.writeNumber(_value);
        }
    }

    // for [JACKSON-670]
    
    static class EmptyBean {
        @JsonIgnore
        public String name = "foo";
    }

    static class EmptyBeanModifier extends BeanSerializerModifier
    {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            JavaType strType = config.constructType(String.class);
            // we need a valid BeanPropertyDefinition; this will do (just need name to match)
            POJOPropertyBuilder prop = new POJOPropertyBuilder(new PropertyName("bogus"), null, true);
            try {
                AnnotatedField f = new AnnotatedField(EmptyBean.class.getDeclaredField("name"), null);
                beanProperties.add(new BeanPropertyWriter(prop, f, null,
                        strType,
                        null, null, strType,
                        false, null));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e.getMessage());
            }
            return beanProperties;
        }
    }

    // [Issue#120], arrays, collections, maps
    
    static class ArraySerializerModifier extends BeanSerializerModifier {
        @Override
        public JsonSerializer<?> modifyArraySerializer(SerializationConfig config,
                ArrayType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeNumber(123);
                }
            };
        }
    }

    static class CollectionSerializerModifier extends BeanSerializerModifier {
        @Override
        public JsonSerializer<?> modifyCollectionSerializer(SerializationConfig config,
                CollectionType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeNumber(123);
                }
            };
        }
    }

    static class MapSerializerModifier extends BeanSerializerModifier {
        @Override
        public JsonSerializer<?> modifyMapSerializer(SerializationConfig config,
                MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeNumber(123);
                }
            };
        }
    }

    static class EnumSerializerModifier extends BeanSerializerModifier {
        @Override
        public JsonSerializer<?> modifyEnumSerializer(SerializationConfig config,
                JavaType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeNumber(123);
                }
            };
        }
    }

    static class KeySerializerModifier extends BeanSerializerModifier {
        @Override
        public JsonSerializer<?> modifyKeySerializer(SerializationConfig config,
                JavaType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeFieldName("foo");
                }
            };
        }
    }
    
    enum EnumABC { A, B, C };
    
    /*
    /********************************************************
    /* Unit tests: success
    /********************************************************
     */

    public void testPropertyRemoval() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new RemovingModifier("a")));
        Bean bean = new Bean();
        assertEquals("{\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    public void testPropertyReorder() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new ReorderingModifier()));
        Bean bean = new Bean();
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    public void testBuilderReplacement() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new BuilderModifier(new BogusBeanSerializer(17))));
        Bean bean = new Bean();
        assertEquals("17", mapper.writeValueAsString(bean));
    }    
    public void testSerializerReplacement() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new ReplacingModifier(new BogusBeanSerializer(123))));
        Bean bean = new Bean();
        assertEquals("123", mapper.writeValueAsString(bean));
    }

    // for [JACKSON-670]
    public void testEmptyBean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test", Version.unknownVersion()) {
            @Override
            public void setupModule(SetupContext context)
            {
                super.setupModule(context);
                context.addBeanSerializerModifier(new EmptyBeanModifier());
            }
        });
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"bogus\":\"foo\"}", json);
    }

    // [Issue#121]

    public void testModifyArraySerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setSerializerModifier(new ArraySerializerModifier()));
        assertEquals("123", mapper.writeValueAsString(new Integer[] { 1, 2 }));
    }

    public void testModifyCollectionSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setSerializerModifier(new CollectionSerializerModifier()));
        assertEquals("123", mapper.writeValueAsString(new ArrayList<Integer>()));
    }

    public void testModifyMapSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setSerializerModifier(new MapSerializerModifier()));
        assertEquals("123", mapper.writeValueAsString(new HashMap<String,String>()));
    }

    public void testModifyEnumSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setSerializerModifier(new EnumSerializerModifier()));
        assertEquals("123", mapper.writeValueAsString(EnumABC.C));
    }

    public void testModifyKeySerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("test")
            .setSerializerModifier(new KeySerializerModifier()));
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("x", 3);
        assertEquals("{\"foo\":3}", mapper.writeValueAsString(map));
    }
}
