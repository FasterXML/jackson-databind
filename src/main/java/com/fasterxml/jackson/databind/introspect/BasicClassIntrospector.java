package com.fasterxml.jackson.databind.introspect;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class BasicClassIntrospector
    extends ClassIntrospector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 2L;

    private final static Class<?> CLS_OBJECT = Object.class;
    private final static Class<?> CLS_STRING = String.class;
    private final static Class<?> CLS_JSON_NODE = JsonNode.class;

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
        STRING_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(String.class),
                AnnotatedClassResolver.createPrimordial(CLS_STRING));
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
    protected final static BasicBeanDescription OBJECT_DESC;
    static {
        OBJECT_DESC = BasicBeanDescription.forOtherUse(null, SimpleType.constructUnsafe(Object.class),
                AnnotatedClassResolver.createPrimordial(CLS_OBJECT));
    }

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    public BasicClassIntrospector() {
    }

    @Override
    public ClassIntrospector copy() {
        return new BasicClassIntrospector();
    }

    /*
    /**********************************************************
    /* Factory method impls
    /**********************************************************
     */

    @Override
    public BasicBeanDescription forSerialization(SerializationConfig config,
            JavaType type, MixInResolver r)
    {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(config, type);
            if (desc == null) {
                desc = BasicBeanDescription.forSerialization(collectProperties(config,
                        type, r, true));
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserialization(DeserializationConfig config,
            JavaType type, MixInResolver r)
    {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            // As per [Databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(config, type);
            if (desc == null) {
                desc = BasicBeanDescription.forDeserialization(collectProperties(config,
                        type, r, false));
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserializationWithBuilder(DeserializationConfig config,
            JavaType builderType, MixInResolver r, BeanDescription valueTypeDesc)
    {
        // no std JDK types with Builders, so:
        return BasicBeanDescription.forDeserialization(collectPropertiesWithBuilder(config,
                builderType, r, valueTypeDesc, false));
    }

    @Override
    @Deprecated // since 2.12
    public BasicBeanDescription forDeserializationWithBuilder(DeserializationConfig config,
            JavaType type, MixInResolver r)
    {
        // no std JDK types with Builders, so:
        return BasicBeanDescription.forDeserialization(collectPropertiesWithBuilder(config,
                type, r, null, false));
    }

    @Override
    public BasicBeanDescription forCreation(DeserializationConfig config,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(config, type);
            if (desc == null) {
                desc = BasicBeanDescription.forDeserialization(
                        collectProperties(config, type, r, false));
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forClassAnnotations(MapperConfig<?> config,
            JavaType type, MixInResolver r)
    {
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
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
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            desc = BasicBeanDescription.forOtherUse(config, type,
                    _resolveAnnotatedWithoutSuperTypes(config, type, r));
        }
        return desc;
    }

    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    /**
     * @since 2.12
     */
    protected POJOPropertiesCollector collectProperties(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization)
    {
        final AnnotatedClass classDef = _resolveAnnotatedClass(config, type, r);
        final AccessorNamingStrategy accNaming = type.isRecordType()
                ? config.getAccessorNaming().forRecord(config, classDef)
                : config.getAccessorNaming().forPOJO(config, classDef);
        return constructPropertyCollector(config, classDef, type, forSerialization, accNaming);
    }

    @Deprecated // since 2.12
    protected POJOPropertiesCollector collectProperties(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization,
            String mutatorPrefix)
    {
        final AnnotatedClass classDef = _resolveAnnotatedClass(config, type, r);
        final AccessorNamingStrategy accNaming = new DefaultAccessorNamingStrategy.Provider().withSetterPrefix(mutatorPrefix).forPOJO(config, classDef);
        return constructPropertyCollector(config, classDef, type, forSerialization, accNaming);
    }

    /**
     * @since 2.12
     */
    protected POJOPropertiesCollector collectPropertiesWithBuilder(MapperConfig<?> config,
            JavaType type, MixInResolver r, BeanDescription valueTypeDesc,
            boolean forSerialization)
    {
        final AnnotatedClass builderClassDef = _resolveAnnotatedClass(config, type, r);
        final AccessorNamingStrategy accNaming = config.getAccessorNaming()
                .forBuilder(config, builderClassDef, valueTypeDesc);
        return constructPropertyCollector(config, builderClassDef, type, forSerialization, accNaming);
    }

    @Deprecated // since 2.12
    protected POJOPropertiesCollector collectPropertiesWithBuilder(MapperConfig<?> config,
            JavaType type, MixInResolver r, boolean forSerialization)
    {
        return collectPropertiesWithBuilder(config, type, r, null, forSerialization);
    }

    /**
     * Overridable method called for creating {@link POJOPropertiesCollector} instance
     * to use; override is needed if a custom sub-class is to be used.
     *
     * @since 2.12
     */
    protected POJOPropertiesCollector constructPropertyCollector(MapperConfig<?> config,
            AnnotatedClass classDef, JavaType type, boolean forSerialization,
            AccessorNamingStrategy accNaming)
    {
        return new POJOPropertiesCollector(config, forSerialization, type, classDef, accNaming);
    }

    @Deprecated // since 2.12
    protected POJOPropertiesCollector constructPropertyCollector(MapperConfig<?> config,
            AnnotatedClass ac, JavaType type, boolean forSerialization,
            String mutatorPrefix)
    {
        return new POJOPropertiesCollector(config, forSerialization, type, ac, mutatorPrefix);
    }

    /**
     * Method called to see if type is one of core JDK types
     * that we have cached for efficiency.
     */
    protected BasicBeanDescription _findStdTypeDesc(MapperConfig<?> config, JavaType type)
    {
        Class<?> cls = type.getRawClass();
        if (cls.isPrimitive()) {
            if (cls == Integer.TYPE) {
                return INT_DESC;
            }
            if (cls == Long.TYPE) {
                return LONG_DESC;
            }
            if (cls == Boolean.TYPE) {
                return BOOLEAN_DESC;
            }
        } else if (ClassUtil.isJDKClass(cls)) {
            if (cls == CLS_OBJECT) {
                return OBJECT_DESC;
            }
            if (cls == CLS_STRING) {
                return STRING_DESC;
            }
            if (cls == Integer.class) {
                return INT_DESC;
            }
            if (cls == Long.class) {
                return LONG_DESC;
            }
            if (cls == Boolean.class) {
                return BOOLEAN_DESC;
            }
        } else if (CLS_JSON_NODE.isAssignableFrom(cls)) {
            return BasicBeanDescription.forOtherUse(config, type,
                    AnnotatedClassResolver.createPrimordial(cls));
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

    /**
     * @since 2.9
     */
    protected AnnotatedClass _resolveAnnotatedClass(MapperConfig<?> config,
            JavaType type, MixInResolver r) {
        return AnnotatedClassResolver.resolve(config, type, r);
    }

    /**
     * @since 2.9
     */
    protected AnnotatedClass _resolveAnnotatedWithoutSuperTypes(MapperConfig<?> config,
            JavaType type, MixInResolver r) {
        return AnnotatedClassResolver.resolveWithoutSuperTypes(config, type, r);
    }
}
