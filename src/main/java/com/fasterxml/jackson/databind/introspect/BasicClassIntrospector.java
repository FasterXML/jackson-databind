package com.fasterxml.jackson.databind.introspect;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.LRUMap;

public class BasicClassIntrospector
    extends ClassIntrospector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /* We keep a small set of pre-constructed descriptions to use for
     * common non-structured values, such as Numbers and Strings.
     * This is strictly performance optimization to reduce what is
     * usually one-time cost, but seems useful for some cases considering
     * simplicity.
     * 
     * @since 2.4
     */
    
    protected final static BasicBeanDescription STRING_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(String.class, null);
        STRING_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(String.class), ac);
    }
    protected final static BasicBeanDescription BOOLEAN_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(Boolean.TYPE, null);
        BOOLEAN_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Boolean.TYPE), ac);
    }
    protected final static BasicBeanDescription INT_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(Integer.TYPE, null);
        INT_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Integer.TYPE), ac);
    }
    protected final static BasicBeanDescription LONG_DESC;
    static {
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(Long.TYPE, null);
        LONG_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Long.TYPE), ac);
    }

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    @Deprecated // since 2.5: construct instance directly
    public final static BasicClassIntrospector instance = new BasicClassIntrospector();

    /**
     * Looks like 'forClassAnnotations()' gets called so frequently that we
     * should consider caching to avoid some of the lookups.
     * 
     * @since 2.5
     */
    protected final LRUMap<JavaType,BasicBeanDescription> _cachedFCA;

    public BasicClassIntrospector() {
        // a small cache should go a long way here
        _cachedFCA = new LRUMap<JavaType,BasicBeanDescription>(16, 64);
    }
    
    /*
    /**********************************************************
    /* Factory method impls
    /**********************************************************
     */

    @Override
    public BasicBeanDescription forSerialization(SerializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            // As per [Databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(cfg, type);
            if (desc == null) {
                desc = BasicBeanDescription.forSerialization(collectProperties(cfg,
                        type, r, true, "set"));
            }
            // Also: this is a superset of "forClassAnnotations", so may optimize by optional add:
            _cachedFCA.putIfAbsent(type, desc);
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserialization(DeserializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            // As per [Databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(cfg, type);
            if (desc == null) {
                desc = BasicBeanDescription.forDeserialization(collectProperties(cfg,
                        		type, r, false, "set"));
            }
            // Also: this is a superset of "forClassAnnotations", so may optimize by optional add:
            _cachedFCA.putIfAbsent(type, desc);
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserializationWithBuilder(DeserializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        // no std JDK types with Builders, so:

        BasicBeanDescription desc = BasicBeanDescription.forDeserialization(collectPropertiesWithBuilder(cfg,
                type, r, false));
        // this is still a superset of "forClassAnnotations", so may optimize by optional add:
        _cachedFCA.putIfAbsent(type, desc);
        return desc;
    }
    
    @Override
    public BasicBeanDescription forCreation(DeserializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {

            // As per [Databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(cfg, type);
            if (desc == null) {
                desc = BasicBeanDescription.forDeserialization(
            		collectProperties(cfg, type, r, false, "set"));
            }
        }
        // should this be cached for FCA?
        return desc;
    }

    @Override
    public BasicBeanDescription forClassAnnotations(MapperConfig<?> config,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            desc = _cachedFCA.get(type);
            if (desc == null) {
                AnnotatedClass ac = AnnotatedClass.construct(type, config, r);
                desc = BasicBeanDescription.forOtherUse(config, type, ac);
                _cachedFCA.put(type, desc);
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDirectClassAnnotations(MapperConfig<?> config,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(type.getRawClass(), config, r);
            desc = BasicBeanDescription.forOtherUse(config, type, ac);
        }
        return desc;
    }
    
    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    protected POJOPropertiesCollector collectProperties(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization,
            String mutatorPrefix)
    {
        AnnotatedClass ac = AnnotatedClass.construct(type, config, r);
        return constructPropertyCollector(config, ac, type, forSerialization, mutatorPrefix);
    }
    
    protected POJOPropertiesCollector collectPropertiesWithBuilder(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization)
    {
        boolean useAnnotations = config.isAnnotationProcessingEnabled();
        AnnotationIntrospector ai = useAnnotations ? config.getAnnotationIntrospector() : null;
        AnnotatedClass ac = AnnotatedClass.construct(type, config, r);
        JsonPOJOBuilder.Value builderConfig = (ai == null) ? null : ai.findPOJOBuilderConfig(ac);
        String mutatorPrefix = (builderConfig == null) ? "with" : builderConfig.withPrefix;
        return constructPropertyCollector(config, ac, type, forSerialization, mutatorPrefix);
    }

    /**
     * Overridable method called for creating {@link POJOPropertiesCollector} instance
     * to use; override is needed if a custom sub-class is to be used.
     */
    protected POJOPropertiesCollector constructPropertyCollector(MapperConfig<?> config,
            AnnotatedClass ac, JavaType type, boolean forSerialization, String mutatorPrefix)
    {
        return new POJOPropertiesCollector(config, forSerialization, type, ac, mutatorPrefix);
    }
    
    /**
     * Method called to see if type is one of core JDK types
     * that we have cached for efficiency.
     */
    protected BasicBeanDescription _findStdTypeDesc(JavaType type)
    {
        Class<?> cls = type.getRawClass();
        if (cls.isPrimitive()) {
            if (cls == Boolean.TYPE) {
                return BOOLEAN_DESC;
            }
            if (cls == Integer.TYPE) {
                return INT_DESC;
            }
            if (cls == Long.TYPE) {
                return LONG_DESC;
            }
        } else {
            if (cls == String.class) {
                return STRING_DESC;
            }
        }
        return null;
    }

    /**
     * Helper method used to decide whether we can omit introspection
     * for members (methods, fields, constructors); we may do so for
     * a limited number of container types JDK provides.
     */
    protected boolean _isStdJDKCollection(JavaType type)
    {
        if (!type.isContainerType() || type.isArrayType()) {
            return false;
        }
        Class<?> raw = type.getRawClass();
        String pkgName = ClassUtil.getPackageName(raw);
        if (pkgName != null) {
            if (pkgName.startsWith("java.lang")
                    || pkgName.startsWith("java.util")) {
                /* 23-Sep-2014, tatu: Should we be conservative here (minimal number
                 *    of matches), or ambitious? Let's do latter for now.
                 */
                if (Collection.class.isAssignableFrom(raw)
                        || Map.class.isAssignableFrom(raw)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected BasicBeanDescription _findStdJdkCollectionDesc(MapperConfig<?> cfg, JavaType type)
    {
        if (_isStdJDKCollection(type)) {
            AnnotatedClass ac = AnnotatedClass.construct(type, cfg);
            return BasicBeanDescription.forOtherUse(cfg, type, ac);
        }
        return null;
    }
}
