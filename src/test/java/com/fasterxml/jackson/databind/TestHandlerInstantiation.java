package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class TestHandlerInstantiation extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Helper classes, beans
    /**********************************************************************
     */

    @JsonDeserialize(using=MyBeanDeserializer.class)
    @JsonSerialize(using=MyBeanSerializer.class)
    static class MyBean
    {
        public String value;

        public MyBean() { this(null); }
        public MyBean(String s) { value = s; }
    }

    @SuppressWarnings("serial")
    @JsonDeserialize(keyUsing=MyKeyDeserializer.class)
    static class MyMap extends HashMap<String,String> { }

    @JsonTypeInfo(use=Id.CUSTOM, include=As.WRAPPER_ARRAY)
    @JsonTypeIdResolver(TestCustomIdResolver.class)
    static class TypeIdBean {
        public int x;

        public TypeIdBean() { }
        public TypeIdBean(int x) { this.x = x; }
    }

    static class TypeIdBeanWrapper {
        public TypeIdBean bean;

        public TypeIdBeanWrapper() { this(null); }
        public TypeIdBeanWrapper(TypeIdBean b) { bean = b; }
    }

    /*
    /**********************************************************************
    /* Helper classes, serializers/deserializers/resolvers
    /**********************************************************************
     */

    static class MyBeanDeserializer extends JsonDeserializer<MyBean>
    {
        public String _prefix = "";

        public MyBeanDeserializer(String p) {
            _prefix  = p;
        }

        @Override
        public MyBean deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException
        {
            return new MyBean(_prefix+jp.getText());
        }
    }

    static class MyKeyDeserializer extends KeyDeserializer
    {
        public MyKeyDeserializer() { }

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt)
                throws IOException
        {
            return "KEY";
        }
    }

    static class MyBeanSerializer extends JsonSerializer<MyBean>
    {
        public String _prefix = "";

        public MyBeanSerializer(String p) {
            _prefix  = p;
        }

        @Override
        public void serialize(MyBean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException
        {
            jgen.writeString(_prefix + value.value);
        }
    }

    // copied from "TestCustomTypeIdResolver"
    static class TestCustomIdResolver extends TypeIdResolverBase
    {
        static List<JavaType> initTypes;

        final String _id;

        public TestCustomIdResolver(String idForBean) {
            _id = idForBean;
        }

        @Override
        public Id getMechanism() {
            return Id.CUSTOM;
        }

        @Override
        public String idFromValue(Object value)
        {
            if (value.getClass() == TypeIdBean.class) {
                return _id;
            }
            return "unknown";
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return idFromValue(value);
        }

        @Override
        public void init(JavaType baseType) {
            if (initTypes != null) {
                initTypes.add(baseType);
            }
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id)
        {
            if (id.equals(_id)) {
                return TypeFactory.defaultInstance().constructType(TypeIdBean.class);
            }
            return null;
        }
        @Override
        public String idFromBaseType() {
            return "xxx";
        }
    }

    /*
    /**********************************************************************
    /* Helper classes, handler instantiator
    /**********************************************************************
     */

    static class MyInstantiator extends HandlerInstantiator
    {
        private final String _prefix;

        public MyInstantiator(String p) {
            _prefix = p;
        }

        @Override
        public JsonDeserializer<?> deserializerInstance(DeserializationConfig config,
                Annotated annotated,
                Class<?> deserClass)
        {
            if (deserClass == MyBeanDeserializer.class) {
                return new MyBeanDeserializer(_prefix);
            }
            return null;
        }

        @Override
        public KeyDeserializer keyDeserializerInstance(DeserializationConfig config,
                Annotated annotated, Class<?> keyDeserClass)
        {
            if (keyDeserClass == MyKeyDeserializer.class) {
                return new MyKeyDeserializer();
            }
            return null;

        }

        @Override
        public JsonSerializer<?> serializerInstance(SerializationConfig config,
                Annotated annotated, Class<?> serClass)
        {
            if (serClass == MyBeanSerializer.class) {
                return new MyBeanSerializer(_prefix);
            }
            return null;
        }

        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config,
                Annotated annotated, Class<?> resolverClass)
        {
            if (resolverClass == TestCustomIdResolver.class) {
                return new TestCustomIdResolver("!!!");
            }
            return null;
        }

        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated,
                Class<?> builderClass)
        {
            return null;
        }
    }

    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    public void testDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("abc:"));
        MyBean result = mapper.readValue(q("123"), MyBean.class);
        assertEquals("abc:123", result.value);
    }

    public void testKeyDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("abc:"));
        MyMap map = mapper.readValue("{\"a\":\"b\"}", MyMap.class);
        // easiest to test by just serializing...
        assertEquals("{\"KEY\":\"b\"}", mapper.writeValueAsString(map));
    }

    public void testSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("xyz:"));
        assertEquals(q("xyz:456"), mapper.writeValueAsString(new MyBean("456")));
    }

    public void testTypeIdResolver() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("foobar"));
        String json = mapper.writeValueAsString(new TypeIdBeanWrapper(new TypeIdBean(123)));
        // should now use our custom id scheme:
        assertEquals("{\"bean\":[\"!!!\",{\"x\":123}]}", json);
        // and bring it back too:
        TypeIdBeanWrapper result = mapper.readValue(json, TypeIdBeanWrapper.class);
        TypeIdBean bean = result.bean;
        assertEquals(123, bean.x);
    }

}
