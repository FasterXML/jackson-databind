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

public class BasicClassIntrospector
    extends ClassIntrospector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /* We keep a small set of pre-constructed descriptions to use for
     * common non-structured values, such as Numbers and Strings.
     * This is strictly performance optimization to reduce what is
     * usually one-time cost, but seems useful for some cases considering
     * simplicity.
     */
    protected final static BasicBeanDescription STRING_DESC;
    static {
        STRING_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(String.class),
                AnnotatedClassResolver.createPrimordial(String.class));
    }
    protected final static BasicBeanDescription BOOLEAN_DESC;
    static {
        BOOLEAN_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Boolean.TYPE),
                AnnotatedClassResolver.createPrimordial(Boolean.TYPE));
    }
    protected final static BasicBeanDescription INT_DESC;
    static {
        INT_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Integer.TYPE),
                AnnotatedClassResolver.createPrimordial(Integer.TYPE));
    }
    protected final static BasicBeanDescription LONG_DESC;
    static {
        LONG_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Long.TYPE),
                AnnotatedClassResolver.createPrimordial(Long.TYPE));
    }

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public BasicClassIntrospector() {
    }

    @Override
    public ClassIntrospector forMapper(Object mapper) {
        return new BasicClassIntrospector();
    }

    /*
    /**********************************************************************
    /* Factory method impls: annotation resolution
    /**********************************************************************
     */

    @Override
    public BasicBeanDescription forClassAnnotations(MapperConfig<?> config,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            desc = BasicBeanDescription.forOtherUse(config, type,
                    _resolveAnnotatedClass(config, type, r));
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDirectClassAnnotations(MapperConfig<?> config,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            desc = BasicBeanDescription.forOtherUse(config, type,
                    _resolveAnnotatedWithoutSuperTypes(config, type, r));
        }
        return desc;
    }

    /*
    /**********************************************************************
    /* Factory method impls: bean introspection
    /**********************************************************************
     */

    @Override
    public BasicBeanDescription forSerialization(SerializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(cfg, type);
            if (desc == null) {
                desc = BasicBeanDescription.forSerialization(collectProperties(cfg,
                        type, r, true, "set"));
            }
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
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserializationWithBuilder(DeserializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        // no std JDK types with Builders, so:
        return BasicBeanDescription.forDeserialization(collectPropertiesWithBuilder(cfg,
                type, r, false));
    }
    
    @Override
    public BasicBeanDescription forCreation(DeserializationConfig cfg,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(cfg, type);
            if (desc == null) {
                desc = BasicBeanDescription.forDeserialization(
            		collectProperties(cfg, type, r, false, "set"));
            }
        }
        return desc;
    }

    /*
    /**********************************************************************
    /* Overridable helper methods
    /**********************************************************************
     */

    protected POJOPropertiesCollector collectProperties(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization,
            String mutatorPrefix)
    {
        return constructPropertyCollector(config,
                _resolveAnnotatedClass(config, type, r),
                type, forSerialization, mutatorPrefix);
    }

    protected POJOPropertiesCollector collectPropertiesWithBuilder(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization)
    {
        AnnotatedClass ac = _resolveAnnotatedClass(config, type, r);
        AnnotationIntrospector ai = config.isAnnotationProcessingEnabled() ? config.getAnnotationIntrospector() : null;
        JsonPOJOBuilder.Value builderConfig = (ai == null) ? null
                : ai.findPOJOBuilderConfig(config, ac);
        String mutatorPrefix = (builderConfig == null) ? JsonPOJOBuilder.DEFAULT_WITH_PREFIX : builderConfig.withPrefix;
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
        if (ClassUtil.isJDKClass(raw)) {
            // 23-Sep-2014, tatu: Should we be conservative here (minimal number
            //    of matches), or ambitious? Let's do latter for now.
            if (Collection.class.isAssignableFrom(raw)
                    || Map.class.isAssignableFrom(raw)) {
                return true;
            }
        }
        return false;
    }

    protected BasicBeanDescription _findStdJdkCollectionDesc(MapperConfig<?> cfg, JavaType type)
    {
        if (_isStdJDKCollection(type)) {
            return BasicBeanDescription.forOtherUse(cfg, type,
                    _resolveAnnotatedClass(cfg, type, cfg));
        }
        return null;
    }

    protected AnnotatedClass _resolveAnnotatedClass(MapperConfig<?> config,
            JavaType type, MixInResolver r) {
        return AnnotatedClassResolver.resolve(config, type, r);
    }

    protected AnnotatedClass _resolveAnnotatedWithoutSuperTypes(MapperConfig<?> config,
            JavaType type, MixInResolver r) {
        return AnnotatedClassResolver.resolveWithoutSuperTypes(config, type, r);
    }
}
