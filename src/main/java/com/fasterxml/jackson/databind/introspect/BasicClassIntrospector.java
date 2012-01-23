package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.SimpleType;

public class BasicClassIntrospector
    extends ClassIntrospector<BasicBeanDescription>
{
    /* We keep a small set of pre-constructed descriptions to use for
     * common non-structured values, such as Numbers and Strings.
     * This is strictly performance optimization to reduce what is
     * usually one-time cost, but seems useful for some cases considering
     * simplicity.
     */
    
    protected final static BasicBeanDescription STRING_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(String.class, null, null);
        STRING_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(String.class), ac);
    }
    protected final static BasicBeanDescription BOOLEAN_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(Boolean.TYPE, null, null);
        BOOLEAN_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Boolean.TYPE), ac);
    }
    protected final static BasicBeanDescription INT_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(Integer.TYPE, null, null);
        INT_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Integer.TYPE), ac);
    }
    protected final static BasicBeanDescription LONG_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(Long.TYPE, null, null);
        LONG_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Long.TYPE), ac);
    }
    
    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    public final static BasicClassIntrospector instance = new BasicClassIntrospector();

    public BasicClassIntrospector() { }
    
    /*
    /**********************************************************
    /* Factory method impls
    /**********************************************************
     */

    @Override
    public BasicBeanDescription forSerialization(SerializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        // minor optimization: for JDK types do minimal introspection
        BasicBeanDescription desc = _findCachedDesc(type);
        if (desc == null) {
            desc = BasicBeanDescription.forSerialization(collectProperties(cfg, type, r, true));
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserialization(DeserializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        // minor optimization: for JDK types do minimal introspection
        BasicBeanDescription desc = _findCachedDesc(type);
        if (desc == null) {
            desc = BasicBeanDescription.forDeserialization(collectProperties(cfg, type, r, false));
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forCreation(DeserializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findCachedDesc(type);
        if (desc == null) {
            desc = BasicBeanDescription.forDeserialization(collectProperties(cfg, type, r, false));
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forClassAnnotations(MapperConfig<?> cfg,
            JavaType type, MixInResolver r)
    {
        boolean useAnnotations = cfg.isAnnotationProcessingEnabled();
        AnnotationIntrospector ai =  cfg.getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.construct(type.getRawClass(), (useAnnotations ? ai : null), r);
        return BasicBeanDescription.forOtherUse(cfg, type, ac);
    }

    @Override
    public BasicBeanDescription forDirectClassAnnotations(MapperConfig<?> cfg,
            JavaType type, MixInResolver r)
    {
        boolean useAnnotations = cfg.isAnnotationProcessingEnabled();
        AnnotationIntrospector ai =  cfg.getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(type.getRawClass(),
                (useAnnotations ? ai : null), r);
        return BasicBeanDescription.forOtherUse(cfg, type, ac);
    }
    
    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    public POJOPropertiesCollector collectProperties(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization)
    {
        boolean useAnnotations = config.isAnnotationProcessingEnabled();
        AnnotatedClass ac = AnnotatedClass.construct(type.getRawClass(),
                (useAnnotations ? config.getAnnotationIntrospector() : null), r);
        return constructPropertyCollector(config, ac, type, forSerialization).collect();
    }

    /**
     * Overridable method called for creating {@link POJOPropertiesCollector} instance
     * to use; override is needed if a custom sub-class is to be used.
     */
    protected POJOPropertiesCollector constructPropertyCollector(MapperConfig<?> config,
            AnnotatedClass ac, JavaType type,
            boolean forSerialization)
    {
        return new POJOPropertiesCollector(config, forSerialization, type, ac);
    }
    
    /**
     * Method called to see if type is one of core JDK types
     * that we have cached for efficiency.
     */
    protected BasicBeanDescription _findCachedDesc(JavaType type)
    {
        Class<?> cls = type.getRawClass();
        if (cls == String.class) {
            return STRING_DESC;
        }
        if (cls == Boolean.TYPE) {
            return BOOLEAN_DESC;
        }
        if (cls == Integer.TYPE) {
            return INT_DESC;
        }
        if (cls == Long.TYPE) {
            return LONG_DESC;
        }
        return null;
    }
}
