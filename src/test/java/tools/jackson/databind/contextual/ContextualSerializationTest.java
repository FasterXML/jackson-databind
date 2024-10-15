package tools.jackson.databind.contextual;

import java.lang.annotation.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static tools.jackson.databind.testutil.DatabindTestUtil.q;

/**
 * Test cases to verify that it is possible to define serializers
 * that can use contextual information (like field/method
 * annotations) for configuration.
 */
public class ContextualSerializationTest
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
        extends ValueSerializer<String>
    {
        protected final String _prefix;

        public AnnotatedContextualSerializer() { this(""); }
        public AnnotatedContextualSerializer(String p) {
            _prefix = p;
        }

        @Override
        public void serialize(String value, JsonGenerator g, SerializerProvider provider)
        {
            g.writeString(_prefix + value);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
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
        extends ValueSerializer<String>
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
        public void serialize(String value, JsonGenerator g, SerializerProvider provider)
        {
            g.writeString("contextual="+isContextual+",resolved="+isResolved);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
        {
            return new ContextualAndResolvable(isResolved, isContextual+1);
        }

        @Override
        public void resolve(SerializerProvider provider) {
            ++isResolved;
        }
    }

    static class AccumulatingContextual
        extends ValueSerializer<String>
    {
        protected String desc;

        public AccumulatingContextual() { this(""); }

        public AccumulatingContextual(String newDesc) {
            desc = newDesc;
        }

        @Override
        public void serialize(String value, JsonGenerator g, SerializerProvider provider)
        {
            g.writeString(desc+"/"+value);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
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
    @Test
    public void testMethodAnnotations() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        assertEquals("{\"value\":\"see:foobar\"}", mapper.writeValueAsString(new ContextualBean("foobar")));
    }

    // Test to verify that contextual serializer can also use annotations
    // for enclosing class.
    @Test
    public void testClassAnnotations() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        assertEquals("{\"value\":\"Voila->xyz\"}", mapper.writeValueAsString(new BeanWithClassConfig("xyz")));
    }

    @Test
    public void testWrappedBean() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        assertEquals("{\"wrapped\":{\"value\":\"see:xyz\"}}", mapper.writeValueAsString(new ContextualBeanWrapper("xyz")));
    }

    // Serializer should get passed property context even if contained in an array.
    @Test
    public void testMethodAnnotationInArray() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        ContextualArrayBean beans = new ContextualArrayBean("123");
        assertEquals("{\"beans\":[\"array->123\"]}", mapper.writeValueAsString(beans));
    }

    // Serializer should get passed property context even if contained in a Collection.
    @Test
    public void testMethodAnnotationInList() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        ContextualListBean beans = new ContextualListBean("abc");
        assertEquals("{\"beans\":[\"list->abc\"]}", mapper.writeValueAsString(beans));
    }

    // Serializer should get passed property context even if contained in a Collection.
    @Test
    public void testMethodAnnotationInMap() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        ContextualMapBean map = new ContextualMapBean();
        map.beans.put("first", "In Map");
        assertEquals("{\"beans\":{\"first\":\"map->In Map\"}}", mapper.writeValueAsString(map));
    }

    @Test
    public void testContextualViaAnnotation() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        AnnotatedContextualBean bean = new AnnotatedContextualBean("abc");
        assertEquals("{\"value\":\"prefix->abc\"}", mapper.writeValueAsString(bean));
    }

    @Test
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

    @Test
    public void testContextualArrayElement() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        ContextualArrayElementBean beans = new ContextualArrayElementBean("456");
        assertEquals("{\"beans\":[\"elem->456\"]}", mapper.writeValueAsString(beans));
    }

    // Test to verify aspects of [databind#2429]
    @Test
    public void testRootContextualization2429() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .addModule(new SimpleModule("test", Version.unknownVersion())
                        .addSerializer(String.class, new AccumulatingContextual()))
                .build();
        assertEquals(q("/ROOT/foo"), mapper.writeValueAsString("foo"));
        assertEquals(q("/ROOT/bar"), mapper.writeValueAsString("bar"));
        assertEquals(q("/ROOT/3"), mapper.writeValueAsString("3"));
    }
}
