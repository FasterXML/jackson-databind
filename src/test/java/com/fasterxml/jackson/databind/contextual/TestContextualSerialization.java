package com.fasterxml.jackson.databind.contextual;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

/**
 * Test cases to verify that it is possible to define serializers
 * that can use contextual information (like field/method
 * annotations) for configuration.
 */
public class TestContextualSerialization extends BaseMapTest
{
    // NOTE: important; MUST be considered a 'Jackson' annotation to be seen
    // (or recognized otherwise via AnnotationIntrospect.isHandled())
    @Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    public @interface Prefix {
        public String value();
    }

    static class ContextualBean
    {
        protected final String _value;

        public ContextualBean(String s) { _value = s; }

        @Prefix("see:")
        public String getValue() { return _value; }
    }

    // For [JACKSON-569]
    static class AnnotatedContextualBean
    {
        @Prefix("prefix->")
        @JsonSerialize(using=AnnotatedContextualSerializer.class)
        protected final String value;

        public AnnotatedContextualBean(String s) { value = s; }
    }


    @Prefix("wrappedBean:")
    static class ContextualBeanWrapper
    {
        @Prefix("wrapped:")
        public ContextualBean wrapped;

        public ContextualBeanWrapper(String s) {
            wrapped = new ContextualBean(s);
        }
    }

    static class ContextualArrayBean
    {
        @Prefix("array->")
        public final String[] beans;

        public ContextualArrayBean(String... strings) {
            beans = strings;
        }
    }

    static class ContextualArrayElementBean
    {
        @Prefix("elem->")
        @JsonSerialize(contentUsing=AnnotatedContextualSerializer.class)
        public final String[] beans;

        public ContextualArrayElementBean(String... strings) {
            beans = strings;
        }
    }

    static class ContextualListBean
    {
        @Prefix("list->")
        public final List<String> beans = new ArrayList<String>();

        public ContextualListBean(String... strings) {
            for (String string : strings) {
                beans.add(string);
            }
        }
    }

    static class ContextualMapBean
    {
        @Prefix("map->")
        public final Map<String, String> beans = new HashMap<String, String>();
    }

    /**
     * Another bean that has class annotations that should be visible for
     * contextualizer, too
     */
    @Prefix("Voila->")
    static class BeanWithClassConfig
    {
        public String value;

        public BeanWithClassConfig(String v) { value = v; }
    }

    /**
     * Annotation-based contextual serializer that simply prepends piece of text.
     */
    static class AnnotatedContextualSerializer
        extends JsonSerializer<String>
        implements ContextualSerializer
    {
        protected final String _prefix;

        public AnnotatedContextualSerializer() { this(""); }
        public AnnotatedContextualSerializer(String p) {
            _prefix = p;
        }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeString(_prefix + value);
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
        {
            String prefix = "UNKNOWN";
            Prefix ann = null;
            if (property != null) {
                ann = property.getAnnotation(Prefix.class);
                if (ann == null) {
                    ann = property.getContextAnnotation(Prefix.class);
                }
            }
            if (ann != null) {
                prefix = ann.value();
            }
            return new AnnotatedContextualSerializer(prefix);
        }
    }

    static class ContextualAndResolvable
        extends JsonSerializer<String>
        implements ContextualSerializer, ResolvableSerializer
    {
        protected int isContextual;
        protected int isResolved;

        public ContextualAndResolvable() { this(0, 0); }

        public ContextualAndResolvable(int resolved, int contextual)
        {
            isContextual = contextual;
            isResolved = resolved;
        }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeString("contextual="+isContextual+",resolved="+isResolved);
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
        {
            return new ContextualAndResolvable(isResolved, isContextual+1);
        }

        @Override
        public void resolve(SerializerProvider provider) {
            ++isResolved;
        }
    }

    static class AccumulatingContextual
        extends JsonSerializer<String>
        implements ContextualSerializer
    {
        protected String desc;

        public AccumulatingContextual() { this(""); }

        public AccumulatingContextual(String newDesc) {
            desc = newDesc;
        }

        @Override
        public void serialize(String value, JsonGenerator g, SerializerProvider provider) throws IOException
        {
            g.writeString(desc+"/"+value);
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
        {
            if (property == null) {
                return new AccumulatingContextual(desc+"/ROOT");
            }
            return new AccumulatingContextual(desc+"/"+property.getName());
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // Test to verify that contextual serializer can make use of property
    // (method, field) annotations.
    public void testMethodAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"value\":\"see:foobar\"}", mapper.writeValueAsString(new ContextualBean("foobar")));
    }

    // Test to verify that contextual serializer can also use annotations
    // for enclosing class.
    public void testClassAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"value\":\"Voila->xyz\"}", mapper.writeValueAsString(new BeanWithClassConfig("xyz")));
    }

    public void testWrappedBean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"wrapped\":{\"value\":\"see:xyz\"}}", mapper.writeValueAsString(new ContextualBeanWrapper("xyz")));
    }

    // Serializer should get passed property context even if contained in an array.
    public void testMethodAnnotationInArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        ContextualArrayBean beans = new ContextualArrayBean("123");
        assertEquals("{\"beans\":[\"array->123\"]}", mapper.writeValueAsString(beans));
    }

    // Serializer should get passed property context even if contained in a Collection.
    public void testMethodAnnotationInList() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        ContextualListBean beans = new ContextualListBean("abc");
        assertEquals("{\"beans\":[\"list->abc\"]}", mapper.writeValueAsString(beans));
    }

    // Serializer should get passed property context even if contained in a Collection.
    public void testMethodAnnotationInMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        ContextualMapBean map = new ContextualMapBean();
        map.beans.put("first", "In Map");
        assertEquals("{\"beans\":{\"first\":\"map->In Map\"}}", mapper.writeValueAsString(map));
    }

    public void testContextualViaAnnotation() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotatedContextualBean bean = new AnnotatedContextualBean("abc");
        assertEquals("{\"value\":\"prefix->abc\"}", mapper.writeValueAsString(bean));
    }

    public void testResolveOnContextual() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new ContextualAndResolvable());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        assertEquals(q("contextual=1,resolved=1"), mapper.writeValueAsString("abc"));

        // also: should NOT be called again
        assertEquals(q("contextual=1,resolved=1"), mapper.writeValueAsString("foo"));
    }

    public void testContextualArrayElement() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        ContextualArrayElementBean beans = new ContextualArrayElementBean("456");
        assertEquals("{\"beans\":[\"elem->456\"]}", mapper.writeValueAsString(beans));
    }

    // Test to verify aspects of [databind#2429]
    public void testRootContextualization2429() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test", Version.unknownVersion())
                        .addSerializer(String.class, new AccumulatingContextual()))
                .build();
        assertEquals(q("/ROOT/foo"), mapper.writeValueAsString("foo"));
        assertEquals(q("/ROOT/bar"), mapper.writeValueAsString("bar"));
        assertEquals(q("/ROOT/3"), mapper.writeValueAsString("3"));
    }
}
