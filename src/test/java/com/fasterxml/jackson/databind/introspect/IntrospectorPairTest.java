package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
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
        public JsonInclude.Value findPropertyInclusion(Annotated a) {
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
        public JsonInclude.Value findPropertyInclusion(Annotated a) {
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
        public Object findDeserializer(Annotated am) {
            return _deserializer;
        }

        @Override
        public Object findSerializer(Annotated am) {
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
        public JsonInclude.Value findPropertyInclusion(Annotated a) {
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
        public PropertyName findRootName(AnnotatedClass ac) {
            return (PropertyName) values.get("findRootName");
        }

        @Override
        public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated a) {
            return (JsonIgnoreProperties.Value) values.get("findPropertyIgnorals");
        }

        @Override
        public Boolean isIgnorableType(AnnotatedClass ac) {
            return (Boolean) values.get("isIgnorableType");
        }

        @Override
        public Object findFilterId(Annotated ann) {
            return (Object) values.get("findFilterId");
        }
        
        @Override
        public Object findNamingStrategy(AnnotatedClass ac) {
            return (Object) values.get("findNamingStrategy");
        }

        @Override
        public String findClassDescription(AnnotatedClass ac) {
            return (String) values.get("findClassDescription");
        }

        /*
        /******************************************************
        /* Property auto-detection
        /******************************************************
        */

        @Override
        public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
            VisibilityChecker<?> checker)
        {
            VisibilityChecker<?> vc = (VisibilityChecker<?>) values.get("findAutoDetectVisibility");
            // not really good but:
            return (vc == null) ? checker : vc;
        }

        /*
        /******************************************************
        /* Type handling
        /******************************************************
         */

        @Override
        public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config,
                AnnotatedClass ac, JavaType baseType)
        {
            return (TypeResolverBuilder<?>) values.get("findTypeResolver");
        }

        @Override
        public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config,
                AnnotatedMember am, JavaType baseType)
        {
            return (TypeResolverBuilder<?>) values.get("findPropertyTypeResolver");
        }

        @Override
        public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config,
                AnnotatedMember am, JavaType baseType)
        {
            return (TypeResolverBuilder<?>) values.get("findPropertyContentTypeResolver");
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public List<NamedType> findSubtypes(Annotated a)
        {
            return (List<NamedType>) values.get("findSubtypes");
        }

        @Override
        public String findTypeName(AnnotatedClass ac) {
            return (String) values.get("findTypeName");
        }

        /*
        /******************************************************
        /* Deserialization introspection
        /******************************************************
         */

        @Override
        public Boolean hasAnySetter(Annotated a) {
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
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findRootName(null));
        assertEquals(name, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr).findRootName(null));
        assertEquals(name, new AnnotationIntrospectorPair(intr, NO_ANNOTATIONS).findRootName(null));
    }

    public void testPropertyIgnorals() throws Exception
    {
        JsonIgnoreProperties.Value incl = JsonIgnoreProperties.Value.forIgnoredProperties("foo");
        IntrospectorWithMap intr = new IntrospectorWithMap()
                .add("findPropertyIgnorals", incl);
        IntrospectorWithMap intrEmpty = new IntrospectorWithMap()
                .add("findPropertyIgnorals", JsonIgnoreProperties.Value.empty());
        assertEquals(JsonIgnoreProperties.Value.empty(),
                new AnnotationIntrospectorPair(intrEmpty, intrEmpty).findPropertyIgnorals(null));
        // should actually verify inclusion combining, but there are separate tests for that
        assertEquals(incl, new AnnotationIntrospectorPair(intrEmpty, intr).findPropertyIgnorals(null));
        assertEquals(incl, new AnnotationIntrospectorPair(intr, intrEmpty).findPropertyIgnorals(null));
    }

    public void testIsIgnorableType() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("isIgnorableType", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("isIgnorableType", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).isIgnorableType(null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2).isIgnorableType(null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1).isIgnorableType(null));
    }

    public void testFindFilterId() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findFilterId", "a");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findFilterId", "b");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findFilterId(null));
        assertEquals("a", new AnnotationIntrospectorPair(intr1, intr2).findFilterId(null));
        assertEquals("b", new AnnotationIntrospectorPair(intr2, intr1).findFilterId(null));
    }

    public void testFindNamingStrategy() throws Exception
    {
        // shouldn't be bogus Classes for real use, but works here
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findNamingStrategy", Integer.class);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findNamingStrategy", String.class);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findNamingStrategy(null));
        assertEquals(Integer.class,
                new AnnotationIntrospectorPair(intr1, intr2).findNamingStrategy(null));
        assertEquals(String.class,
                new AnnotationIntrospectorPair(intr2, intr1).findNamingStrategy(null));
    }

    public void testFindClassDescription() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findClassDescription", "Desc1");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findClassDescription", "Desc2");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findClassDescription(null));
        assertEquals("Desc1",
                new AnnotationIntrospectorPair(intr1, intr2).findClassDescription(null));
        assertEquals("Desc2",
                new AnnotationIntrospectorPair(intr2, intr1).findClassDescription(null));
    }

    // // // 3 deprecated methods, skip

    /*
    /**********************************************************
    /* Test methods, ser/deser
    /**********************************************************
     */

    public void testFindSerializer() throws Exception
    {
        final JsonSerializer<?> serString = new StringSerializer();
        final JsonSerializer<?> serToString = ToStringSerializer.instance;

        AnnotationIntrospector intr1 = new IntrospectorWithHandlers(null, serString);
        AnnotationIntrospector intr2 = new IntrospectorWithHandlers(null, serToString);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        AnnotationIntrospector nop2 = new IntrospectorWithHandlers(null, JsonSerializer.None.class);

        assertSame(serString,
                new AnnotationIntrospectorPair(intr1, intr2).findSerializer(null));
        assertSame(serToString,
                new AnnotationIntrospectorPair(intr2, intr1).findSerializer(null));

        // also: no-op instance should not block real one, regardless
        assertSame(serString,
                new AnnotationIntrospectorPair(nop, intr1).findSerializer(null));
        assertSame(serString,
                new AnnotationIntrospectorPair(nop2, intr1).findSerializer(null));

        // nor should no-op result in non-null result
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findSerializer(null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findSerializer(null));
    }

    public void testFindDeserializer() throws Exception
    {
        final JsonDeserializer<?> deserString = StringDeserializer.instance;
        final JsonDeserializer<?> deserObject = UntypedObjectDeserializer.Vanilla.std;

        AnnotationIntrospector intr1 = new IntrospectorWithHandlers(deserString, null);
        AnnotationIntrospector intr2 = new IntrospectorWithHandlers(deserObject, null);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        AnnotationIntrospector nop2 = new IntrospectorWithHandlers(JsonDeserializer.None.class, null);

        assertSame(deserString,
                new AnnotationIntrospectorPair(intr1, intr2).findDeserializer(null));
        assertSame(deserObject,
                new AnnotationIntrospectorPair(intr2, intr1).findDeserializer(null));
        // also: no-op instance should not block real one, regardless
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop, intr1).findDeserializer(null));
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop2, intr1).findDeserializer(null));

        // nor should no-op result in non-null result
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findDeserializer(null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findDeserializer(null));
    }

    /*
    /******************************************************
    /* Property auto-detection
    /******************************************************
     */

    public void testFindAutoDetectVisibility() throws Exception
    {
        VisibilityChecker<?> vc = VisibilityChecker.Std.defaultInstance();
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findAutoDetectVisibility", vc);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findAutoDetectVisibility(null, null));
        assertSame(vc, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .findAutoDetectVisibility(null, null));
        assertSame(vc, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .findAutoDetectVisibility(null, null));
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
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findTypeName(null));
        assertEquals("type1",
                new AnnotationIntrospectorPair(intr1, intr2).findTypeName(null));
        assertEquals("type2",
                new AnnotationIntrospectorPair(intr2, intr1).findTypeName(null));
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
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).hasAnySetter(null));
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(intr1, intr2).hasAnySetter(null));
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1).hasAnySetter(null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(intr2, intr1).hasAnySetter(null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).hasAnySetter(null));
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
        JsonInclude.Value v12 = introPair12.findPropertyInclusion(null);
        JsonInclude.Value v21 = introPair21.findPropertyInclusion(null);

        assertEquals(JsonInclude.Include.ALWAYS, v12.getContentInclusion());
        assertEquals(JsonInclude.Include.NON_ABSENT, v12.getValueInclusion());

        assertEquals(JsonInclude.Include.NON_EMPTY, v21.getContentInclusion());
        assertEquals(JsonInclude.Include.NON_ABSENT, v21.getValueInclusion());
    }
}
