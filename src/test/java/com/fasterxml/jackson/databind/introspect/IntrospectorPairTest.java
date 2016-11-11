package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;

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

    private final IntrospectorWithMap NO_ANNOTATIONS = new IntrospectorWithMap();

    public void testVersion() throws Exception
    {
        Version v = new Version(1, 2, 3, null,
                "com.fasterxml", "IntrospectorPairTest");
        IntrospectorWithMap withVersion = new IntrospectorWithMap()
                .version(v);
        assertEquals(v,
                new AnnotationIntrospectorPair(withVersion, NO_ANNOTATIONS).version());
        assertEquals(Version.unknownVersion(),
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, withVersion).version());
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
