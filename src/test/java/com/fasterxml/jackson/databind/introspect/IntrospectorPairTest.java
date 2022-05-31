package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.*;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.jdk.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.jdk.StringDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.jdk.StringSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

// started with [databind#1025] in mind
@SuppressWarnings("serial")
public class IntrospectorPairTest extends BaseMapTest
{
    static class Introspector1 extends AnnotationIntrospector {
        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a) {
            return JsonInclude.Value.empty()
                    .withContentInclusion(JsonInclude.Include.ALWAYS)
                    .withValueInclusion(JsonInclude.Include.NON_ABSENT);
        }
    }

    static class Introspector2 extends AnnotationIntrospector {
        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a) {
            return JsonInclude.Value.empty()
                    .withContentInclusion(JsonInclude.Include.NON_EMPTY)
                    .withValueInclusion(JsonInclude.Include.USE_DEFAULTS);
        }
    }

    static class IntrospectorWithHandlers extends AnnotationIntrospector {
        final Object _deserializer;
        final Object _serializer;

        public IntrospectorWithHandlers(Object deser, Object ser) {
            _deserializer = deser;
            _serializer = ser;
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public Object findDeserializer(MapperConfig<?> config, Annotated am) {
            return _deserializer;
        }

        @Override
        public Object findSerializer(MapperConfig<?> config, Annotated am) {
            return _serializer;
        }
    }

    static class IntrospectorWithMap extends AnnotationIntrospector
    {
        private final Map<String, Object> values = new HashMap<>();

        private Version version = Version.unknownVersion();

        public IntrospectorWithMap add(String key, Object value) {
            values.put(key, value);
            return this;
        }

        public IntrospectorWithMap version(Version v) {
            version = v;
            return this;
        }

        @Override
        public Version version() {
            return version;
        }

        @Override
        public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a) {
            return JsonInclude.Value.empty()
                    .withContentInclusion(JsonInclude.Include.NON_EMPTY)
                    .withValueInclusion(JsonInclude.Include.USE_DEFAULTS);
        }

        @Override
        public boolean isAnnotationBundle(Annotation ann) {
            return _boolean("isAnnotationBundle");
        }

        /*
        /******************************************************
        /* General class annotations
        /******************************************************
         */        

        @Override
        public PropertyName findRootName(MapperConfig<?> config, AnnotatedClass ac) {
            return (PropertyName) values.get("findRootName");
        }

        @Override
        public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated a) {
            return (JsonIgnoreProperties.Value) values.get("findPropertyIgnoralByName");
        }

        @Override
        public Boolean isIgnorableType(MapperConfig<?> config, AnnotatedClass ac) {
            return (Boolean) values.get("isIgnorableType");
        }

        @Override
        public Object findFilterId(MapperConfig<?> config, Annotated ann) {
            return (Object) values.get("findFilterId");
        }
        
        @Override
        public Object findNamingStrategy(MapperConfig<?> config, AnnotatedClass ac) {
            return (Object) values.get("findNamingStrategy");
        }

        @Override
        public String findClassDescription(MapperConfig<?> config, AnnotatedClass ac) {
            return (String) values.get("findClassDescription");
        }

        /*
        /******************************************************
        /* Property auto-detection
        /******************************************************
        */

        @Override
        public VisibilityChecker findAutoDetectVisibility(MapperConfig<?> config,
                AnnotatedClass ac, VisibilityChecker checker)
        {
            VisibilityChecker vc = (VisibilityChecker) values.get("findAutoDetectVisibility");
            // not really good but:
            return (vc == null) ? checker : vc;
        }

        /*
        /******************************************************
        /* Type handling
        /******************************************************
         */

        @SuppressWarnings("unchecked")
        @Override
        public List<NamedType> findSubtypes(MapperConfig<?> config, Annotated a)
        {
            return (List<NamedType>) values.get("findSubtypes");
        }

        @Override
        public String findTypeName(MapperConfig<?> config, AnnotatedClass ac) {
            return (String) values.get("findTypeName");
        }

        /*
        /******************************************************
        /* Serialization introspection
        /******************************************************
         */

        @Override
        public Boolean hasAsKey(MapperConfig<?> config, Annotated a) {
            return (Boolean) values.get("hasAsKey");
        }

        @Override
        public Boolean hasAsValue(MapperConfig<?> config, Annotated a) {
            return (Boolean) values.get("hasAsValue");
        }

        @Override
        public Boolean hasAnyGetter(MapperConfig<?> config, Annotated ann) {
            return (Boolean) values.get("hasAnyGetter");
        }

        /*
        /******************************************************
        /* Deserialization introspection
        /******************************************************
         */

        @Override
        public Boolean hasAnySetter(MapperConfig<?> config, Annotated a) {
            return (Boolean) values.get("hasAnySetter");
        }

        /*
        /******************************************************
        /* Helper methods
        /******************************************************
         */        

        private boolean _boolean(String key) {
            Object ob = values.get(key);
            return Boolean.TRUE.equals(ob);
        }
    }

    /*
    /**********************************************************
    /* Test methods, misc
    /**********************************************************
     */

    private final AnnotationIntrospector NO_ANNOTATIONS = AnnotationIntrospector.nopInstance();

    public void testVersion() throws Exception
    {
        Version v = new Version(1, 2, 3, null,
                "com.fasterxml", "IntrospectorPairTest");
        IntrospectorWithMap withVersion = new IntrospectorWithMap()
                .version(v);
        assertEquals(v,
                new AnnotationIntrospectorPair(withVersion, NO_ANNOTATIONS).version());
        IntrospectorWithMap noVersion = new IntrospectorWithMap();
        assertEquals(Version.unknownVersion(),
                new AnnotationIntrospectorPair(noVersion, withVersion).version());
    }

    public void testAccess() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap();
        AnnotationIntrospectorPair pair = new AnnotationIntrospectorPair(intr1,
                NO_ANNOTATIONS);
        Collection<AnnotationIntrospector> intrs = pair.allIntrospectors();
        assertEquals(2, intrs.size());
        Iterator<AnnotationIntrospector> it = intrs.iterator();
        assertSame(intr1, it.next());
        assertSame(NO_ANNOTATIONS, it.next());
    }

    public void testAnnotationBundle() throws Exception
    {
        IntrospectorWithMap isBundle = new IntrospectorWithMap()
                .add("isAnnotationBundle", true);
        assertTrue(new AnnotationIntrospectorPair(NO_ANNOTATIONS, isBundle)
                .isAnnotationBundle(null));
        assertTrue(new AnnotationIntrospectorPair(isBundle, NO_ANNOTATIONS)
                .isAnnotationBundle(null));
        assertFalse(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .isAnnotationBundle(null));
    }

    /*
    /**********************************************************
    /* Test methods, general class annotations
    /**********************************************************
     */

    public void testFindRootName() throws Exception
    {
        PropertyName name = new PropertyName("test");
        IntrospectorWithMap intr = new IntrospectorWithMap()
                .add("findRootName", name);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findRootName(null, null));
        assertEquals(name, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr).findRootName(null, null));
        assertEquals(name, new AnnotationIntrospectorPair(intr, NO_ANNOTATIONS).findRootName(null, null));
    }

    public void testPropertyIgnorals() throws Exception
    {
        JsonIgnoreProperties.Value incl = JsonIgnoreProperties.Value.forIgnoredProperties("foo");
        IntrospectorWithMap intr = new IntrospectorWithMap()
                .add("findPropertyIgnoralByName", incl);
        IntrospectorWithMap intrEmpty = new IntrospectorWithMap()
                .add("findPropertyIgnoralByName", JsonIgnoreProperties.Value.empty());
        assertEquals(JsonIgnoreProperties.Value.empty(),
                new AnnotationIntrospectorPair(intrEmpty, intrEmpty).findPropertyIgnoralByName(null, null));
        // should actually verify inclusion combining, but there are separate tests for that
        assertEquals(incl, new AnnotationIntrospectorPair(intrEmpty, intr).findPropertyIgnoralByName(null, null));
        assertEquals(incl, new AnnotationIntrospectorPair(intr, intrEmpty).findPropertyIgnoralByName(null, null));
    }

    public void testIsIgnorableType() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("isIgnorableType", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("isIgnorableType", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).isIgnorableType(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2).isIgnorableType(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1).isIgnorableType(null, null));
    }

    public void testFindFilterId() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findFilterId", "a");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findFilterId", "b");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findFilterId(null, null));
        assertEquals("a", new AnnotationIntrospectorPair(intr1, intr2).findFilterId(null, null));
        assertEquals("b", new AnnotationIntrospectorPair(intr2, intr1).findFilterId(null, null));
    }

    public void testFindNamingStrategy() throws Exception
    {
        // shouldn't be bogus Classes for real use, but works here
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findNamingStrategy", Integer.class);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findNamingStrategy", String.class);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findNamingStrategy(null, null));
        assertEquals(Integer.class,
                new AnnotationIntrospectorPair(intr1, intr2).findNamingStrategy(null, null));
        assertEquals(String.class,
                new AnnotationIntrospectorPair(intr2, intr1).findNamingStrategy(null, null));
    }

    public void testFindClassDescription() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findClassDescription", "Desc1");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findClassDescription", "Desc2");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findClassDescription(null, null));
        assertEquals("Desc1",
                new AnnotationIntrospectorPair(intr1, intr2).findClassDescription(null, null));
        assertEquals("Desc2",
                new AnnotationIntrospectorPair(intr2, intr1).findClassDescription(null, null));
    }

    // // // 3 deprecated methods, skip

    /*
    /**********************************************************
    /* Test methods, ser/deser
    /**********************************************************
     */

    public void testFindSerializer() throws Exception
    {
        final ValueSerializer<?> serString = new StringSerializer();
        final ValueSerializer<?> serToString = ToStringSerializer.instance;

        AnnotationIntrospector intr1 = new IntrospectorWithHandlers(null, serString);
        AnnotationIntrospector intr2 = new IntrospectorWithHandlers(null, serToString);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        AnnotationIntrospector nop2 = new IntrospectorWithHandlers(null, ValueSerializer.None.class);

        assertSame(serString,
                new AnnotationIntrospectorPair(intr1, intr2).findSerializer(null, null));
        assertSame(serToString,
                new AnnotationIntrospectorPair(intr2, intr1).findSerializer(null, null));

        // also: no-op instance should not block real one, regardless
        assertSame(serString,
                new AnnotationIntrospectorPair(nop, intr1).findSerializer(null, null));
        assertSame(serString,
                new AnnotationIntrospectorPair(nop2, intr1).findSerializer(null, null));

        // nor should no-op result in non-null result
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findSerializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findSerializer(null, null));
    }

    public void testHasAsValue() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAsValue", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAsValue", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .hasAsValue(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .hasAsValue(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .hasAsValue(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, NO_ANNOTATIONS)
                .hasAsValue(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2)
                .hasAsValue(null, null));

        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2)
                .hasAsValue(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1)
                .hasAsValue(null, null));
    }

    public void testHasAsKey() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAsKey", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAsKey", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .hasAsKey(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .hasAsKey(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .hasAsKey(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, NO_ANNOTATIONS)
                .hasAsKey(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2)
                .hasAsKey(null, null));

        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2)
                .hasAsKey(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1)
                .hasAsKey(null, null));
    }

    public void testHasAnyGetter() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAnyGetter", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAnyGetter", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, NO_ANNOTATIONS)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2)
                .hasAnyGetter(null, null));

        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1)
                .hasAnyGetter(null, null));
    }

    /*
    /**********************************************************
    /* Test methods, deser
    /**********************************************************
     */

    public void testFindDeserializer() throws Exception
    {
        final ValueDeserializer<?> deserString = StringDeserializer.instance;
        final ValueDeserializer<?> deserBoolean = NumberDeserializers.find(Boolean.TYPE);

        AnnotationIntrospector intr1 = new IntrospectorWithHandlers(deserString, null);
        AnnotationIntrospector intr2 = new IntrospectorWithHandlers(deserBoolean, null);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        AnnotationIntrospector nop2 = new IntrospectorWithHandlers(ValueDeserializer.None.class, null);

        assertSame(deserString,
                new AnnotationIntrospectorPair(intr1, intr2).findDeserializer(null, null));
        assertSame(deserBoolean,
                new AnnotationIntrospectorPair(intr2, intr1).findDeserializer(null, null));
        // also: no-op instance should not block real one, regardless
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop, intr1).findDeserializer(null, null));
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop2, intr1).findDeserializer(null, null));

        // nor should no-op result in non-null result
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findDeserializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findDeserializer(null, null));
    }

    /*
    /******************************************************
    /* Property auto-detection
    /******************************************************
     */

    public void testFindAutoDetectVisibility() throws Exception
    {
        VisibilityChecker vc = VisibilityChecker.defaultInstance();
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findAutoDetectVisibility", vc);
        SerializationConfig config = null;
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findAutoDetectVisibility(config, null, null));
        assertSame(vc, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .findAutoDetectVisibility(config, null, null));
        assertSame(vc, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .findAutoDetectVisibility(config, null, null));
    }

    /*
    /******************************************************
    /* Type handling
    /******************************************************
     */

    public void testFindTypeResolver() throws Exception
    {
        /*
        TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config,
            AnnotatedClass ac, JavaType baseType)
        return (TypeResolverBuilder<?>) values.get("findTypeResolver");
        */
    }
    public void testFindPropertyTypeResolver() {
    }

    public void testFindPropertyContentTypeResolver() {
    }

    public void testFindSubtypes() {
    }

    public void testFindTypeName() {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findTypeName", "type1");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findTypeName", "type2");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findTypeName(null, null));
        assertEquals("type1",
                new AnnotationIntrospectorPair(intr1, intr2).findTypeName(null, null));
        assertEquals("type2",
                new AnnotationIntrospectorPair(intr2, intr1).findTypeName(null, null));
    }

    /*
    /******************************************************
    /* Deserialization introspection
    /******************************************************
     */

    // for [databind#1672]
    public void testHasAnySetter() {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAnySetter", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAnySetter", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).hasAnySetter(null, null));
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(intr1, intr2).hasAnySetter(null, null));
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1).hasAnySetter(null, null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(intr2, intr1).hasAnySetter(null, null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).hasAnySetter(null, null));
    }

    /*
    /**********************************************************
    /* Test methods, others
    /**********************************************************
     */

    private final AnnotationIntrospectorPair introPair12
        = new AnnotationIntrospectorPair(new Introspector1(), new Introspector2());

    private final AnnotationIntrospectorPair introPair21
        = new AnnotationIntrospectorPair(new Introspector2(), new Introspector1());

    // for [databind#1025]
    public void testInclusionMerging() throws Exception
    {
        // argument is ignored by test introspectors, may be null
        JsonInclude.Value v12 = introPair12.findPropertyInclusion(null, null);
        JsonInclude.Value v21 = introPair21.findPropertyInclusion(null, null);

        assertEquals(JsonInclude.Include.ALWAYS, v12.getContentInclusion());
        assertEquals(JsonInclude.Include.NON_ABSENT, v12.getValueInclusion());

        assertEquals(JsonInclude.Include.NON_EMPTY, v21.getContentInclusion());
        assertEquals(JsonInclude.Include.NON_ABSENT, v21.getValueInclusion());
    }

    /*
    /**********************************************************
    /* Introspectors and test for [jackson-modules-base#134]/[databind#962]
    /**********************************************************
     */
    static class TestIntrospector extends NopAnnotationIntrospector {
        @Override
        public JacksonInject.Value findInjectableValue(MapperConfig<?> config,
                AnnotatedMember m) {
            if (m.getRawType() == UnreadableBean.class) {
                return JacksonInject.Value.forId("jjj");
            }
            return null;
        }
    }

    static class TestInjector extends InjectableValues {
        @Override
        public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) {
            if (valueId == "jjj") {
                UnreadableBean bean = new UnreadableBean();
                bean.setValue(1);
                return bean;
            }
            return null;
        }

        @Override
        public InjectableValues snapshot() {
            return this;
        }
    }

    enum SimpleEnum { ONE, TWO }

    static class UnreadableBean {
        public SimpleEnum value;

        public void setValue(SimpleEnum value) {
            this.value = value;
        }

        public void setValue(Integer intValue) {
            this.value = SimpleEnum.values()[intValue];
        }

        public SimpleEnum getValue() {
            return value;
        }
    }

    static class ReadableInjectedBean {
        public ReadableInjectedBean(@JacksonInject(useInput = OptBoolean.FALSE) UnreadableBean injectBean) {
            this.injectBean = injectBean;
        }
        @JsonProperty
        String foo;
        @JsonIgnore
        UnreadableBean injectBean;
    }

    static class UnreadableInjectedBean {
        public UnreadableInjectedBean(@JacksonInject UnreadableBean injectBean) {
            this.injectBean = injectBean;
        }
        @JsonProperty
        private String foo;
        @JsonIgnore
        private UnreadableBean injectBean;
    }

    public void testMergingIntrospectorsForInjection() throws Exception {
        AnnotationIntrospector testIntrospector = new TestIntrospector();
        ObjectMapper mapper = JsonMapper.builder()
                .injectableValues(new TestInjector())
                .annotationIntrospector(new AnnotationIntrospectorPair(testIntrospector,
                        new JacksonAnnotationIntrospector()))
                .build();
        ReadableInjectedBean bean = mapper.readValue("{\"foo\": \"bob\"}", ReadableInjectedBean.class);
        assertEquals("bob", bean.foo);
        assertEquals(SimpleEnum.TWO, bean.injectBean.value);

        boolean successReadingUnreadableInjectedBean;
        try {
            /*UnreadableInjectedBean noBean =*/ mapper.readValue("{\"foo\": \"bob\"}", UnreadableInjectedBean.class);
            successReadingUnreadableInjectedBean = true;
        } catch (DatabindException e) {
            successReadingUnreadableInjectedBean = false;
            assertTrue(e.getMessage().contains("Conflicting setter definitions"));
        }
        assertFalse(successReadingUnreadableInjectedBean);
    }
}
